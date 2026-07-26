package com.screenguard.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimitEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String = "",
    val dailyLimitMinutes: Int = 60,
    val warnAtPercent: Int = 80,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
