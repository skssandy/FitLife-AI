package com.fitlife.ai.ui.screens.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.fitlife.ai.data.local.entity.WorkoutEntity
import com.fitlife.ai.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<WorkoutEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, "Add workout")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Text("Workouts", style = MaterialTheme.typography.headlineMedium) }

            if (uiState.workouts.isEmpty()) {
                item { Text("No workouts yet. Tap + to add one.", style = MaterialTheme.typography.bodyMedium) }
            }

            items(uiState.workouts) { workout ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(workout.exerciseName, style = MaterialTheme.typography.titleSmall)
                                Row {
                                    Text("${workout.sets}x${workout.reps}", style = MaterialTheme.typography.bodyMedium)
                                    workout.weightKg?.let { Text(" · ${it}kg", style = MaterialTheme.typography.bodyMedium) }
                                    workout.caloriesBurned?.let { Text(" · ${it}cal", style = MaterialTheme.typography.bodyMedium) }
                                }
                                workout.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            IconButton(onClick = { editing = workout; showDialog = true }) {
                                Icon(Icons.Default.Edit, "Edit")
                            }
                            IconButton(onClick = { viewModel.deleteWorkout(workout.id) }) {
                                Icon(Icons.Default.Delete, "Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        WorkoutDialog(
            initial = editing,
            onDismiss = { showDialog = false; editing = null },
            onAdd = { name, sets, reps, weight, duration, calories, notes ->
                val existing = editing
                if (existing != null) {
                    viewModel.updateWorkout(
                        existing.copy(
                            exerciseName = name,
                            sets = sets,
                            reps = reps,
                            weightKg = weight,
                            durationMinutes = duration,
                            caloriesBurned = calories,
                            notes = notes
                        )
                    )
                } else {
                    viewModel.addWorkout(name, sets, reps, weight, duration, calories, notes)
                }
                showDialog = false
                editing = null
            }
        )
    }
}

@Composable
fun WorkoutDialog(
    initial: WorkoutEntity?,
    onDismiss: () -> Unit,
    onAdd: (String, Int, Int, Double?, Int?, Int?, String?) -> Unit
) {
    var name by remember { mutableStateOf(initial?.exerciseName ?: "") }
    var sets by remember { mutableStateOf(initial?.sets?.toString() ?: "") }
    var reps by remember { mutableStateOf(initial?.reps?.toString() ?: "") }
    var weight by remember { mutableStateOf(initial?.weightKg?.toString() ?: "") }
    var duration by remember { mutableStateOf(initial?.durationMinutes?.toString() ?: "") }
    var calories by remember { mutableStateOf(initial?.caloriesBurned?.toString() ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial != null) "Edit Workout" else "Add Workout") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Exercise Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedTextField(value = sets, onValueChange = { sets = it }, label = { Text("Sets") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Reps") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight kg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = calories, onValueChange = { calories = it }, label = { Text("Calories") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (min)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    val s = sets.toIntOrNull() ?: return@TextButton
                    val r = reps.toIntOrNull() ?: return@TextButton
                    if (name.isBlank()) return@TextButton
                    onAdd(name, s, r, weight.toDoubleOrNull(), duration.toIntOrNull(), calories.toIntOrNull(), notes.ifBlank { null })
                },
                enabled = name.isNotBlank()
            ) { Text(if (initial != null) "Save" else "Add") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
