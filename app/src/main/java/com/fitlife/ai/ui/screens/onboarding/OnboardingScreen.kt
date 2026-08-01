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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitlife.ai.viewmodel.OnboardingViewModel

private val genders = listOf("Male", "Female", "Other")
private val goals = listOf("Weight Loss", "Muscle Gain", "Endurance", "General Fitness")
private val activityLevels = listOf("Sedentary", "Light", "Moderate", "Active")

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

    var displayName by remember { mutableStateOf(uiState.prefill?.displayName ?: "") }
    var heightCm by remember { mutableStateOf(uiState.prefill?.heightCm?.toString() ?: "") }
    var weightKg by remember { mutableStateOf(uiState.prefill?.weightKg?.toString() ?: "") }
    var gender by remember { mutableStateOf(uiState.prefill?.gender ?: "") }
    var goal by remember { mutableStateOf(uiState.prefill?.fitnessGoal ?: "") }
    var activityLevel by remember { mutableStateOf(uiState.prefill?.activityLevel ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Welcome to FitLife AI", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tell us a little about yourself so we can personalize your fitness journey.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = heightCm,
                onValueChange = { heightCm = it },
                label = { Text("Height (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = weightKg,
                onValueChange = { weightKg = it },
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(20.dp))

        Text("Gender", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            genders.forEach { g ->
                FilterChip(
                    selected = gender == g,
                    onClick = { gender = if (gender == g) "" else g },
                    label = { Text(g) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        Text("Fitness Goal", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FlowChipsRow(options = goals, selected = goal) { goal = it }
        Spacer(Modifier.height(20.dp))

        Text("Activity Level", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FlowChipsRow(options = activityLevels, selected = activityLevel) { activityLevel = it }
        Spacer(Modifier.height(24.dp))

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                viewModel.saveProfile(
                    displayName = displayName,
                    heightCm = heightCm.toDoubleOrNull(),
                    weightKg = weightKg.toDoubleOrNull(),
                    dateOfBirth = null,
                    gender = gender.ifBlank { null },
                    fitnessGoal = goal.ifBlank { null },
                    activityLevel = activityLevel.ifBlank { null },
                    onDone = onComplete
                )
            },
            enabled = displayName.isNotBlank() && !uiState.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isSaving) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.height(20.dp))
            else Text("Save & Continue")
        }
        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
            Text("Skip for now")
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
