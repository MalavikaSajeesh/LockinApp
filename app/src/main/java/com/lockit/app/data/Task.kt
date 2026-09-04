package com.lockin.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskDuration { SHORT_TERM, LONG_TERM }
enum class TaskRecurrence { ONE_TIME, RECURRING }

enum class VerificationMethod {
    CAMERA_SCAN,
    TIMER,
    LOCATION,
    MANUAL
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val duration: TaskDuration,
    val recurrence: TaskRecurrence,
    val verificationMethod: VerificationMethod,
    val expectedLabels: String = "",
    val timerMinutes: Int = 10,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int = 150,
    val placeLabel: String = "",
    val weightagePercent: Int = 0,
    /** Which calendar day (yyyy-MM-dd) this task belongs to. */
    val dateKey: String,
    /** Manual ordering within the day. Lower sorts first. */
    val sortOrder: Int = 0,
    /**
     * Optional reminder time in 24-hour "HH:mm" format.
     * Empty string means no specific time set.
     * Notifications use this; blank tasks are just day-level.
     */
    val scheduledTime: String = "",
    val isCompleted: Boolean = false,
    val completedAtEpochMillis: Long? = null,
    /**
     * Set when this row was generated from a template (recurring roll-forward)
     * or carried forward from an incomplete previous day.
     */
    val templateOfDateKey: String? = null
)
