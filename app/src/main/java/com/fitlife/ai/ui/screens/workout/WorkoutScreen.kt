package com.fitlife.ai.ui.screens.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    onProgramClick: (String) -> Unit = {},
    onStartSession: (String) -> Unit = {}
) {
    val programs = listOf(
        WorkoutProgramItem("1", "Upper Body Strength", "Build upper body muscle & strength", "Beginner", "4 weeks", Icons.Default.FitnessCenter),
        WorkoutProgramItem("2", "HIIT Cardio Burn", "High-intensity interval training", "Intermediate", "6 weeks", Icons.Default.LocalFireDepartment),
        WorkoutProgramItem("3", "Core & Flexibility", "Strengthen core, improve flexibility", "Beginner", "4 weeks", Icons.Default.SelfImprovement),
        WorkoutProgramItem("4", "Full Body Power", "Total body compound movements", "Advanced", "8 weeks", Icons.Default.Bolt),
        WorkoutProgramItem("5", "AI Custom Plan", "Personalized by your AI coach", "All Levels", "Variable", Icons.Default.SmartToy),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workouts") },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Add, contentDescription = "Create Program")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Workout Programs",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(programs) { program ->
                WorkoutProgramCard(
                    program = program,
                    onClick = { onProgramClick(program.id) },
                    onStart = { onStartSession(program.id) }
                )
            }
        }
    }
}

data class WorkoutProgramItem(
    val id: String,
    val name: String,
    val description: String,
    val difficulty: String,
    val duration: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun WorkoutProgramCard(
    program: WorkoutProgramItem,
    onClick: () -> Unit,
    onStart: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = program.icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(program.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        program.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(program.difficulty) },
                    leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = {},
                    label = { Text(program.duration) },
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Program")
            }
        }
    }
}
