package com.screenguard.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.screenguard.app.ScreenGuardApp
import com.screenguard.ui.ReminderActivity
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class MonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val lastRemindedMap = ConcurrentHashMap<String, Long>()
    private val sentFullReminders = ConcurrentHashMap<String, Boolean>()

    override fun onCreate() {
        super.onCreate()
        startForeground()
        resetRemindersAtMidnight()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITOR -> startMonitoring()
            ACTION_STOP_MONITOR -> stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForeground() {
        val notification = NotificationCompat.Builder(this, ScreenGuardApp.CHANNEL_SERVICE)
            .setContentTitle("屏控")
            .setContentText("正在监控应用使用时长")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startMonitoring() {
        serviceScope.launch {
            delay(5000)
            while (isActive) {
                try {
                    checkAllAppLimits()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(30_000)
            }
        }
    }

    private suspend fun checkAllAppLimits() {
        val app = ScreenGuardApp.instance
        val limitList = app.repository.getAllEnabledList()

        for (limit in limitList) {
            val usageMs = app.usageStatsHelper.getTodayUsageForPackage(limit.packageName)
            val usageMinutes = usageMs / 60_000
            val limitMinutes = limit.dailyLimitMinutes
            val warnThreshold = (limitMinutes * limit.warnAtPercent / 100)

            when {
                usageMinutes >= limitMinutes && sentFullReminders[limit.packageName] != true -> {
                    showFullScreenReminder(limit.packageName, limit.appName, usageMinutes, limitMinutes)
                    sentFullReminders[limit.packageName] = true
                }
                usageMinutes >= warnThreshold && usageMinutes < limitMinutes -> {
                    val lastReminded = lastRemindedMap[limit.packageName] ?: 0
                    val now = System.currentTimeMillis()
                    if (now - lastReminded > 15 * 60 * 1000) {
                        showWarningNotification(limit.packageName, limit.appName, usageMinutes, limitMinutes)
                        lastRemindedMap[limit.packageName] = now
                    }
                }
            }
        }
    }

    private fun showWarningNotification(pkg: String, appName: String, used: Long, limit: Long) {
        val intent = Intent(this, com.screenguard.ui.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val percent = if (limit > 0) (used * 100 / limit) else 0
        val notification = NotificationCompat.Builder(this, ScreenGuardApp.CHANNEL_REMINDER)
            .setContentTitle("⏰ 该休息了")
            .setContentText("「${appName}」已使用 ${used} 分钟（限额 ${limit} 分钟）")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("「${appName}」今日已使用 ${used} 分钟，达到每日限额的 ${percent}%。\n休息一下，保护眼睛 👀"))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_WARN_ID + pkg.hashCode(), notification)
    }

    private fun showFullScreenReminder(pkg: String, appName: String, used: Long, limit: Long) {
        val intent = Intent(this, ReminderActivity::class.java).apply {
            putExtra("package_name", pkg)
            putExtra("app_name", appName)
            putExtra("used_minutes", used)
            putExtra("limit_minutes", limit)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    private fun resetRemindersAtMidnight() {
        val calendar = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 1)
        }
        val delayMs = calendar.timeInMillis - System.currentTimeMillis()

        serviceScope.launch {
            delay(delayMs)
            sentFullReminders.clear()
            lastRemindedMap.clear()
            resetRemindersAtMidnight()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_MONITOR = "com.screenguard.action.START_MONITOR"
        const val ACTION_STOP_MONITOR = "com.screenguard.action.STOP_MONITOR"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_WARN_ID = 2001

        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java).apply {
                action = ACTION_START_MONITOR
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MonitorService::class.java).apply {
                action = ACTION_STOP_MONITOR
            }
            context.stopService(intent)
        }
    }
}
