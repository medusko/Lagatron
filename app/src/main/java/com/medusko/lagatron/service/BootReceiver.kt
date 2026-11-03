package com.medusko.lagatron.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.medusko.lagatron.data.AppPreferences

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = AppPreferences(context)
            if (prefs.startOnBoot && Settings.canDrawOverlays(context)) {
                PingForegroundService.start(context)
            }
        }
    }
}
