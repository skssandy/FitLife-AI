package com.fitlife.ai.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitlife.ai.viewmodel.ProfileViewModel

private val dietOptions = listOf("Vegetarian", "Non-Vegetarian", "Jain", "No preference")
private val mealCountOptions = listOf("3", "4", "5", "6")

private fun dietLabelFor(dietKey: String?): String = when (dietKey?.trim()?.lowercase()) {
    "veg", "vegetarian" -> "Vegetarian"
    "non_veg", "non-veg", "nonveg" -> "Non-Vegetarian"
    "jain" -> "Jain"
    else -> "No preference"
}

private fun dietKeyFor(label: String): String = when (label) {
    "Vegetarian" -> "veg"
    "Non-Vegetarian" -> "non_veg"
    "Jain" -> "jain"
    else -> "any"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.user

    var displayName by remember(user) { mutableStateOf(user?.displayName ?: "") }
    var heightCm by remember(user) { mutableStateOf(user?.heightCm?.toString() ?: "") }
    var weightKg by remember(user) { mutableStateOf(user?.weightKg?.toString() ?: "") }
    var fitnessGoal by remember(user) { mutableStateOf(user?.fitnessGoal ?: "") }
    var dietType by remember(user) { mutableStateOf(dietLabelFor(user?.dietType)) }
    var mealCount by remember(user) { mutableStateOf(user?.mealCount?.toString() ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Profile", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = heightCm,
            onValueChange = { heightCm = it },
            label = { Text("Height (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = weightKg,
            onValueChange = { weightKg = it },
            label = { Text("Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = fitnessGoal,
            onValueChange = { fitnessGoal = it },
            label = { Text("Fitness Goal") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        Text("Dietary Preference", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FlowRow {
            dietOptions.forEach { option ->
                FilterChip(
                    selected = dietType == option,
                    onClick = { dietType = option },
                    label = { Text(option) },
                    modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        Text("Meals Per Day", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FlowRow {
            mealCountOptions.forEach { option ->
                FilterChip(
                    selected = mealCount == option,
                    onClick = { mealCount = option },
                    label = { Text(option) },
                    modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                viewModel.updateProfile(
                    displayName = displayName,
                    heightCm = heightCm.toDoubleOrNull(),
                    weightKg = weightKg.toDoubleOrNull(),
                    dateOfBirth = null,
                    gender = null,
                    fitnessGoal = fitnessGoal.ifBlank { null },
                    activityLevel = null,
                    dietType = dietType.ifBlank { null }?.let { dietKeyFor(it) },
                    mealCount = mealCount.toIntOrNull()
                )
            },
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isSaving) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.height(20.dp))
            else Text("Save Profile")
        }
    }
}
