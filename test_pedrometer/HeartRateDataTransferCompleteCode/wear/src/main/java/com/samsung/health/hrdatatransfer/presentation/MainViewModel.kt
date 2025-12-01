/*
 * Copyright 2023 Samsung Electronics Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.samsung.health.hrdatatransfer.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.health.data.TrackedData
import com.samsung.health.hrdatatransfer.data.ConnectionMessage
import com.samsung.health.hrdatatransfer.data.TrackerMessage
import com.samsung.health.hrdatatransfer.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainViewModel"

@HiltViewModel
class MainViewModel @Inject constructor(
    private val makeConnectionToHealthTrackingServiceUseCase: MakeConnectionToHealthTrackingServiceUseCase,
    private val stopTrackingUseCase: StopTrackingUseCase,
    private val areTrackingCapabilitiesAvailableUseCase: AreTrackingCapabilitiesAvailableUseCase
) : ViewModel() {

    private val _trackingState = MutableStateFlow(
        TrackingState(
            trackingRunning = false,
            trackingError = false,
            valueHR = "-",
            valueIBI = arrayListOf(),
            message = ""
        )
    )
    val trackingState: StateFlow<TrackingState> = _trackingState

    private val _connectionState = MutableStateFlow(
        ConnectionState(connected = false, message = "", connectionException = null)
    )
    val connectionState: StateFlow<ConnectionState> = _connectionState

    @Inject
    lateinit var trackHeartRateUseCase: TrackHeartRateUseCase

    private var trackingJob: Job? = null
    private var sendingJob: Job? = null
    private var currentIBI = arrayListOf<Int>(4)

    // --- Tracking Setup ---

    fun setUpTracking() {
        viewModelScope.launch {
            makeConnectionToHealthTrackingServiceUseCase().collect { connectionMessage ->
                when (connectionMessage) {
                    is ConnectionMessage.ConnectionSuccessMessage -> {
                        _connectionState.value = ConnectionState(
                            connected = true,
                            message = "Connected to Health Tracking Service",
                            connectionException = null
                        )
                    }
                    is ConnectionMessage.ConnectionFailedMessage -> {
                        _connectionState.value = ConnectionState(
                            connected = false,
                            message = "Connection failed",
                            connectionException = connectionMessage.exception
                        )
                    }
                    is ConnectionMessage.ConnectionEndedMessage -> {
                        _connectionState.value = ConnectionState(
                            connected = false,
                            message = "Connection ended",
                            connectionException = null
                        )
                    }
                }
            }
        }
    }

    // --- Tracking Lifecycle ---

    fun startTracking(context: Context) {
        trackingJob?.cancel()
        if (!areTrackingCapabilitiesAvailableUseCase()) {
            _trackingState.value = TrackingState(
                trackingRunning = false,
                trackingError = true,
                valueHR = "-",
                valueIBI = arrayListOf(),
                message = "HR tracking not available"
            )
            return
        }

        trackingJob = viewModelScope.launch {
            startPeriodicSendingHR(context) // send HR to Flutter every second

            trackHeartRateUseCase().collect { trackerMessage ->
                when (trackerMessage) {
                    is TrackerMessage.DataMessage -> processHeartRate(trackerMessage.trackedData)
                    is TrackerMessage.FlushCompletedMessage -> stopTracking()
                    is TrackerMessage.TrackerErrorMessage -> {
                        stopTracking()
                        _trackingState.value = _trackingState.value.copy(
                            trackingError = true,
                            message = trackerMessage.trackerError
                        )
                    }
                    is TrackerMessage.TrackerWarningMessage -> {
                        _trackingState.value = _trackingState.value.copy(
                            trackingError = false,
                            message = trackerMessage.trackerWarning
                        )
                    }
                }
            }
        }
    }

    fun stopTracking() {
        stopTrackingUseCase()
        trackingJob?.cancel()
        stopPeriodicSendingHR()
        _trackingState.value = TrackingState(
            trackingRunning = false,
            trackingError = false,
            valueHR = "-",
            valueIBI = arrayListOf(),
            message = ""
        )
    }

    // --- Heart Rate Processing ---

    private fun processHeartRate(trackedData: TrackedData) {
        val hr = if (trackedData.hr > 0) trackedData.hr.toString() else "-"
        currentIBI = trackedData.ibi
        _trackingState.value = _trackingState.value.copy(
            trackingRunning = true,
            trackingError = false,
            valueHR = hr,
            valueIBI = currentIBI
        )
    }

    // --- Periodic Sending HR to Flutter ---

    private fun startPeriodicSendingHR(context: Context) {
        sendingJob?.cancel()
        sendingJob = viewModelScope.launch {
            while (isActive) {
                sendHeartRateToFlutter(context, _trackingState.value.valueHR)
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun stopPeriodicSendingHR() {
        sendingJob?.cancel()
    }

    private fun sendHeartRateToFlutter(context: Context, hr: String) {
        if (hr == "-") return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                nodes.forEach { node ->
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, "/heart_rate", hr.toByteArray())
                        .addOnSuccessListener { Log.i(TAG, "HR sent: $hr") }
                        .addOnFailureListener { e -> Log.e(TAG, "Failed sending HR", e) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending HR", e)
            }
        }
    }
}

// --- Data Classes ---

data class ConnectionState(
    val connected: Boolean,
    val message: String,
    val connectionException: HealthTrackerException?
)

data class TrackingState(
    val trackingRunning: Boolean,
    val trackingError: Boolean,
    val valueHR: String,
    val valueIBI: ArrayList<Int>,
    val message: String
)
