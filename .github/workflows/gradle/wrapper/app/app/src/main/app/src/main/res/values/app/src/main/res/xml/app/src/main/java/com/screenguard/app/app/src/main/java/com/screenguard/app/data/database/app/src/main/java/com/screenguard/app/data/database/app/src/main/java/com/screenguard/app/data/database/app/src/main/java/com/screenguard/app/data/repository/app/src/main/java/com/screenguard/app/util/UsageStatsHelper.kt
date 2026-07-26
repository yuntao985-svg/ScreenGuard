package com.screenguard.util

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import java.util.*

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageTimeMs: Long,
    val lastTimeUsed: Long
)

class UsageStatsHelper(private val context: Context) {

    private val usageStatsManager: UsageStatsManager
        get() = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun getTodayUsageStats(): List<AppUsageInfo> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val pm = context.packageManager

        return usageStatsList
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
            .map { stats ->
                val appName = try {
                    pm.getApplicationLabel(
                        pm.getApplicationInfo(stats.packageName, 0)
                    ).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    stats.packageName
                }

                AppUsageInfo(
                    packageName = stats.packageName,
                    appName = appName,
                    usageTimeMs = stats.totalTimeInForeground,
                    lastTimeUsed = stats.lastTimeUsed
                )
            }
    }

    fun getTodayUsageForPackage(packageName: String): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val statsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            calendar.timeInMillis,
            System.currentTimeMillis()
        )

        return statsList
            .find { it.packageName == packageName }
            ?.totalTimeInForeground ?: 0
    }

    fun getTotalScreenTime(): Long {
        return getTodayUsageStats().sumOf { it.usageTimeMs }
    }

    fun hasUsageStatsPermission(): Boolean {
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - 1000 * 60,
            now
        )
        return stats.isNotEmpty()
    }
}
