package com.lockit.app.worker

import android.content.Context
import androidx.work.*
import com.lockit.app.data.AppDatabase
import com.lockit.app.data.TokenState
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class WeeklyTokenResetWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val state = db.tokenDao().get() ?: return Result.success()
        val today = LocalDate.now().toEpochDay()
        val daysSinceReset = today - state.weekStartEpochDay

        if (daysSinceReset >= 7) {
            db.tokenDao().upsert(
                state.copy(tokensRemaining = 10, weekStartEpochDay = today, activeSkipExpiresAtEpochMillis = null)
            )
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "weekly_token_reset"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeeklyTokenResetWorker>(1, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
