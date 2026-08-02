package com.fitlife.ai.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitlife.ai.viewmodel.InitialTargets
import com.fitlife.ai.viewmodel.OnboardingViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val steps = listOf("Profile", "Goals", "Activity", "Lifestyle", "Summary")
private val cycleSteps = listOf("Profile", "Goals", "Activity", "Lifestyle", "Cycle", "Summary")
private val genders = listOf("Male", "Female", "Other")
private val goals = listOf("Weight Loss", "Muscle Gain", "Strength", "Endurance", "General Fitness")
private val frequencies = listOf("Rarely", "1-2x / week", "3-4x / week", "5+ / week")
private val activityLevels = listOf("Sedentary", "Light", "Moderate", "Active", "Very Active")
private val equipmentOptions = listOf("Gym", "Home", "Bodyweight", "Dumbbells", "Barbell", "Resistance Bands", "Cardio Machine")
private val sleepOptions = listOf("Less than 6", "6-7", "7-8", "8+")
private val stressLevels = listOf("Low", "Moderate", "High")
private val lifestyleOptions = listOf("None", "Alcohol", "Smoking", "High Sugar", "High Caffeine")

private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isProfileComplete) {
        if (uiState.isProfileComplete) onComplete()
    }

    if (uiState.isLoading) {
        CircularProgressIndicator(modifier = Modifier.fillMaxSize())
        return
    }

    val prefill = uiState.prefill

    var step by remember { mutableStateOf(0) }
    var displayName by remember { mutableStateOf(prefill?.displayName ?: "") }
    var heightCm by remember { mutableStateOf(prefill?.heightCm?.toString() ?: "") }
    var weightKg by remember { mutableStateOf(prefill?.weightKg?.toString() ?: "") }
    var dateOfBirth by remember { mutableStateOf(prefill?.dateOfBirth ?: "") }
    var gender by remember { mutableStateOf(prefill?.gender ?: "") }
    var goal by remember { mutableStateOf(prefill?.fitnessGoal ?: "") }
    var frequency by remember { mutableStateOf(prefill?.workoutFrequency ?: "") }
    var activityLevel by remember { mutableStateOf(prefill?.activityLevel ?: "") }
    var equipment by remember {
        mutableStateOf((prefill?.equipment ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableStateList())
    }
    var injuries by remember { mutableStateOf(prefill?.injuries ?: "") }
    var lifestyle by remember {
        mutableStateOf((prefill?.lifestyle ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableStateList())
    }
    var sleep by remember { mutableStateOf(prefill?.sleepHours?.let { sleepLabelFor(it) } ?: "") }
    var stressLevel by remember { mutableStateOf(prefill?.stressLevel ?: "") }
    var cycleLength by remember { mutableStateOf(prefill?.cycleLength?.toString() ?: "28") }
    var lastPeriodStart by remember { mutableStateOf(prefill?.lastPeriodStart?.let { millisToDateString(it) } ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var pickerTarget by remember { mutableStateOf("") }

    val showCycleStep = gender == "Female"
    val activeSteps = if (showCycleStep) cycleSteps else steps
    val summaryIndex = activeSteps.size - 1

    val sleepHours: Double? = when (sleep) {
        "Less than 6" -> 5.5
        "6-7" -> 6.5
        "7-8" -> 7.5
        "8+" -> 8.5
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Welcome to FitLife AI", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(4.dp))
        StepIndicator(current = step, total = activeSteps.size)

        when (step) {
            0 -> ProfileStep(
                displayName = displayName,
                heightCm = heightCm,
                weightKg = weightKg,
                dateOfBirth = dateOfBirth,
                gender = gender,
                onDisplayNameChange = { displayName = it },
                onHeightChange = { heightCm = it },
                onWeightChange = { weightKg = it },
                onDateOfBirthChange = { dateOfBirth = it },
                onGenderChange = { gender = it },
                onPickDate = { showDatePicker = true }
            )
            1 -> GoalsStep(
                goal = goal,
                frequency = frequency,
                onGoalChange = { goal = it },
                onFrequencyChange = { frequency = it }
            )
            2 -> ActivityStep(
                activityLevel = activityLevel,
                equipment = equipment,
                onActivityChange = { activityLevel = it },
                onEquipmentToggle = { item ->
                    if (equipment.contains(item)) equipment.remove(item) else equipment.add(item)
                }
            )
            3 -> LifestyleStep(
                sleep = sleep,
                stressLevel = stressLevel,
                lifestyle = lifestyle,
                injuries = injuries,
                onSleepChange = { sleep = it },
                onStressChange = { stressLevel = it },
                onLifestyleToggle = { item ->
                    if (lifestyle.contains(item)) lifestyle.remove(item) else lifestyle.add(item)
                },
                onInjuriesChange = { injuries = it }
            )
            4 -> if (showCycleStep) {
                CycleStep(
                    cycleLength = cycleLength,
                    lastPeriodStart = lastPeriodStart,
                    onCycleLengthChange = { cycleLength = it },
                    onLastPeriodChange = { lastPeriodStart = it },
                    onPickDate = {
                        pickerTarget = "period"
                        showDatePicker = true
                    }
                )
            } else {
                SummaryStep(
                    targets = viewModel.computeTargets(
                        heightCm = heightCm.toDoubleOrNull(),
                        weightKg = weightKg.toDoubleOrNull(),
                        dateOfBirth = dateOfBirth.ifBlank { null },
                        gender = gender.ifBlank { null },
                        activityLevel = activityLevel.ifBlank { null },
                        fitnessGoal = goal.ifBlank { null }
                    ),
                    displayName = displayName,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    gender = gender
                )
            }
            5 -> SummaryStep(
                targets = viewModel.computeTargets(
                    heightCm = heightCm.toDoubleOrNull(),
                    weightKg = weightKg.toDoubleOrNull(),
                    dateOfBirth = dateOfBirth.ifBlank { null },
                    gender = gender.ifBlank { null },
                    activityLevel = activityLevel.ifBlank { null },
                    fitnessGoal = goal.ifBlank { null }
                ),
                displayName = displayName,
                heightCm = heightCm,
                weightKg = weightKg,
                gender = gender
            )
        }

        Spacer(Modifier.height(24.dp))

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (step > 0) {
                TextButton(
                    onClick = { step -= 1 },
                    modifier = Modifier.weight(1f)
                ) { Text("Back") }
            }
            if (step < summaryIndex) {
                Button(
                    onClick = { step += 1 },
                    enabled = !(step == 0 && displayName.isBlank()),
                    modifier = Modifier.weight(1f)
                ) { Text("Continue") }
            } else {
                Button(
                    onClick = {
                        viewModel.saveProfile(
                            displayName = displayName,
                            heightCm = heightCm.toDoubleOrNull(),
                            weightKg = weightKg.toDoubleOrNull(),
                            dateOfBirth = dateOfBirth.ifBlank { null },
                            gender = gender.ifBlank { null },
                            fitnessGoal = goal.ifBlank { null },
                            activityLevel = activityLevel.ifBlank { null },
                            workoutFrequency = frequency.ifBlank { null },
                            equipment = equipment.joinToString(",").ifBlank { null },
                            injuries = injuries.ifBlank { null },
                            lifestyle = lifestyle.joinToString(",").ifBlank { null },
                            sleepHours = sleepHours,
                            stressLevel = stressLevel.ifBlank { null },
                            cycleLength = cycleLength.toIntOrNull(),
                            lastPeriodStart = lastPeriodStart.toMillis(),
                            onDone = onComplete
                        )
                    },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isSaving) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.height(20.dp))
                    else Text("Save & Continue")
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
            Text("Skip for now")
        }
    }

    if (showDatePicker) {
        val initialMillis = if (pickerTarget == "period") lastPeriodStart.toMillis() else dateOfBirth.toMillis()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        if (pickerTarget == "period") lastPeriodStart = millis.toDateString()
                        else dateOfBirth = millis.toDateString()
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
}

@Composable
private fun StepIndicator(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        (1..total).forEach { index ->
            FilterChip(
                selected = index <= current + 1,
                onClick = {},
                label = { Text("$index") },
                enabled = false
            )
        }
        Spacer(Modifier.weight(1f))
        Text("Step ${current + 1} of $total", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProfileStep(
    displayName: String,
    heightCm: String,
    weightKg: String,
    dateOfBirth: String,
    gender: String,
    onDisplayNameChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onPickDate: () -> Unit
) {
    Column {
        Text(
            "Tell us a little about yourself so we can personalize your fitness journey.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            label = { Text("Display Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = heightCm,
                onValueChange = onHeightChange,
                label = { Text("Height (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = weightKg,
                onValueChange = onWeightChange,
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = dateOfBirth,
            onValueChange = onDateOfBirthChange,
            label = { Text("Date of Birth") },
            placeholder = { Text("yyyy-MM-dd") },
            readOnly = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { TextButton(onClick = onPickDate) { Text("Pick") } }
        )
        Spacer(Modifier.height(20.dp))

        Text("Gender", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            genders.forEach { g ->
                FilterChip(
                    selected = gender == g,
                    onClick = { onGenderChange(if (gender == g) "" else g) },
                    label = { Text(g) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
private fun GoalsStep(
    goal: String,
    frequency: String,
    onGoalChange: (String) -> Unit,
    onFrequencyChange: (String) -> Unit
) {
    Column {
        Text(
            "What do you want to achieve?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        Text("Fitness Goal", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FlowChipsRow(options = goals, selected = goal, onSelect = onGoalChange)
        Spacer(Modifier.height(20.dp))

        Text("How often do you work out?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FlowChipsRow(options = frequencies, selected = frequency, onSelect = onFrequencyChange)
    }
}

@Composable
private fun ActivityStep(
    activityLevel: String,
    equipment: List<String>,
    onActivityChange: (String) -> Unit,
    onEquipmentToggle: (String) -> Unit
) {
    Column {
        Text(
            "What does a typical week look like for you?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        Text("Activity Level", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FlowChipsRow(options = activityLevels, selected = activityLevel, onSelect = onActivityChange)
        Spacer(Modifier.height(20.dp))

        Text("Equipment Access", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        MultiChipRow(options = equipmentOptions, selected = equipment, onToggle = onEquipmentToggle)
    }
}

@Composable
private fun LifestyleStep(
    sleep: String,
    stressLevel: String,
    lifestyle: List<String>,
    injuries: String,
    onSleepChange: (String) -> Unit,
    onStressChange: (String) -> Unit,
    onLifestyleToggle: (String) -> Unit,
    onInjuriesChange: (String) -> Unit
) {
    Column {
        Text(
            "These help us recommend realistic targets.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        Text("Sleep", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FlowChipsRow(options = sleepOptions, selected = sleep, onSelect = onSleepChange)
        Spacer(Modifier.height(20.dp))

        Text("Stress Level", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FlowChipsRow(options = stressLevels, selected = stressLevel, onSelect = onStressChange)
        Spacer(Modifier.height(20.dp))

        Text("Lifestyle Habits", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        MultiChipRow(options = lifestyleOptions, selected = lifestyle, onToggle = onLifestyleToggle)
        Spacer(Modifier.height(20.dp))

        Text("Injuries & Limitations", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = injuries,
            onValueChange = onInjuriesChange,
            placeholder = { Text("e.g. lower back, knee pain (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CycleStep(
    cycleLength: String,
    lastPeriodStart: String,
    onCycleLengthChange: (String) -> Unit,
    onLastPeriodChange: (String) -> Unit,
    onPickDate: () -> Unit
) {
    Column {
        Text(
            "Understanding your cycle helps us adapt workouts and nutrition by phase.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        Text("Average Cycle Length (days)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FlowChipsRow(options = listOf("21", "24", "28", "32", "35"), selected = cycleLength, onSelect = onCycleLengthChange)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = cycleLength,
            onValueChange = onCycleLengthChange,
            label = { Text("Custom cycle length") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        Text("First Day of Last Period", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = lastPeriodStart,
            onValueChange = onLastPeriodChange,
            label = { Text("Date") },
            placeholder = { Text("yyyy-MM-dd") },
            readOnly = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { TextButton(onClick = onPickDate) { Text("Pick") } }
        )
    }
}

@Composable
private fun SummaryStep(
    targets: InitialTargets,
    displayName: String,
    heightCm: String,
    weightKg: String,
    gender: String
) {
    Column {
        Text("Here are your starting numbers", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        targets.bmi?.let {
            TargetCard("Body Mass Index", String.format(Locale.US, "%.1f", it))
            Spacer(Modifier.height(8.dp))
        }
        targets.tdee?.let {
            TargetCard("Estimated Daily Burn (TDEE)", "$it kcal")
            Spacer(Modifier.height(8.dp))
        }
        targets.calorieTarget?.let {
            TargetCard("Daily Calorie Target", "$it kcal")
            Spacer(Modifier.height(8.dp))
        }
        targets.proteinTarget?.let {
            TargetCard("Daily Protein Target", "$it g")
            Spacer(Modifier.height(8.dp))
        }

        if (targets.bmi == null) {
            Text(
                "Enter your height and weight in step 1 to see your starting numbers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Your profile", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text("Name: ${displayName.ifBlank { "-" }}", style = MaterialTheme.typography.bodyMedium)
                Text("Height: ${heightCm.ifBlank { "-" }} cm · Weight: ${weightKg.ifBlank { "-" }} kg", style = MaterialTheme.typography.bodyMedium)
                Text("Gender: ${gender.ifBlank { "-" }}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TargetCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FlowChipsRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    FilterChip(
                        selected = selected == option,
                        onClick = { onSelect(if (selected == option) "" else option) },
                        label = { Text(option) },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiChipRow(
    options: List<String>,
    selected: List<String>,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    FilterChip(
                        selected = selected.contains(option),
                        onClick = { onToggle(option) },
                        label = { Text(option) },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }
    }
}

private fun sleepLabelFor(hours: Double): String = when {
    hours < 6 -> "Less than 6"
    hours < 7 -> "6-7"
    hours < 8 -> "7-8"
    else -> "8+"
}

private fun millisToDateString(millis: Long): String = dateFormatter.format(java.util.Date(millis))

private fun String.toMillis(): Long? {
    if (isBlank()) return null
    return try {
        dateFormatter.parse(this)?.time
    } catch (e: Exception) {
        null
    }
}

private fun Long.toDateString(): String = dateFormatter.format(java.util.Date(this))
