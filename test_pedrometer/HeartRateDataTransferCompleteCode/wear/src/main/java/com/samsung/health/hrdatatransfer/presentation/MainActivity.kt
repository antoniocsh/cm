/*
 * Copyright 2023 Samsung Electronics Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.samsung.health.hrdatatransfer.presentation

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samsung.health.hrdatatransfer.presentation.ui.MainScreen
import com.samsung.health.hrdatatransfer.presentation.ui.Permission
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.getValue


private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel>()

    @SuppressLint("VisibleForTests")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val trackingState by viewModel.trackingState.collectAsStateWithLifecycle()
            val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

            if (trackingState.trackingRunning) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            Log.i(
                TAG, "connected: ${connectionState.connected}, " +
                        "message: ${connectionState.message}, " +
                        "connectionException: ${connectionState.connectionException}"
            )

            connectionState.connectionException?.resolve(this)

            Permission {
                MainScreen(
                    connected = connectionState.connected,
                    connectionMessage = connectionState.message,
                    trackingRunning = trackingState.trackingRunning,
                    trackingError = trackingState.trackingError,
                    trackingMessage = trackingState.message,
                    valueHR = trackingState.valueHR,
                    valueIBI = trackingState.valueIBI,
                    onStartTracking = {
                        viewModel.startTracking(this)
                        Log.i(TAG, "startTracking()")
                    },
                    onStopTracking = {
                        viewModel.stopTracking()
                        Log.i(TAG, "stopTracking()")
                    },
                    onSendMessage = {
                        // removed because we now send HR automatically in the ViewModel
                        Log.i(TAG, "HR is sent automatically; no manual send needed")
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!viewModel.connectionState.value.connected) {
            viewModel.setUpTracking()
        }
    }
}
