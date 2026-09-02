package com.lockit.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.lockit.app.data.AppDatabase
import com.lockit.app.ui.LockScreenActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Watches for the foreground app changing. If it's a package the user has
 * locked, and it's not currently unlocked (per weightage progress or an
 * active emergency token skip), launches LockScreenActivity as a full-screen
 * overlay that blocks interaction with the underlying app.
 */
class AppLockAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (packageName == applicationContext.packageName) return

        scope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val lockedApp = db.lockedAppDao().getByPackage(packageName) ?: return@launch

            val tokenState = db.tokenDao().get()
            val skipActive = tokenState?.activeSkipExpiresAtEpochMillis?.let {
                it > System.currentTimeMillis()
            } ?: false
            if (skipActive) return@launch // emergency skip in effect, let it through

            val dateKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val totalWeightage = db.taskDao().getTotalWeightageForDate(dateKey)
            // Re-query completed sum synchronously (Flow -> first value) is avoided here
            // for simplicity; LockScreenActivity re-checks live state on open/close.

            val today = LocalDate.now().toEpochDay()
            val usedMinutes = if (lockedApp.minutesUsedTodayEpochDay == today) lockedApp.minutesUsedToday else 0
            val allowedNow = usedMinutes < lockedApp.dailyUsageMinutes // upper bound check happens in LockScreenActivity too

            if (!allowedNow || totalWeightage >= 0) {
                // Always route through LockScreenActivity, which computes the
                // precise unlocked-minutes figure and decides show/skip itself.
                val intent = Intent(applicationContext, LockScreenActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(LockScreenActivity.EXTRA_PACKAGE, packageName)
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() { /* no-op */ }
}
