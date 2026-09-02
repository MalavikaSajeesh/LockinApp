package com.lockit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lockit.app.data.LockedApp
import com.lockit.app.ui.TodoViewModel

@Composable
fun LockedAppsScreen(viewModel: TodoViewModel) {
    val context = LocalContext.current
    val lockedApps by viewModel.lockedApps.collectAsState()
    var showPicker by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add app")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Locked Apps", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Set a daily usage budget for each app you want gated behind your tasks.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(lockedApps, key = { it.packageName }) { app ->
                    LockedAppRow(app, onRemove = { viewModel.removeLockedApp(app) })
                }
            }
        }
    }

    if (showPicker) {
        AppPickerDialog(
            onDismiss = { showPicker = false },
            onPicked = { pkg, label, minutes ->
                viewModel.addLockedApp(pkg, label, minutes)
                showPicker = false
            }
        )
    }
}

@Composable
private fun LockedAppRow(app: LockedApp, onRemove: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(app.appLabel, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${app.dailyUsageMinutes} min/day allotted · ${app.minutesUsedToday} used today",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
        }
    }
}

/**
 * Simplified app picker: in a full build, this queries PackageManager for
 * installed launchable apps. Kept as a manual-entry dialog here since the
 * device's actual installed-app list can't be enumerated in this preview -
 * swap the TextField for a LazyColumn of PackageManager.getInstalledApplications()
 * results when you build this in Android Studio.
 */
@Composable
private fun AppPickerDialog(
    onDismiss: () -> Unit,
    onPicked: (packageName: String, label: String, minutes: Int) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add app to lock") },
        text = {
            Column {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("App name (e.g. Instagram)") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = packageName, onValueChange = { packageName = it }, label = { Text("Package name (e.g. com.instagram.android)") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = minutes, onValueChange = { minutes = it.filter(Char::isDigit) }, label = { Text("Daily usage minutes") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val m = minutes.toIntOrNull() ?: 30
                if (packageName.isNotBlank() && label.isNotBlank()) onPicked(packageName, label, m)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
