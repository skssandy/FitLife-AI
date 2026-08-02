package com.fitlife.ai.ui.screens.health

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.fitlife.ai.data.health.DetectedExerciseSession
import com.fitlife.ai.data.local.entity.DailyMetricEntity
import com.fitlife.ai.viewmodel.HealthUiState
import com.fitlife.ai.viewmodel.HealthViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HealthScreen(
    onBack: () -> Unit = {},
    viewModel: HealthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(viewModel.permissionContract) {
        viewModel.refresh()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Health Connect", style = MaterialTheme.typography.headlineMedium)
                    Text("Wearables, activity & recovery", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onBack) { Text("Back") }
            }
        }

        uiState.error?.let {
            item {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        if (uiState.isLoading) {
            item { CircularProgressIndicator(modifier = Modifier.padding(16.dp)) }
        }

        item {
            ConnectionCard(
                uiState = uiState,
                onConnect = { permissionLauncher.launch(viewModel.requiredPermissions) },
                onSync = viewModel::refresh
            )
        }

        item {
            ActivityScoreCard(score = uiState.activityScore)
        }

        item {
            TodayMetricsCard(metric = uiState.today)
        }

        if (uiState.detectedWorkouts.isNotEmpty()) {
            item { Text("Detected Workouts", style = MaterialTheme.typography.titleLarge) }
            items(uiState.detectedWorkouts) { session ->
                DetectedWorkoutCard(session = session, onLog = { viewModel.logDetectedWorkout(session) })
            }
        }

        item {
            Text("Manual Entry", style = MaterialTheme.typography.titleLarge)
            Text(
                "No wearables? Log your metrics manually.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            ManualEntryCard(viewModel = viewModel)
        }

        if (uiState.weeklyMetrics.isNotEmpty()) {
            item { Text("This Week", style = MaterialTheme.typography.titleLarge) }
            item {
                WeeklyStepsChart(uiState = uiState)
            }
        }
    }
}

@Composable
private fun ConnectionCard(uiState: HealthUiState, onConnect: () -> Unit, onSync: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.isAvailable && uiState.permissionsGranted)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Connection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        statusText(uiState),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (uiState.isAvailable && uiState.permissionsGranted) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(8.dp))
            uiState.lastSync?.let {
                Text(
                    "Last synced: ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            if (!uiState.isAvailable) {
                Text(
                    "Health Connect is not available on this device. Use manual entry below to track your metrics.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (!uiState.permissionsGranted) {
                Text(
                    "Allow FitLife to read your steps, heart rate, sleep, and body metrics from Health Connect.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Sync, null, modifier = Modifier.size(18.dp))
                    Text(" Connect to Health Connect")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onSync, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Text(" Sync Now")
                    }
                }
            }
        }
    }
}

@Composable
private fun statusText(uiState: HealthUiState): String = when {
    !uiState.isAvailable -> "Not available on this device"
    !uiState.permissionsGranted -> "Not connected"
    else -> "Connected — reading your metrics"
}

@Composable
private fun ActivityScoreCard(score: Int) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Ring(
                progress = score / 100f,
                size = 88.dp,
                strokeWidth = 10.dp,
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surface
            )
            Column {
                Text("Daily Activity Score", style = MaterialTheme.typography.titleMedium)
                Text("$score", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Steps 25% · Workout 25% · Sleep 20% · HRV 15% · Active 15%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TodayMetricsCard(metric: DailyMetricEntity?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Today's Metrics", style = MaterialTheme.typography.titleMedium)
            if (metric == null || (metric.steps == null && metric.heartRateAvg == null && metric.hrvAvg == null &&
                    metric.sleepMinutes == null && metric.caloriesBurned == null && metric.weightKg == null)
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "No data yet today. Sync Health Connect or log manually below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(Icons.Default.DirectionsRun, "Steps", metric?.steps?.let { "$it" } ?: "—", Modifier.weight(1f), MaterialTheme.colorScheme.primary)
                    MetricCard(Icons.Default.Favorite, "Heart", metric?.heartRateAvg?.let { "$it bpm" } ?: "—", Modifier.weight(1f), MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(Icons.Default.MonitorHeart, "HRV", metric?.hrvAvg?.let { "$it ms" } ?: "—", Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
                    MetricCard(Icons.Default.Bedtime, "Sleep", metric?.sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "—", Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(Icons.Default.LocalFireDepartment, "Calories", metric?.caloriesBurned?.let { "$it kcal" } ?: "—", Modifier.weight(1f), MaterialTheme.colorScheme.primary)
                    MetricCard(Icons.Default.Speed, "Weight", metric?.weightKg?.let { "$it kg" } ?: "—", Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
                }
                metric.bodyFatPct?.let {
                    Spacer(Modifier.height(10.dp))
                    Text("Body fat: $it%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(icon: ImageVector, label: String, value: String, modifier: Modifier, color: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetectedWorkoutCard(session: DetectedExerciseSession, onLog: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.DirectionsRun, null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(session.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${session.durationMinutes} min" + (session.calories?.let { " · $it kcal" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onLog) { Text("Log") }
        }
    }
}

@Composable
private fun ManualEntryCard(viewModel: HealthViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            ManualRow("Steps", "", KeyboardType.Number) { value ->
                viewModel.addManualSteps(value)
            }
            Spacer(Modifier.height(6.dp))
            ManualRow("Sleep (hours)", "", KeyboardType.Number) { value ->
                viewModel.addManualSleep(value * 60)
            }
            Spacer(Modifier.height(6.dp))
            ManualRow("Weight (kg)", "", KeyboardType.Decimal) { value ->
                viewModel.addManualWeight(value.toDouble())
            }
            Spacer(Modifier.height(6.dp))
            ManualRow("Body fat (%)", "", KeyboardType.Decimal) { value ->
                viewModel.addManualBodyFat(value.toDouble())
            }
        }
    }
}

@Composable
private fun ManualRow(label: String, suffix: String, keyboardType: KeyboardType, onSave: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            modifier = Modifier.width(110.dp),
            placeholder = { Text(suffix) }
        )
        TextButton(
            onClick = {
                val value = text.toIntOrNull()
                if (value != null) onSave(value)
                text = ""
            },
            enabled = text.isNotEmpty()
        ) { Text("Save") }
    }
}

@Composable
private fun WeeklyStepsChart(uiState: HealthUiState) {
    val entries = (6 downTo 0).map { daysAgo ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis
        val metric = uiState.weeklyMetrics.firstOrNull { it.date == dayStart }
        val label = SimpleDateFormat("E", Locale.getDefault()).format(Date(dayStart))
        label to (metric?.steps ?: 0)
    }
    val max = (entries.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            entries.forEach { (label, value) ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (value > 0) "$value" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    val barHeight = if (value > 0) (value.toFloat() / max).coerceIn(0.08f, 1f) else 0.04f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((90 * barHeight).dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Ring(
    progress: Float,
    size: Dp,
    strokeWidth: Dp,
    color: Color,
    trackColor: Color
) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val inset = strokeWidth.toPx() / 2
        val arcSize = Size(size.toPx() - inset * 2, size.toPx() - inset * 2)
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = stroke
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = stroke
        )
    }
}
