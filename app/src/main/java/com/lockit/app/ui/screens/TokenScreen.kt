package com.lockit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lockit.app.ui.TodoViewModel
import com.lockit.app.ui.theme.LockItGreen
import com.lockit.app.ui.theme.LockItRed

@Composable
fun TokenScreen(viewModel: TodoViewModel) {
    val tokenState by viewModel.tokenState.collectAsState()
    var showPicker by remember { mutableStateOf(false) }
    val remaining = tokenState?.tokensRemaining ?: 0
    val skipActive = (tokenState?.activeSkipExpiresAtEpochMillis ?: 0) > System.currentTimeMillis()

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Emergency Tokens", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        Text(
            "$remaining",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = if (remaining > 0) LockItGreen else LockItRed
        )
        Text("tokens left this week", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Resets to 10 every 7 days",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(32.dp))

        if (skipActive) {
            Text("An emergency skip is currently active.", color = LockItGreen)
        } else {
            Button(
                onClick = { showPicker = true },
                enabled = remaining > 0
            ) {
                Text("Use a Token to Skip Lock")
            }
        }
    }

    if (showPicker) {
        MinutesPickerDialog(
            onDismiss = { showPicker = false },
            onConfirm = { minutes ->
                viewModel.useEmergencyToken(minutes)
                showPicker = false
            }
        )
    }
}

@Composable
private fun MinutesPickerDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var minutesText by remember { mutableStateOf("15") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Skip duration") },
        text = {
            OutlinedTextField(
                value = minutesText,
                onValueChange = { minutesText = it.filter(Char::isDigit) },
                label = { Text("Minutes to unlock") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(minutesText.toIntOrNull() ?: 15) }) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
