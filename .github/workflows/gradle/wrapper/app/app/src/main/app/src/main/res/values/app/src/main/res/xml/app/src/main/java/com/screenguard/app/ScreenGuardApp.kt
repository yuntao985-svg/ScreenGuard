package com.screenguard.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.screenguard.data.database.AppDatabase
import com.screenguard.data.repository.AppLimitRepository
import com.screenguard.util.UsageStatsHelper

class ScreenGuardApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var repository: AppLimitRepository
        private set
    lateinit var usageStatsHelper: UsageStatsHelper
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        repository = AppLimitRepository(database.appLimitDao())
        usageStatsHelper = UsageStatsHelper(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val reminderChannel = NotificationChannel(
            CHANNEL_REMINDER,
            "使用时长提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "当应用使用时长达到设定阈值时提醒"
            enableVibration(true)
        }

        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            "后台监控服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "后台使用时长监控服务"
            setShowBadge(false)
        }

        manager.createNotificationChannel(reminderChannel)
        manager.createNotificationChannel(serviceChannel)
    }

    companion object {
        lateinit var instance: ScreenGuardApp
            private set

        const val CHANNEL_REMINDER = "channel_reminder"
        const val CHANNEL_SERVICE = "channel_service"
    }
}
