package com.fitlife.ai.ui.screens.programs

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fitlife.ai.data.ExerciseLibrary
import com.fitlife.ai.data.ProgramExercise
import com.fitlife.ai.data.WorkoutProgramTemplate
import com.fitlife.ai.data.local.entity.WorkoutProgramEntity
import com.fitlife.ai.viewmodel.ProgramsViewModel

@Composable
fun ProgramsScreen(
    onBack: () -> Unit = {},
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Programs", style = MaterialTheme.typography.headlineMedium)
                    OutlinedButton(onClick = onBack) { Text("Back") }
                }
            }

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
    var expandedExercise by remember { mutableStateOf<String?>(null) }
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
                days.forEachIndexed { dayIndex, day ->
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(10.dp).fillMaxWidth()) {
                            Text(day.name, style = MaterialTheme.typography.titleSmall)
                            day.exercises.forEachIndexed { exIndex, ex ->
                                ExerciseRow(
                                    exercise = ex,
                                    expanded = expandedExercise == "$dayIndex-$exIndex",
                                    onToggle = {
                                        expandedExercise =
                                            if (expandedExercise == "$dayIndex-$exIndex") null else "$dayIndex-$exIndex"
                                    }
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
private fun ExerciseRow(
    exercise: ProgramExercise,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onToggle, modifier = Modifier.weight(1f)) {
            Text(
                "${exercise.name} · ${exercise.sets} × ${exercise.reps}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
        IconButton(onClick = onToggle) {
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Hide details" else "Show details"
            )
        }
    }
    if (expanded) {
        val info = ExerciseLibrary.get(exercise.name)
        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            info.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = exercise.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                "Sets: ${exercise.sets}   Reps: ${exercise.reps}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(info.instructions, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(info.videoUrl))
                    )
                }
            ) {
                Text("Watch on YouTube")
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
                if (viewModel.uiState.value.userEquipment.isNotEmpty()) {
                    Text(
                        "Showing programs that match your equipment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                }
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
                            Text(
                                viewModel.equipmentLabel(preset),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
