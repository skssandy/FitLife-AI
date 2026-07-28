package com.fitlife.ai.ui.screens.workout

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    programId: String,
    onFinish: () -> Unit
) {
    var isRunning by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var completedSets by remember { mutableIntStateOf(0) }
    var showRestTimer by remember { mutableStateOf(false) }
    var restTimeRemaining by remember { mutableIntStateOf(60) }

    val exercises = listOf(
        ActiveExercise("Barbell Bench Press", "4", "8-10", "60s"),
        ActiveExercise("Incline Dumbbell Press", "3", "10-12", "45s"),
        ActiveExercise("Cable Flyes", "3", "12-15", "30s"),
        ActiveExercise("Shoulder Press", "4", "8-10", "60s"),
        ActiveExercise("Lateral Raises", "3", "12-15", "30s"),
        ActiveExercise("Tricep Pushdowns", "3", "10-12", "45s"),
    )

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    LaunchedEffect(showRestTimer) {
        if (showRestTimer) {
            restTimeRemaining = 60
            while (restTimeRemaining > 0) {
                delay(1000L)
                restTimeRemaining--
            }
            showRestTimer = false
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upper Body Strength") },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.Default.Close, contentDescription = "End Session")
                    }
                },
                actions = {
                    Text(
                        text = "%02d:%02d".format(minutes, seconds),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { padding ->
        if (showRestTimer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Text(
                        text = "Rest",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "$restTimeRemaining",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.scale(scale)
                    )
                    Text(
                        text = "seconds remaining",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    OutlinedButton(onClick = { showRestTimer = false }) {
                        Text("Skip Rest")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    LinearProgressIndicator(
                        progress = { (currentExerciseIndex.toFloat() / exercises.size) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Exercise ${currentExerciseIndex + 1} of ${exercises.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                itemsIndexed(exercises) { index, exercise ->
                    val isActive = index == currentExerciseIndex
                    val isCompleted = index < currentExerciseIndex

                    ActiveExerciseCard(
                        exercise = exercise,
                        isActive = isActive,
                        isCompleted = isCompleted,
                        completedSets = if (isActive) completedSets else if (isCompleted) exercise.sets.toInt() else 0,
                        onSetComplete = {
                            completedSets++
                            if (completedSets >= exercise.sets.toInt()) {
                                currentExerciseIndex++
                                completedSets = 0
                                if (currentExerciseIndex < exercises.size) {
                                    showRestTimer = true
                                }
                            }
                        }
                    )
                }

                item {
                    if (currentExerciseIndex >= exercises.size) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Workout Complete!", style = MaterialTheme.typography.headlineMedium)
                                Text(
                                    "Great job! You completed all exercises.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = onFinish) {
                                    Text("Save & Finish")
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            if (!isRunning && currentExerciseIndex < exercises.size) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Button(
                        onClick = { isRunning = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (elapsedSeconds == 0) "Start Workout" else "Resume", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

data class ActiveExercise(
    val name: String,
    val sets: String,
    val reps: String,
    val rest: String
)

@Composable
fun ActiveExerciseCard(
    exercise: ActiveExercise,
    isActive: Boolean,
    isCompleted: Boolean,
    completedSets: Int,
    onSetComplete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isActive -> MaterialTheme.colorScheme.primaryContainer
                isCompleted -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isActive) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCompleted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (isActive) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${exercise.sets} sets × ${exercise.reps} reps • Rest ${exercise.rest}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isActive) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Set $completedSets / ${exercise.sets}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    FilledTonalButton(onClick = onSetComplete) {
                        Text("Complete Set")
                    }
                }
            }
        }
    }
}
