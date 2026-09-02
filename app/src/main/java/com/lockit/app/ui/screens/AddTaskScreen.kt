package com.lockit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lockit.app.data.TaskDuration
import com.lockit.app.data.TaskRecurrence
import com.lockit.app.data.VerificationMethod
import com.lockit.app.ui.TodoViewModel
import com.lockit.app.util.UnlockCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(viewModel: TodoViewModel, onDone: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(TaskDuration.SHORT_TERM) }
    var recurrence by remember { mutableStateOf(TaskRecurrence.ONE_TIME) }
    var verification by remember { mutableStateOf(VerificationMethod.MANUAL) }
    var weightage by remember { mutableStateOf(0f) }
    var labelsText by remember { mutableStateOf("") }

    // Auto-suggest expected labels as the user types a title
    LaunchedEffect(title) {
        if (verification == VerificationMethod.CAMERA_SCAN && labelsText.isBlank()) {
            val suggestion = UnlockCalculator.suggestLabelsForTitle(title)
            if (suggestion.isNotEmpty()) labelsText = suggestion.joinToString(", ")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("New Task", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Task title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Text("Term", style = MaterialTheme.typography.labelLarge)
        Row {
            FilterChip(
                selected = duration == TaskDuration.SHORT_TERM,
                onClick = { duration = TaskDuration.SHORT_TERM },
                label = { Text("Short-term") }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = duration == TaskDuration.LONG_TERM,
                onClick = { duration = TaskDuration.LONG_TERM },
                label = { Text("Long-term (fixed)") }
            )
        }
        Spacer(Modifier.height(16.dp))

        Text("Repeats", style = MaterialTheme.typography.labelLarge)
        Row {
            FilterChip(
                selected = recurrence == TaskRecurrence.ONE_TIME,
                onClick = { recurrence = TaskRecurrence.ONE_TIME },
                label = { Text("One-time") }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = recurrence == TaskRecurrence.RECURRING,
                onClick = { recurrence = TaskRecurrence.RECURRING },
                label = { Text("Recurring") }
            )
        }
        Spacer(Modifier.height(16.dp))

        Text("How should this be verified?", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        VerificationMethod.entries.forEach { method ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                RadioButton(selected = verification == method, onClick = { verification = method })
                Text(
                    when (method) {
                        VerificationMethod.CAMERA_SCAN -> "Scan with camera (on-device image check)"
                        VerificationMethod.TIMER -> "Run a timer to completion"
                        VerificationMethod.LOCATION -> "Check my location"
                        VerificationMethod.HEALTH_DATA -> "Check step/activity data"
                        VerificationMethod.MANUAL -> "Just let me check it off"
                    }
                )
            }
        }

        if (verification == VerificationMethod.CAMERA_SCAN) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = labelsText,
                onValueChange = { labelsText = it },
                label = { Text("Expected in photo (comma separated)") },
                supportingText = { Text("Auto-filled from title, edit if needed") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("Weightage: ${weightage.toInt()}%", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = weightage,
            onValueChange = { weightage = it },
            valueRange = 0f..100f,
            steps = 19
        )
        Text(
            "How much of the locked app's usage time unlocks when this task is done.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                if (title.isNotBlank()) {
                    val labels = labelsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    viewModel.addTask(title, duration, recurrence, verification, labels, weightage.toInt())
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Task")
        }
    }
}
