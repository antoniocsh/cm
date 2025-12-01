package com.samsung.health.hrdatatransfer.presentation

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "WearDataSenderService"
private const val PATH_HEART_RATE = "/heart_rate"

fun sendHeartRateToPhone(context: Context, hr: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
            nodes.forEach { node ->
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, "/heart_rate", hr.toByteArray())
                    .addOnSuccessListener { Log.i("WearDataSender", "HR sent: $hr") }
                    .addOnFailureListener { e -> Log.e("WearDataSender", "Failed sending HR", e) }
            }
        } catch (e: Exception) {
            Log.e("WearDataSender", "Error sending HR", e)
        }
    }
}
