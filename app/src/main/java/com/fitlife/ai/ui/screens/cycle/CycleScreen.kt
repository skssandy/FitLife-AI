package com.fitlife.ai.ui.screens.cycle

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitlife.ai.data.local.entity.CycleDayEntity
import com.fitlife.ai.util.CycleCalculator
import com.fitlife.ai.util.CyclePhase
import com.fitlife.ai.util.SupportMode
import com.fitlife.ai.util.SymptomCatalog
import com.fitlife.ai.viewmodel.CycleViewModel
import com.fitlife.ai.viewmodel.MoodOption
import com.fitlife.ai.viewmodel.PhaseSnapshot
import com.fitlife.ai.worker.CycleReminderScheduler
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
    val context = LocalContext.current
    val onBirthControl = viewModel.isOnBirthControl()

    var logOffsetDays by rememberSaveable { mutableStateOf(0) }
    var notes by rememberSaveable { mutableStateOf("") }
    var logSymptoms by rememberSaveable { mutableStateOf(listOf<String>()) }

    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var cycleRemindersEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_CYCLE_REMINDERS, false)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            setCycleReminders(context, true)
            cycleRemindersEnabled = true
        }
    }

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
                PeriodStatusBanner(
                    snapshot = snapshot,
                    todayMillis = System.currentTimeMillis(),
                    onLogStarted = { viewModel.markPeriodStarted() }
                )
                Spacer(Modifier.height(16.dp))
                CycleCalendar(
                    lastPeriodStart = user.lastPeriodStart,
                    cycleLength = snapshot.cycleLength,
                    periodLength = snapshot.periodLengthDays,
                    entries = uiState.entries,
                    phaseInfo = snapshot,
                    suppressFertile = onBirthControl
                )
                Spacer(Modifier.height(16.dp))
                PhaseCard(snapshot = snapshot)
                Spacer(Modifier.height(16.dp))
                if (!onBirthControl && mode != SupportMode.PCOS) {
                    FertilityCard(snapshot = snapshot, todayMillis = System.currentTimeMillis())
                    Spacer(Modifier.height(16.dp))
                }
                GuidanceCard(title = "Training", body = if (mode == SupportMode.STANDARD) snapshot.phase.training else mode.training)
                Spacer(Modifier.height(16.dp))
                GuidanceCard(title = "Nutrition", body = if (mode == SupportMode.STANDARD) snapshot.phase.nutrition else mode.nutrition)
                Spacer(Modifier.height(16.dp))
                PhasePicksCard(phase = snapshot.phase)
                if (mode == SupportMode.STANDARD) {
                    Spacer(Modifier.height(16.dp))
                    NutritionAdjustmentCard(snapshot = snapshot, calorieTarget = user.calorieTarget)
                }
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
                    Text("Period not started yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Log the first day of your last period to unlock phase-aware training and nutrition guidance. You can also set it below.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        CycleDetailsCard(
            lastPeriodStart = user.lastPeriodStart,
            cycleLength = user.cycleLength ?: 28,
            birthControl = user.birthControl,
            onLastPeriodStartChange = { viewModel.setLastPeriodStart(it) },
            onCycleLengthChange = { viewModel.setCycleLength(it) },
            onBirthControlChange = { viewModel.setBirthControl(it) },
            onShareReport = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "FitLife AI Cycle Report")
                    putExtra(Intent.EXTRA_TEXT, viewModel.buildCycleReport())
                }
                context.startActivity(Intent.createChooser(intent, "Share cycle report"))
            },
            cycleRemindersEnabled = cycleRemindersEnabled,
            onCycleRemindersChange = { enabled ->
                if (enabled) {
                    if (android.os.Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        setCycleReminders(context, true)
                        cycleRemindersEnabled = true
                    }
                } else {
                    setCycleReminders(context, false)
                    cycleRemindersEnabled = false
                }
            }
        )
        Spacer(Modifier.height(16.dp))

        PeriodLogCard(
            offsetDays = logOffsetDays,
            onOffsetChange = { logOffsetDays = it },
            defaultDuration = viewModel.lastDurationDays(),
            notes = notes,
            onNotesChange = { notes = it },
            symptoms = logSymptoms,
            onSymptomsChange = { logSymptoms = it },
            saving = uiState.saving,
            onLog = { duration ->
                viewModel.logPeriod(
                    startDateMillis = dayOffsetMillis(logOffsetDays),
                    durationDays = duration,
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

        DailyJournalCard(
            journal = viewModel.todayJournal(),
            saving = uiState.saving,
            moodOptions = CycleViewModel.moodOptions,
            onSave = { note, moodId, weightKg ->
                viewModel.saveJournal(note, moodId, weightKg)
            }
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
    val subtitle = when {
        snapshot.day <= 0 ->
            "Cycle tracking starts ${formatDate(snapshot.lastPeriodStartMillis)} · Log your period to begin"
        snapshot.confirmedBleedingDay > 0 ->
            "Day ${snapshot.day} of ${snapshot.cycleLength} · Period in progress (Day ${snapshot.confirmedBleedingDay} of ${snapshot.periodLengthDays})"
        snapshot.expectedBleedingDay > 0 ->
            "Day ${snapshot.day} of ${snapshot.cycleLength} · Expected period window — log it when it starts"
        else ->
            "Day ${snapshot.day} of ${snapshot.cycleLength}" +
                (snapshot.nextPeriodMillis?.let { " · Next period ~${formatDate(it)}" } ?: "")
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (snapshot.day <= 0) "Cycle not started" else snapshot.phase.displayName,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                if (snapshot.day <= 0)
                    "Once you log the first day of your period, workouts and nutrition adapt to your cycle."
                else snapshot.phase.description,
                style = MaterialTheme.typography.bodyMedium
            )
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
private fun PhasePicksCard(phase: CyclePhase) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("${phase.displayName} phase picks", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Suggested moves and foods for the current phase.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text("Workouts", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            CycleCalculator.phaseWorkoutPicks(phase).forEach {
                Text("• $it", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text("Foods", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            CycleCalculator.phaseFoodPicks(phase).forEach {
                Text("• $it", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyJournalCard(
    journal: CycleDayEntity?,
    saving: Boolean,
    moodOptions: List<MoodOption>,
    onSave: (note: String, moodId: String?, weightKg: Double?) -> Unit
) {
    var note by rememberSaveable { mutableStateOf(journal?.note ?: "") }
    var moodId by rememberSaveable { mutableStateOf(journal?.moodId) }
    var weightText by rememberSaveable { mutableStateOf(journal?.weightKg?.toString() ?: "") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Daily journal", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Record your mood, weight, and a note for today.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("Mood", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                moodOptions.forEach { option ->
                    FilterChip(
                        selected = option.id == moodId,
                        onClick = {
                            moodId = if (option.id == moodId) null else option.id
                        },
                        label = { Text("${option.emoji} ${option.name}") }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = weightText,
                onValueChange = { input ->
                    val clean = input.filter { it.isDigit() || it == '.' }.take(5)
                    weightText = clean
                },
                label = { Text("Weight (kg) — optional") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    onSave(note.trim(), moodId, weightText.toDoubleOrNull())
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving
            ) {
                Text(if (saving) "Saving…" else "Save journal")
            }
        }
    }
}

@Composable
private fun CycleLengthSelector(selected: Int, onSelect: (Int) -> Unit) {
    val presets = listOf(21, 24, 28, 30, 35)
    var customText by remember(selected) { mutableStateOf(if (selected in presets) "" else selected.toString()) }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { length ->
                OutlinedButton(onClick = { onSelect(length) }, modifier = Modifier.weight(1f)) {
                    Text(if (length == selected) "$length ✓" else "$length")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = customText,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }.take(2)
                customText = digits
                digits.toIntOrNull()?.let { if (it in 21..45) onSelect(it) }
            },
            label = { Text("Custom length (21-45 days)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PeriodStatusBanner(snapshot: PhaseSnapshot, todayMillis: Long, onLogStarted: () -> Unit) {
    val confirmed = snapshot.confirmedBleedingDay
    val expected = snapshot.expectedBleedingDay
    val late = snapshot.lateByDays
    val state: BannerState = when {
        confirmed > 0 -> BannerState.InProgress
        expected > 0 -> BannerState.Expected
        late > 0 -> BannerState.Late
        else -> BannerState.Upcoming
    }
    val text = when (state) {
        BannerState.InProgress -> "Period in progress · Day $confirmed of ${snapshot.periodLengthDays}"
        BannerState.Expected ->
            "Expected period window · Day $expected of ${snapshot.periodLengthDays} · Log it when it starts"
        BannerState.Late -> {
            val on = snapshot.currentExpectedStartMillis?.let { formatDate(it) } ?: ""
            "Period is $late ${if (late == 1) "day" else "days"} late" + if (on.isNotEmpty()) " · Expected on $on" else ""
        }
        BannerState.Upcoming ->
            "Period not started yet · Next expected ${snapshot.nextPeriodMillis?.let { formatDate(it) } ?: "—"}"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                BannerState.InProgress -> MaterialTheme.colorScheme.errorContainer
                BannerState.Expected, BannerState.Late -> MaterialTheme.colorScheme.tertiaryContainer
                BannerState.Upcoming -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (state == BannerState.Expected || state == BannerState.Late) {
                Button(onClick = onLogStarted) { Text("Log as started") }
            }
        }
    }
}

private enum class BannerState { InProgress, Expected, Late, Upcoming }

@Composable
private fun CycleCalendar(
    lastPeriodStart: Long?,
    cycleLength: Int,
    periodLength: Int,
    entries: List<com.fitlife.ai.data.local.entity.CycleEntryEntity>,
    phaseInfo: PhaseSnapshot,
    suppressFertile: Boolean = false
) {
    var monthOffset by rememberSaveable { mutableStateOf(0) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Cycle calendar", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "solid red = logged period · light red = predicted · tinted by phase · ring = today",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { monthOffset-- }) { Icon(Icons.Filled.ChevronLeft, "Previous month") }
                Text(monthTitle(monthOffset), style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = { monthOffset++ }) { Icon(Icons.Filled.ChevronRight, "Next month") }
            }
            Spacer(Modifier.height(8.dp))

            val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(Modifier.fillMaxWidth()) {
                dayLabels.forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            val todayStart = startOfDayMillis(System.currentTimeMillis())
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, monthOffset)
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val firstDow = Calendar.getInstance().apply { set(year, month, 1, 0, 0, 0) }
                .get(Calendar.DAY_OF_WEEK)
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            val confirmedPeriods = entries.map { entry ->
                val start = startOfDayMillis(entry.startDate)
                start to start + entry.durationDays.coerceIn(1, 14) * DAY_MILLIS
            }
            val fertileStart = phaseInfo.fertileStartMillis
            val fertileEnd = phaseInfo.fertileEndMillis

            val cells = (0 until 42).map { index ->
                val day = index - (firstDow - 1) + 1
                val inMonth = day in 1..daysInMonth
                val millis = if (inMonth) {
                    Calendar.getInstance().apply {
                        set(year, month, day, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                } else null
                Triple(day, inMonth, millis)
            }

            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { (day, inMonth, millis) ->
                        val isConfirmed = millis != null && confirmedPeriods.any { (s, e) -> millis in s until e }
                        val isExpected = millis != null && !isConfirmed &&
                            lastPeriodStart != null && lastPeriodStart > 0 &&
                            CycleCalculator.bleedingDay(millis, lastPeriodStart, cycleLength, periodLength) > 0
                        DayCell(
                            day = day,
                            inMonth = inMonth,
                            isToday = millis != null && millis == todayStart,
                            isConfirmedPeriod = isConfirmed,
                            isExpectedPeriod = isExpected,
                            isFertile = !suppressFertile && millis != null && fertileStart != null &&
                                fertileEnd != null && millis in fertileStart..fertileEnd,
                            phase = if (inMonth && millis != null && lastPeriodStart != null && lastPeriodStart > 0)
                                CycleCalculator.phaseForDay(
                                    CycleCalculator.cycleDay(millis, lastPeriodStart, cycleLength)
                                )
                            else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(MaterialTheme.colorScheme.error) { "Logged" }
                LegendDot(MaterialTheme.colorScheme.error.copy(alpha = 0.35f)) { "Predicted" }
                if (!suppressFertile) {
                    LegendDot(MaterialTheme.colorScheme.tertiary) { "Fertile" }
                }
                LegendDot(MaterialTheme.colorScheme.primaryContainer) { "Phase tint" }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    inMonth: Boolean,
    isToday: Boolean,
    isConfirmedPeriod: Boolean,
    isExpectedPeriod: Boolean,
    isFertile: Boolean,
    phase: CyclePhase?,
    modifier: Modifier = Modifier
) {
    val bg = when {
        isConfirmedPeriod -> MaterialTheme.colorScheme.error
        isExpectedPeriod -> MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
        isFertile -> MaterialTheme.colorScheme.tertiary
        phase != null -> phaseColor(phase)
        else -> Color.Transparent
    }
    val fg = when {
        isConfirmedPeriod -> MaterialTheme.colorScheme.onError
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(bg)
            .then(
                if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (inMonth) day.toString() else "",
            style = MaterialTheme.typography.labelMedium,
            color = if (inMonth) fg else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            fontWeight = if (isConfirmedPeriod || isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: () -> String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(10.dp).clip(CircleShape).background(color)
        )
        Text(label(), style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CycleDetailsCard(
    lastPeriodStart: Long?,
    cycleLength: Int,
    birthControl: String?,
    onLastPeriodStartChange: (Long) -> Unit,
    onCycleLengthChange: (Int) -> Unit,
    onBirthControlChange: (String?) -> Unit,
    onShareReport: () -> Unit,
    cycleRemindersEnabled: Boolean,
    onCycleRemindersChange: (Boolean) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Cycle details", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Last period start", style = MaterialTheme.typography.labelLarge)
                    Text(
                        if (lastPeriodStart != null && lastPeriodStart > 0) formatFullDate(lastPeriodStart)
                        else "Not set",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = { showDatePicker = true }) { Text("Change") }
            }
            Spacer(Modifier.height(12.dp))
            Text("Cycle length", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            CycleLengthSelector(selected = cycleLength, onSelect = onCycleLengthChange)

            Spacer(Modifier.height(12.dp))
            Text("Birth control", style = MaterialTheme.typography.labelLarge)
            Text(
                "Ovulation and fertile-window predictions are hidden while a method is selected.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf<Pair<String?, String>>(
                    null to "None",
                    "pill" to "Pill",
                    "iud" to "IUD",
                    "implant" to "Implant",
                    "shot" to "Shot",
                    "patch" to "Patch",
                    "ring" to "Ring"
                ).forEach { (method, label) ->
                    FilterChip(
                        selected = (birthControl?.isNotBlank() == true) == (method != null) &&
                            birthControl.orEmpty() == (method ?: ""),
                        onClick = { onBirthControlChange(method) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Period & ovulation reminders", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Daily 8 AM notification near your predicted period and fertile window",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = cycleRemindersEnabled,
                    onCheckedChange = onCycleRemindersChange
                )
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onShareReport,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export cycle report")
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = lastPeriodStart?.takeIf { it > 0L }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onLastPeriodStartChange(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun phaseColor(phase: CyclePhase): Color = when (phase) {
    CyclePhase.MENSTRUAL -> MaterialTheme.colorScheme.errorContainer
    CyclePhase.FOLLICULAR -> MaterialTheme.colorScheme.primaryContainer
    CyclePhase.OVULATORY -> MaterialTheme.colorScheme.tertiaryContainer
    CyclePhase.LUTEAL -> MaterialTheme.colorScheme.secondaryContainer
}

private fun monthTitle(offset: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, offset)
    return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
}

private fun startOfDayMillis(millis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun formatFullDate(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))

private const val DAY_MILLIS = 86_400_000L

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodLogCard(
    offsetDays: Int,
    onOffsetChange: (Int) -> Unit,
    defaultDuration: Int,
    notes: String,
    onNotesChange: (String) -> Unit,
    symptoms: List<String>,
    onSymptomsChange: (List<String>) -> Unit,
    saving: Boolean,
    onLog: (durationDays: Int) -> Unit
) {
    var durationDays by rememberSaveable { mutableStateOf(defaultDuration.coerceIn(1, 14)) }
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
            Text("How many days does it last?", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 4, 5, 6, 7).forEach { days ->
                    FilterChip(
                        selected = durationDays == days,
                        onClick = { durationDays = days },
                        label = { Text("$days days") }
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
                onClick = { onLog(durationDays) },
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
                            "${entry.durationDays} days",
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

private const val PREFS = "fitlife_prefs"
private const val KEY_CYCLE_REMINDERS = "cycle_reminders_enabled"

private fun setCycleReminders(context: Context, enabled: Boolean) {
    if (enabled) {
        CycleReminderScheduler.schedule(context)
    } else {
        CycleReminderScheduler.cancel(context)
    }
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_CYCLE_REMINDERS, enabled)
        .apply()
}

private fun decodeSymptomsCount(json: String): Int {
    val trimmed = json.trim()
    if (trimmed.length < 2) return 0
    val inner = trimmed.substring(1, trimmed.length - 1)
    if (inner.isBlank()) return 0
    return inner.split(",").count { it.trim().isNotEmpty() }
}
