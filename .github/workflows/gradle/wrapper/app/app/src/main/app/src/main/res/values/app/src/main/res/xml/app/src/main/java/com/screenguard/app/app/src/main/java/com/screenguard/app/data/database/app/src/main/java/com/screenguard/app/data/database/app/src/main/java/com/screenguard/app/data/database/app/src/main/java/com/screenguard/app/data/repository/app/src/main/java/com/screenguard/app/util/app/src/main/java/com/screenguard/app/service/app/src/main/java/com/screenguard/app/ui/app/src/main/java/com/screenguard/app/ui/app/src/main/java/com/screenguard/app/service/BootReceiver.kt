package com.screenguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.screenguard.ScreenGuardApp

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = ScreenGuardApp.instance
            if (app.usageStatsHelper.hasUsageStatsPermission()) {
                MonitorService.start(context)
            }
        }
    }
}
