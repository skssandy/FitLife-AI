package com.fitlife.ai.ui.screens.cycle

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitlife.ai.util.CycleCalculator
import com.fitlife.ai.util.CyclePhase
import com.fitlife.ai.util.SupportMode
import com.fitlife.ai.util.SymptomCatalog
import com.fitlife.ai.viewmodel.CycleViewModel
import com.fitlife.ai.viewmodel.PhaseSnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CycleScreen(
    onBack: () -> Unit = {},
    viewModel: CycleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.user
    val mode = viewModel.supportMode()
    val phaseInfo = viewModel.phaseInfo()

    var logOffsetDays by rememberSaveable { mutableStateOf(0) }
    var flowLevel by rememberSaveable { mutableStateOf("Medium") }
    var notes by rememberSaveable { mutableStateOf("") }
    var logSymptoms by rememberSaveable { mutableStateOf(listOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
        if (user == null) return

        SupportModeSelector(selected = mode, onSelect = { viewModel.setSupportMode(it) })
        Spacer(Modifier.height(16.dp))

        val cycleActive = phaseInfo != null && mode in listOf(SupportMode.STANDARD, SupportMode.TTC, SupportMode.PCOS)
        if (cycleActive) {
            phaseInfo?.let { snapshot ->
                PhaseCard(snapshot = snapshot)
                Spacer(Modifier.height(16.dp))
                if (mode != SupportMode.PCOS) {
                    FertilityCard(snapshot = snapshot, todayMillis = System.currentTimeMillis())
                    Spacer(Modifier.height(16.dp))
                }
                GuidanceCard(title = "Training", body = if (mode == SupportMode.STANDARD) snapshot.phase.training else mode.training)
                Spacer(Modifier.height(16.dp))
                GuidanceCard(title = "Nutrition", body = if (mode == SupportMode.STANDARD) snapshot.phase.nutrition else mode.nutrition)
                if (mode == SupportMode.STANDARD) {
                    Spacer(Modifier.height(16.dp))
                    NutritionAdjustmentCard(snapshot = snapshot, calorieTarget = user.calorieTarget)
                }
                Spacer(Modifier.height(16.dp))
                CycleLengthSelector(
                    selected = snapshot.cycleLength,
                    onSelect = { viewModel.setCycleLength(it) }
                )
            }
        } else if (mode in listOf(SupportMode.PREGNANCY, SupportMode.POSTPARTUM, SupportMode.MENOPAUSE)) {
            GuidanceCard(title = "Training", body = mode.training)
            Spacer(Modifier.height(16.dp))
            GuidanceCard(title = "Nutrition", body = mode.nutrition)
            Spacer(Modifier.height(16.dp))
            Text(
                "Cycle phase tracking is paused in ${
                    mode.displayName
                } mode. You can still log symptoms and period history.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (user.lastPeriodStart == null || user.lastPeriodStart == 0L) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("No cycle data yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Log the first day of your last period to unlock phase-aware training and nutrition guidance.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        PeriodLogCard(
            offsetDays = logOffsetDays,
            onOffsetChange = { logOffsetDays = it },
            flowLevel = flowLevel,
            onFlowChange = { flowLevel = it },
            notes = notes,
            onNotesChange = { notes = it },
            symptoms = logSymptoms,
            onSymptomsChange = { logSymptoms = it },
            saving = uiState.saving,
            onLog = {
                viewModel.logPeriod(
                    startDateMillis = dayOffsetMillis(logOffsetDays),
                    flowLevel = flowLevel,
                    symptoms = logSymptoms,
                    notes = notes
                )
                logSymptoms = emptyList()
                notes = ""
            }
        )
        Spacer(Modifier.height(16.dp))

        SymptomTrackerCard(
            selected = uiState.todaySymptoms,
            onToggle = { viewModel.toggleSymptom(it) }
        )
        Spacer(Modifier.height(16.dp))

        if (uiState.entries.isNotEmpty()) {
            HistoryCard(entries = uiState.entries)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupportModeSelector(selected: SupportMode, onSelect: (SupportMode) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Support mode", style = MaterialTheme.typography.titleMedium)
            Text(
                selected.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SupportMode.entries.forEach { mode ->
                    FilterChip(
                        selected = mode == selected,
                        onClick = { onSelect(mode) },
                        label = { Text(mode.displayName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhaseCard(snapshot: PhaseSnapshot) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(snapshot.phase.displayName, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Day ${snapshot.day} of ${snapshot.cycleLength}" +
                    (snapshot.nextPeriodMillis?.let { " · Next period ~${formatDate(it)}" } ?: ""),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(snapshot.phase.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FertilityCard(snapshot: PhaseSnapshot, todayMillis: Long) {
    val start = snapshot.fertileStartMillis ?: return
    val end = snapshot.fertileEndMillis ?: return
    val inWindow = todayMillis in start..end
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (inWindow) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Fertile window", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "${formatRange(start, end)}" + if (inWindow) " — You are in your fertile window now" else "",
                style = MaterialTheme.typography.bodyMedium
            )
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

@Composable
private fun NutritionAdjustmentCard(snapshot: PhaseSnapshot, calorieTarget: Int?) {
    val adj = CycleCalculator.nutritionAdjustment(snapshot.phase, calorieTarget)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Phase nutrition adjustments", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (adj.calorieDelta > 0) {
                Text(
                    "Add ~${adj.calorieDelta} kcal/day (about +${(adj.calorieDelta * 100 / (calorieTarget ?: 2000).coerceAtLeast(1))}% of your target)",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text("Keep your current calorie target for this phase.", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text(adj.proteinGuidance, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(adj.hydrationGuidance, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CycleLengthSelector(selected: Int, onSelect: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Cycle length", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(21, 28, 35).forEach { length ->
                    OutlinedButton(onClick = { onSelect(length) }, modifier = Modifier.weight(1f)) {
                        Text(if (length == selected) "$length ✓" else "$length")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodLogCard(
    offsetDays: Int,
    onOffsetChange: (Int) -> Unit,
    flowLevel: String,
    onFlowChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    symptoms: List<String>,
    onSymptomsChange: (List<String>) -> Unit,
    saving: Boolean,
    onLog: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Log a period start", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Start date", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Today", 1 to "Yesterday", 2 to "2 days ago").forEach { (offset, label) ->
                    OutlinedButton(onClick = { onOffsetChange(offset) }, modifier = Modifier.weight(1f)) {
                        Text(if (offsetDays == offset) "$label ✓" else label)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Flow", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Light", "Medium", "Heavy").forEach { level ->
                    FilterChip(
                        selected = flowLevel == level,
                        onClick = { onFlowChange(level) },
                        label = { Text(level) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Symptoms (optional)", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SymptomCatalog.all.filter { it.id in setOf("cramps", "bloating", "headache", "fatigue", "nausea", "backache", "mood_swings") }
                    .forEach { symptom ->
                        FilterChip(
                            selected = symptom.id in symptoms,
                            onClick = {
                                onSymptomsChange(
                                    if (symptom.id in symptoms) symptoms - symptom.id else symptoms + symptom.id
                                )
                            },
                            label = { Text(symptom.name) }
                        )
                    }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onLog,
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving
            ) {
                Text(if (saving) "Saving…" else "Save Period Start")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SymptomTrackerCard(selected: List<String>, onToggle: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Today's symptoms", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Tap to log. Selections are saved to today's symptom log.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            SymptomCatalog.categories.forEach { category ->
                Text(category, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SymptomCatalog.byCategory(category).forEach { symptom ->
                        FilterChip(
                            selected = symptom.id in selected,
                            onClick = { onToggle(symptom.id) },
                            label = { Text(symptom.name) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HistoryCard(entries: List<com.fitlife.ai.data.local.entity.CycleEntryEntity>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Period history", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            entries.take(10).forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                Column {
                    Text(formatDate(entry.startDate), style = MaterialTheme.typography.titleSmall)
                    Text(
                        listOfNotNull(
                            entry.flowLevel.takeIf { it.isNotBlank() },
                            decodeSymptomsCount(entry.symptomsJson).let {
                                if (it > 0) "$it symptoms" else null
                            }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    entry.notes.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))

private fun formatRange(startMillis: Long, endMillis: Long): String =
    "${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(startMillis))} – " +
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(endMillis))

private fun dayOffsetMillis(daysAgo: Int): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun decodeSymptomsCount(json: String): Int {
    val trimmed = json.trim()
    if (trimmed.length < 2) return 0
    val inner = trimmed.substring(1, trimmed.length - 1)
    if (inner.isBlank()) return 0
    return inner.split(",").count { it.trim().isNotEmpty() }
}
