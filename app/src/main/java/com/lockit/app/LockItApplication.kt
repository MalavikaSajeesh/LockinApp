package com.lockit.app

import android.app.Application
import com.lockit.app.data.AppDatabase
import com.lockit.app.data.TokenState
import com.lockit.app.worker.WeeklyTokenResetWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class LockItApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        CoroutineScope(Dispatchers.IO).launch {
            if (db.tokenDao().get() == null) {
                db.tokenDao().upsert(
                    TokenState(tokensRemaining = 10, weekStartEpochDay = LocalDate.now().toEpochDay())
                )
            }
        }
        WeeklyTokenResetWorker.schedule(this)
    }
}
