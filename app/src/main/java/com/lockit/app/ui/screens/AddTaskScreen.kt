package com.lockin.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lockin.app.data.TaskDuration
import com.lockin.app.data.TaskRecurrence
import com.lockin.app.data.VerificationMethod
import com.lockin.app.ui.TodoViewModel
import com.lockin.app.ui.theme.Emerald
import com.lockin.app.ui.theme.Violet
import com.lockin.app.ui.verify.fetchLocation
import com.lockin.app.util.UnlockCalculator
import com.lockin.app.util.UnlockRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTaskScreen(viewModel: TodoViewModel, editTaskId: Long?, onDone: () -> Unit) {
    val context = LocalContext.current
    val lockedApps by viewModel.lockedApps.collectAsState()

    val todayKey = remember { UnlockRepository.todayKey() }

    // ----- form state -----
    var title by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(TaskDuration.SHORT_TERM) }
    var recurrence by remember { mutableStateOf(TaskRecurrence.ONE_TIME) }
    var verification by remember { mutableStateOf(VerificationMethod.MANUAL) }
    var weightage by remember { mutableStateOf(0f) }
    var timerMinutes by remember { mutableStateOf(10f) }
    var labelsText by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf(150f) }
    var placeLabel by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var locationStatus by remember { mutableStateOf<String?>(null) }
    val selectedApps = remember { mutableStateListOf<String>() }
    var ceiling by remember { mutableStateOf(100) }
    var loaded by remember { mutableStateOf(editTaskId == null) }

    // ----- scheduling -----
    var selectedDateKey by remember { mutableStateOf(todayKey) }
    var scheduledTime by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val todayMillis = remember {
        LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = todayMillis,
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis >= todayMillis
        }
    )
    val timePickerState = rememberTimePickerState(initialHour = 9, initialMinute = 0)

    // ----- load existing task when editing -----
    LaunchedEffect(editTaskId) {
        if (editTaskId != null) {
            viewModel.taskById(editTaskId)?.let { t ->
                title = t.title
                duration = t.duration
                recurrence = t.recurrence
                verification = t.verificationMethod
                weightage = t.weightagePercent.toFloat()
                timerMinutes = t.timerMinutes.toFloat()
                labelsText = t.expectedLabels
                latitude = t.latitude
                longitude = t.longitude
                radius = t.radiusMeters.toFloat()
                placeLabel = t.placeLabel
                if (t.latitude != null) locationStatus = "Place saved"
                selectedDateKey = t.dateKey
                scheduledTime = t.scheduledTime
            }
            selectedApps.clear()
            selectedApps.addAll(viewModel.linkedPackagesFor(editTaskId))
            loaded = true
        }
    }

    // ----- recompute weightage ceiling when date or task changes -----
    LaunchedEffect(selectedDateKey, editTaskId) {
        ceiling = viewModel.remainingWeightage(selectedDateKey, editTaskId)
    }

    // ----- auto-suggest camera labels -----
    LaunchedEffect(title, verification) {
        if (verification == VerificationMethod.CAMERA_SCAN && labelsText.isBlank()) {
            val suggestion = UnlockCalculator.suggestLabelsForTitle(title)
            if (suggestion.isNotEmpty()) labelsText = suggestion.joinToString(", ")
        }
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Violet)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            if (editTaskId == null) "New task" else "Edit task",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(18.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("What needs doing?") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // ---- scheduling row ----
        Spacer(Modifier.height(20.dp))
        SectionLabel("When")
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Date chip
            OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(friendlyDate(selectedDateKey, todayKey), style = MaterialTheme.typography.bodyMedium)
            }

            // Time chip
            OutlinedButton(
                onClick = { showTimePicker = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (scheduledTime.isBlank()) "Any time" else friendly24h(scheduledTime),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (scheduledTime.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = { scheduledTime = "" },
                modifier = Modifier.align(Alignment.End)
            ) { Text("Clear time", style = MaterialTheme.typography.bodySmall) }
        }

        // ---- recurrence & term ----
        Spacer(Modifier.height(20.dp))
        SectionLabel("Term")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = duration == TaskDuration.SHORT_TERM,
                onClick = { duration = TaskDuration.SHORT_TERM },
                label = { Text("Short-term") }
            )
            FilterChip(
                selected = duration == TaskDuration.LONG_TERM,
                onClick = { duration = TaskDuration.LONG_TERM },
                label = { Text("Long-term") }
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("Repeats")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = recurrence == TaskRecurrence.ONE_TIME,
                onClick = { recurrence = TaskRecurrence.ONE_TIME },
                label = { Text("One-time") }
            )
            FilterChip(
                selected = recurrence == TaskRecurrence.RECURRING,
                onClick = { recurrence = TaskRecurrence.RECURRING },
                label = { Text("Every day") }
            )
        }

        // ---- per-app scoping ----
        Spacer(Modifier.height(20.dp))
        SectionLabel("Which apps does this unlock?")
        Spacer(Modifier.height(2.dp))
        Text(
            if (selectedApps.isEmpty()) "Nothing selected means it unlocks every locked app."
            else "Only the ${selectedApps.size} selected.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        if (lockedApps.isEmpty()) {
            Text(
                "No apps locked yet. Add some on the Apps tab first.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedApps.isEmpty(),
                    onClick = { selectedApps.clear() },
                    label = { Text("All apps") }
                )
                lockedApps.forEach { app ->
                    FilterChip(
                        selected = app.packageName in selectedApps,
                        onClick = {
                            if (app.packageName in selectedApps) selectedApps.remove(app.packageName)
                            else selectedApps.add(app.packageName)
                        },
                        label = { Text(app.appLabel) }
                    )
                }
            }
        }

        // ---- verification ----
        Spacer(Modifier.height(20.dp))
        SectionLabel("How do you prove it's done?")
        Spacer(Modifier.height(4.dp))
        VerificationMethod.entries.forEach { method ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = verification == method,
                    onClick = { verification = method },
                    colors = RadioButtonDefaults.colors(selectedColor = Violet)
                )
                Column {
                    Text(
                        when (method) {
                            VerificationMethod.CAMERA_SCAN -> "Photo check"
                            VerificationMethod.TIMER -> "Run a timer"
                            VerificationMethod.LOCATION -> "Be at a place"
                            VerificationMethod.MANUAL -> "Just tick it off"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        when (method) {
                            VerificationMethod.CAMERA_SCAN ->
                                "Take a photo, checked on-device. Rough but it makes you show up."
                            VerificationMethod.TIMER -> "Stay on a countdown until it finishes."
                            VerificationMethod.LOCATION ->
                                "Checks your GPS against a saved spot. Hardest to fake."
                            VerificationMethod.MANUAL -> "Honour system."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (verification == VerificationMethod.CAMERA_SCAN) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = labelsText,
                onValueChange = { labelsText = it },
                label = { Text("Expected in the photo") },
                supportingText = { Text("Comma separated. Auto-filled from the title.") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (verification == VerificationMethod.TIMER) {
            Spacer(Modifier.height(12.dp))
            SectionLabel("Timer length: ${timerMinutes.toInt()} min")
            Slider(value = timerMinutes, onValueChange = { timerMinutes = it },
                valueRange = 1f..120f, steps = 118)
        }

        if (verification == VerificationMethod.LOCATION) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = placeLabel,
                onValueChange = { placeLabel = it },
                label = { Text("Place name (e.g. the gym)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    locationStatus = "Getting a fix…"
                    fetchLocation(context) { location ->
                        if (location == null) {
                            locationStatus = "Couldn't get a fix. Is GPS on?"
                        } else {
                            latitude = location.latitude
                            longitude = location.longitude
                            locationStatus = "Place saved from where you are now"
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (latitude == null) "Use my current location" else "Update saved location")
            }
            locationStatus?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (latitude != null) Emerald else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            SectionLabel("Must be within ${radius.toInt()} m")
            Slider(value = radius, onValueChange = { radius = it },
                valueRange = 50f..1000f, steps = 18)
            Text(
                "Stand at the place before saving. GPS indoors is easily 50 m out.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ---- weightage ----
        Spacer(Modifier.height(20.dp))
        SectionLabel("Weightage: ${weightage.toInt()}%")
        Slider(
            value = weightage,
            onValueChange = { weightage = it },
            valueRange = 0f..ceiling.toFloat().coerceAtLeast(1f),
            steps = if (ceiling >= 20) 19 else 0
        )
        Text(
            if (ceiling <= 0)
                "Today's tasks already add up to 100%. This saves at 0% but still counts toward full unlock."
            else
                "Finishing this unlocks ${weightage.toInt()}% of the daily time. " +
                    "$ceiling% of ${friendlyDate(selectedDateKey, todayKey).lowercase()} is unassigned.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                if (title.isNotBlank()) {
                    viewModel.saveTask(
                        existingId = editTaskId,
                        title = title,
                        duration = duration,
                        recurrence = recurrence,
                        verification = verification,
                        expectedLabels = labelsText.split(",")
                            .map { it.trim() }.filter { it.isNotEmpty() },
                        timerMinutes = timerMinutes.toInt(),
                        latitude = latitude,
                        longitude = longitude,
                        radiusMeters = radius.toInt(),
                        placeLabel = placeLabel,
                        weightage = weightage.toInt(),
                        appPackages = selectedApps.toList(),
                        dateKey = selectedDateKey,
                        scheduledTime = scheduledTime
                    )
                    onDone()
                }
            },
            enabled = title.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Violet),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(if (editTaskId == null) "Save task" else "Save changes")
        }
        Spacer(Modifier.height(24.dp))
    }

    // ---- date picker dialog ----
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDateKey = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ---- time picker dialog ----
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Set a time (optional)") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                    TimePicker(state = timePickerState)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Just a label so you remember when to do it. " +
                            "Notifications for specific times are coming.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scheduledTime = "%02d:%02d".format(
                        timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = {
                    scheduledTime = ""
                    showTimePicker = false
                }) { Text("Clear") }
            }
        )
    }
}

// ---- helpers ----

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** "Today", "Tomorrow", "Sat, 7 Jun 2025", etc. */
private fun friendlyDate(dateKey: String, todayKey: String): String {
    val tomorrow = LocalDate.now().plusDays(1)
        .format(DateTimeFormatter.ISO_LOCAL_DATE)
    return when (dateKey) {
        todayKey -> "Today"
        tomorrow -> "Tomorrow"
        else -> runCatching {
            val d = LocalDate.parse(dateKey)
            val dow = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val mon = d.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            "$dow, ${d.dayOfMonth} $mon ${d.year}"
        }.getOrDefault(dateKey)
    }
}

/** "14:30" → "2:30 PM" */
private fun friendly24h(time: String): String = runCatching {
    LocalTime.parse(time).let { t ->
        val h = if (t.hour % 12 == 0) 12 else t.hour % 12
        val m = "%02d".format(t.minute)
        val ampm = if (t.hour < 12) "AM" else "PM"
        "$h:$m $ampm"
    }
}.getOrDefault(time)
