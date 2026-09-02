package com.lockit.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lockit.app.data.*
import com.lockit.app.util.UnlockCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TodoViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val todayKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val tasksToday: StateFlow<List<Task>> = db.taskDao().getTasksForDate(todayKey)
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())

    val lockedApps: StateFlow<List<LockedApp>> = db.lockedAppDao().getAll()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())

    val tokenState: StateFlow<TokenState?> = db.tokenDao().observe()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, null)

    fun addTask(
        title: String,
        duration: TaskDuration,
        recurrence: TaskRecurrence,
        verification: VerificationMethod,
        expectedLabels: List<String>,
        weightage: Int
    ) {
        viewModelScope.launch {
            val currentTotal = db.taskDao().getTotalWeightageForDate(todayKey)
            val safeWeightage = if (UnlockCalculator.canAssignWeightage(currentTotal, weightage)) weightage else (100 - currentTotal).coerceAtLeast(0)
            db.taskDao().insert(
                Task(
                    title = title,
                    duration = duration,
                    recurrence = recurrence,
                    verificationMethod = verification,
                    expectedLabels = expectedLabels.joinToString(","),
                    weightagePercent = safeWeightage,
                    dateKey = todayKey
                )
            )
        }
    }

    fun markComplete(task: Task) {
        viewModelScope.launch {
            db.taskDao().update(
                task.copy(isCompleted = true, completedAtEpochMillis = System.currentTimeMillis())
            )
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { db.taskDao().delete(task) }
    }

    fun addLockedApp(packageName: String, label: String, dailyMinutes: Int) {
        viewModelScope.launch {
            db.lockedAppDao().upsert(
                LockedApp(packageName = packageName, appLabel = label, dailyUsageMinutes = dailyMinutes)
            )
        }
    }

    fun removeLockedApp(app: LockedApp) {
        viewModelScope.launch { db.lockedAppDao().delete(app) }
    }

    fun useEmergencyToken(minutes: Int) {
        viewModelScope.launch {
            val state = db.tokenDao().get() ?: return@launch
            if (state.tokensRemaining <= 0) return@launch
            db.tokenDao().upsert(
                state.copy(
                    tokensRemaining = state.tokensRemaining - 1,
                    activeSkipExpiresAtEpochMillis = System.currentTimeMillis() + minutes * 60_000L
                )
            )
        }
    }

    /** Live-computed unlock summary, recalculated whenever tasksToday changes. */
    val unlockSummary: StateFlow<UnlockCalculator.UnlockResult> = tasksToday
        .map { tasks ->
            val completed = tasks.count { it.isCompleted }
            val completedWeightage = tasks.filter { it.isCompleted }.sumOf { it.weightagePercent }
            // Uses the first locked app's minutes as reference; in the real UI
            // this is computed per-app in LockedAppsScreen.
            UnlockCalculator.calculate(
                dailyAllottedMinutes = 100, // placeholder scale; per-app screen recalculates with real minutes
                totalTasksToday = tasks.size,
                completedTasksToday = completed,
                completedWeightageSum = completedWeightage
            )
        }
        .stateIn(
            viewModelScope,
            kotlinx.coroutines.flow.SharingStarted.Eagerly,
            UnlockCalculator.calculate(100, 0, 0, 0)
        )
}
