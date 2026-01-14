package com.samsung.micrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * BroadcastReceiver that listens for device boot completion.
 * Prioritizes starting the Activity to gain foreground context for the Mic Service.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    private val tag = "BootCompletedReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(tag, "Received broadcast action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON") {

            Log.d(tag, "Boot completed sequence detected. Attempting to launch UI...")

            // 1. Start the Activity First
            // On Android 10+, this ONLY works if "Appear on top" permission is granted.
            val activityIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            
            try {
                context.startActivity(activityIntent)
                Log.d(tag, "MainActivity start command sent")
            } catch (e: Exception) {
                Log.e(tag, "Failed to start activity: ${e.message}")
            }

            // 2. Start Service (This will likely fail on Android 14 if Activity isn't visible yet,
            // but the Activity will start it again once it comes to foreground).
            val canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true

            if (canDrawOverlays) {
                val serviceIntent = Intent(context, MicRecorderService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    Log.d(tag, "MicRecorderService start command sent")
                } catch (e: Exception) {
                    Log.e(tag, "Service start blocked (expected behavior on Android 14 background): ${e.message}")
                }
            } else {
                Log.w(tag, "Cannot start service or activity: 'Appear on top' permission missing")
            }
        }
    }
}
