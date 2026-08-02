package com.fitlife.ai.ui.screens.cycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitlife.ai.util.CycleCalculator
import com.fitlife.ai.util.CyclePhase
import com.fitlife.ai.viewmodel.CycleViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CycleScreen(
    onBack: () -> Unit = {},
    viewModel: CycleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.user

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column {
                Text("Cycle Tracker", style = MaterialTheme.typography.headlineMedium)
                Text("Adapt workouts and nutrition by phase", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
        Spacer(Modifier.height(16.dp))

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.fillMaxWidth().padding(16.dp))
            return
        }

        if (user?.lastPeriodStart == null || user.lastPeriodStart == 0L) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("No cycle data yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Log the first day of your last period to unlock phase-aware training and nutrition guidance.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.logPeriodToday() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Log Period Start Today")
                    }
                }
            }
            return
        }

        val phaseInfo = viewModel.phaseInfo()
        if (phaseInfo != null) {
            val (phase, day, nextPeriod) = phaseInfo
            val cycleLength = (user.cycleLength ?: 28).coerceAtLeast(21)

            PhaseCard(phase = phase, day = day, cycleLength = cycleLength, nextPeriodMillis = nextPeriod)
            Spacer(Modifier.height(16.dp))
            GuidanceCard(title = "Training", body = phase.training)
            Spacer(Modifier.height(16.dp))
            GuidanceCard(title = "Nutrition", body = phase.nutrition)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(21, 28, 35).forEach { length ->
                    OutlinedButton(onClick = { viewModel.setCycleLength(length) }, modifier = Modifier.weight(1f)) {
                        Text(if (length == cycleLength) "$length ✓" else "$length")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { viewModel.logPeriodToday() }, modifier = Modifier.fillMaxWidth()) {
                Text("Log Period Start Today")
            }
        }
    }
}

@Composable
private fun PhaseCard(phase: CyclePhase, day: Int, cycleLength: Int, nextPeriodMillis: Long?) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(phase.displayName, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Day $day of $cycleLength · " + (nextPeriodMillis?.let { "Next period ~${formatDate(it)}" } ?: ""),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(phase.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun GuidanceCard(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
