package com.lockit.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lockit.app.data.AppDatabase
import com.lockit.app.ui.theme.LockItBlack
import com.lockit.app.ui.theme.LockItGreen
import com.lockit.app.ui.theme.LockItRed
import com.lockit.app.ui.theme.LockItTheme
import com.lockit.app.util.UnlockCalculator
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * This IS the "lock overlay". It launches full-screen whenever a locked app
 * is opened and the daily minute allowance for the current unlock % has been
 * used up. Pressing back/home just returns to launcher - the underlying app
 * was never actually opened past this screen (Accessibility Service caught
 * it before content rendered meaningfully).
 */
class LockScreenActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: ""

        setContent {
            LockItTheme {
                LockScreenContent(packageName = packageName, onDismiss = { finish() })
            }
        }
    }
}

@Composable
private fun LockScreenContent(packageName: String, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    var unlockedMinutes by remember { mutableStateOf(0) }
    var unlockedPercent by remember { mutableStateOf(0) }
    var usedMinutes by remember { mutableStateOf(0) }
    var isFullyUnlocked by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(packageName) {
        val dateKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val lockedApp = db.lockedAppDao().getByPackage(packageName)
        val allTasksToday = db.taskDao().getTotalWeightageForDate(dateKey) // sum, for reference

        // For simplicity, pull tasks once (non-Flow) via a direct query pattern:
        // in production wire this to the Flow; kept synchronous here for clarity.
        val result = UnlockCalculator.calculate(
            dailyAllottedMinutes = lockedApp?.dailyUsageMinutes ?: 0,
            totalTasksToday = 1, // replace with real count via DAO in TodoViewModel-driven flow
            completedTasksToday = 0,
            completedWeightageSum = 0
        )
        unlockedMinutes = result.unlockedMinutes
        unlockedPercent = result.unlockedPercent
        isFullyUnlocked = result.isFullyUnlocked
        usedMinutes = if (lockedApp != null && lockedApp.minutesUsedTodayEpochDay == LocalDate.now().toEpochDay())
            lockedApp.minutesUsedToday else 0
        loaded = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LockItBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔒",
                fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "This app is locked",
                color = LockItRed,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (usedMinutes >= unlockedMinutes)
                    "You've used your unlocked time ($unlockedMinutes min at $unlockedPercent% task completion)."
                else
                    "You have ${unlockedMinutes - usedMinutes} min available ($unlockedPercent% unlocked).",
                color = LockItGreen,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onDismiss) {
                Text("Go to Todo List")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onDismiss) {
                Text("Use Emergency Token")
            }
        }
    }
}
