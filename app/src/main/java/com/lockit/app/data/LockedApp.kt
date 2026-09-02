package com.lockit.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locked_apps")
data class LockedApp(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    // Total minutes the user is allowed to use this app per day, once fully unlocked
    val dailyUsageMinutes: Int,
    // Minutes actually used today, resets daily
    val minutesUsedTodayEpochDay: Long = 0, // which epoch day this counter belongs to
    val minutesUsedToday: Int = 0
)
