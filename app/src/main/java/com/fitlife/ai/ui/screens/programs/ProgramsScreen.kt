package com.fitlife.ai.ui.screens.programs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitlife.ai.data.WorkoutProgramTemplate
import com.fitlife.ai.data.local.entity.WorkoutProgramEntity
import com.fitlife.ai.viewmodel.ProgramsViewModel

@Composable
fun ProgramsScreen(
    viewModel: ProgramsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPresets by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showPresets = true }) {
                Icon(Icons.Default.Add, "Add program")
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
            item { Text("Programs", style = MaterialTheme.typography.headlineMedium) }

            if (uiState.isLoading) {
                item { CircularProgressIndicator() }
            }

            if (uiState.programs.isEmpty()) {
                item {
                    Text(
                        "No programs yet. Tap + to browse preset splits.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(uiState.programs) { program ->
                ProgramCard(
                    program = program,
                    expanded = expandedId == program.id,
                    onToggle = { expandedId = if (expandedId == program.id) null else program.id },
                    onDelete = { viewModel.deleteProgram(program.id) },
                    viewModel = viewModel
                )
            }
        }
    }

    if (showPresets) {
        PresetsDialog(
            onDismiss = { showPresets = false },
            onSelect = { template ->
                viewModel.addPreset(template)
                showPresets = false
            },
            viewModel = viewModel
        )
    }
}

@Composable
private fun ProgramCard(
    program: WorkoutProgramEntity,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    viewModel: ProgramsViewModel
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(program.name, style = MaterialTheme.typography.titleMedium)
                    Text(program.description, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete program")
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onToggle) {
                Text(if (expanded) "Hide days" else "Show days (${viewModel.parseDays(program.daysJson).size})")
            }
            if (expanded) {
                val days = viewModel.parseDays(program.daysJson)
                days.forEach { day ->
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(10.dp).fillMaxWidth()) {
                            Text(day.name, style = MaterialTheme.typography.titleSmall)
                            day.exercises.forEach { ex ->
                                Text(
                                    viewModel.exerciseLabel(ex),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetsDialog(
    onDismiss: () -> Unit,
    onSelect: (WorkoutProgramTemplate) -> Unit,
    viewModel: ProgramsViewModel
) {
    val presets = viewModel.presets()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Browse Presets") },
        text = {
            Column {
                presets.forEach { preset ->
                    Card(
                        onClick = { onSelect(preset) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text(preset.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${preset.goal} · ${preset.days.size} days/week",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
