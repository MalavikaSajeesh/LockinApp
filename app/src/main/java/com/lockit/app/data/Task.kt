package com.lockit.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskDuration { SHORT_TERM, LONG_TERM }
enum class TaskRecurrence { ONE_TIME, RECURRING }

enum class VerificationMethod {
    CAMERA_SCAN,   // ML Kit image labeling via live camera
    TIMER,         // must run an in-app timer to completion
    LOCATION,      // GPS check against a saved place
    HEALTH_DATA,   // steps / activity via Health Connect
    MANUAL         // honor-system checkbox
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val duration: TaskDuration,
    val recurrence: TaskRecurrence,
    val verificationMethod: VerificationMethod,
    // Comma-separated expected ML Kit labels, only used when verificationMethod == CAMERA_SCAN
    // e.g. "food,plate,breakfast" - editable by the user, pre-filled with a smart guess from the title
    val expectedLabels: String = "",
    // Weightage 0-100, all tasks for a given day must sum to <= 100
    val weightagePercent: Int = 0,
    // Which calendar day (yyyy-MM-dd) this instance belongs to
    val dateKey: String,
    val isCompleted: Boolean = false,
    val completedAtEpochMillis: Long? = null
)
