package com.fitlife.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.local.entity.CycleDayEntity
import com.fitlife.ai.data.local.entity.CycleEntryEntity
import com.fitlife.ai.data.local.entity.SymptomLogEntity
import com.fitlife.ai.data.local.entity.UserEntity
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.CycleRepository
import com.fitlife.ai.util.CycleCalculator
import com.fitlife.ai.util.CyclePhase
import com.fitlife.ai.util.SupportMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhaseSnapshot(
    val phase: CyclePhase,
    val day: Int,
    val cycleLength: Int,
    val periodLengthDays: Int,
    val lastPeriodStartMillis: Long,
    val confirmedBleedingDay: Int,
    val expectedBleedingDay: Int,
    val lateByDays: Int,
    val nextPeriodMillis: Long?,
    val currentExpectedStartMillis: Long?,
    val fertileStartMillis: Long?,
    val fertileEndMillis: Long?
)

data class CycleUiState(
    val user: UserEntity? = null,
    val entries: List<CycleEntryEntity> = emptyList(),
    val symptomLogs: List<SymptomLogEntity> = emptyList(),
    val cycleDays: List<CycleDayEntity> = emptyList(),
    val todaySymptoms: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CycleViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cycleRepository: CycleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CycleUiState())
    val uiState: StateFlow<CycleUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                launch {
                    authRepository.observeUser(userId).collect { user ->
                        _uiState.value = _uiState.value.copy(user = user, isLoading = false)
                    }
                }
                launch {
                    cycleRepository.getEntries(userId).collect { entries ->
                        _uiState.value = _uiState.value.copy(entries = entries)
                    }
                }
                launch {
                    cycleRepository.getSymptomLogs(userId).collect { logs ->
                        val today = startOfToday()
                        val todayLog = logs.firstOrNull { isSameDay(it.date, today) }
                        _uiState.value = _uiState.value.copy(
                            symptomLogs = logs,
                            todaySymptoms = todayLog?.let { decodeSymptoms(it.symptomsJson) } ?: emptyList()
                        )
                    }
                }
                launch {
                    cycleRepository.getCycleDays(userId).collect { days ->
                        _uiState.value = _uiState.value.copy(cycleDays = days)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun phaseInfo(): PhaseSnapshot? {
        val user = _uiState.value.user ?: return null
        val lastPeriod = user.lastPeriodStart ?: return null
        if (lastPeriod <= 0L) return null
        val cycleLength = (user.cycleLength ?: 28).coerceAtLeast(21)
        val today = System.currentTimeMillis()
        val periodLength = lastDurationDays()
        val day = CycleCalculator.cycleDay(today, lastPeriod, cycleLength)
        val confirmedBleedingDay = confirmedBleedingDay(today, periodLength)
        val expectedBleedingDay = CycleCalculator.bleedingDay(today, lastPeriod, cycleLength, periodLength)
        val phase = if (day > 0) CycleCalculator.phaseForDay(day) else CyclePhase.MENSTRUAL
        val nextPeriod = CycleCalculator.nextPeriodStartMillis(lastPeriod, cycleLength, today)
        val currentExpected = CycleCalculator.currentExpectedStart(today, lastPeriod, cycleLength)
        val fertile = CycleCalculator.fertileWindow(lastPeriod, cycleLength)
        val lateBy = if (confirmedBleedingDay == 0 && expectedBleedingDay == 0) {
            CycleCalculator.daysLate(today, lastPeriod, cycleLength, periodLength)
        } else 0
        return PhaseSnapshot(
            phase = phase,
            day = day,
            cycleLength = cycleLength,
            periodLengthDays = periodLength,
            lastPeriodStartMillis = lastPeriod,
            confirmedBleedingDay = confirmedBleedingDay,
            expectedBleedingDay = expectedBleedingDay,
            lateByDays = lateBy,
            nextPeriodMillis = nextPeriod,
            currentExpectedStartMillis = currentExpected,
            fertileStartMillis = fertile.first,
            fertileEndMillis = fertile.second
        )
    }

    /** Duration of the most recent logged period, defaulting to 5 days. */
    fun lastDurationDays(): Int =
        _uiState.value.entries.maxByOrNull { it.startDate }
            ?.durationDays?.coerceIn(1, 14) ?: 5

    private fun confirmedBleedingDay(todayMillis: Long, periodLength: Int): Int {
        val today = startOfDay(todayMillis)
        val entry = _uiState.value.entries.firstOrNull { e ->
            val start = startOfDay(e.startDate)
            today >= start && today < start + e.durationDays.coerceIn(1, 14) * DAY_MILLIS
        } ?: return 0
        return (((today - startOfDay(entry.startDate)) / DAY_MILLIS).toInt() + 1).coerceIn(1, periodLength)
    }

    fun supportMode(): SupportMode = SupportMode.from(_uiState.value.user?.supportMode)

    fun logPeriod(startDateMillis: Long, durationDays: Int, symptoms: List<String>, notes: String) {
        viewModelScope.launch {
            doLogPeriod(startDateMillis, durationDays, symptoms, notes)
        }
    }

    /** Logs the currently expected (or overdue) period as started without prompting for details. */
    fun markPeriodStarted() {
        viewModelScope.launch {
            val user = _uiState.value.user ?: return@launch
            val lastPeriod = user.lastPeriodStart ?: return@launch
            if (lastPeriod <= 0L) return@launch
            val cycleLength = (user.cycleLength ?: 28).coerceAtLeast(21)
            val expectedStart = CycleCalculator.currentExpectedStart(
                System.currentTimeMillis(), lastPeriod, cycleLength
            )
            doLogPeriod(expectedStart, lastDurationDays(), emptyList(), "Marked as started")
        }
    }

    private suspend fun doLogPeriod(startDateMillis: Long, durationDays: Int, symptoms: List<String>, notes: String) {
        try {
            val userId = authRepository.getCurrentUserId()
            val user = _uiState.value.user ?: return
            val sanitized = startOfDay(startDateMillis)
            val entry = CycleEntryEntity(
                userId = userId,
                startDate = sanitized,
                durationDays = durationDays.coerceIn(1, 14),
                flowLevel = "",
                symptomsJson = encodeSymptoms(symptoms),
                notes = notes
            )
            _uiState.value = _uiState.value.copy(saving = true)
            cycleRepository.upsertEntry(entry)
            authRepository.saveProfile(user.copy(lastPeriodStart = sanitized))
            _uiState.value = _uiState.value.copy(saving = false)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(saving = false, error = e.message)
        }
    }

    fun logSymptoms(symptoms: List<String>) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val today = startOfToday()
                val existing = cycleRepository.getSymptomsForDay(userId, today)
                val log = (existing ?: SymptomLogEntity(userId = userId, date = today)).copy(
                    symptomsJson = encodeSymptoms(symptoms)
                )
                _uiState.value = _uiState.value.copy(saving = true, todaySymptoms = symptoms)
                cycleRepository.upsertSymptomLog(log)
                _uiState.value = _uiState.value.copy(saving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, error = e.message)
            }
        }
    }

    fun toggleSymptom(symptomId: String) {
        val current = _uiState.value.todaySymptoms
        val updated = if (symptomId in current) current - symptomId else current + symptomId
        _uiState.value = _uiState.value.copy(todaySymptoms = updated)
        logSymptoms(updated)
    }

    fun setSupportMode(mode: SupportMode) {
        viewModelScope.launch {
            try {
                val user = _uiState.value.user ?: return@launch
                authRepository.saveProfile(user.copy(supportMode = mode.name))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /** Today's journal row (note/mood/weight), if any. */
    fun todayJournal(): CycleDayEntity? {
        val today = startOfToday()
        return _uiState.value.cycleDays.firstOrNull { isSameDay(it.date, today) }
    }

    /** Saves today's journal. Passing null keeps the existing value for that field. */
    fun saveJournal(note: String?, moodId: String?, weightKg: Double?) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val today = startOfToday()
                val existing = cycleRepository.getCycleDayForDay(userId, today)
                val merged = (existing ?: CycleDayEntity(userId = userId, date = today)).copy(
                    note = note ?: existing?.note ?: "",
                    moodId = moodId ?: existing?.moodId,
                    weightKg = weightKg ?: existing?.weightKg
                )
                _uiState.value = _uiState.value.copy(saving = true)
                cycleRepository.upsertCycleDay(merged)
                _uiState.value = _uiState.value.copy(saving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, error = e.message)
            }
        }
    }

    /** True when the user tracks a birth control method (fertility/ovulation displays are suppressed). */
    fun isOnBirthControl(): Boolean = !_uiState.value.user?.birthControl.isNullOrBlank()

    fun setBirthControl(method: String?) {
        viewModelScope.launch {
            try {
                val user = _uiState.value.user ?: return@launch
                authRepository.saveProfile(user.copy(birthControl = method?.takeIf { it.isNotBlank() }))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /** Human-readable text report of the user's cycle data for sharing/export. */
    fun buildCycleReport(): String {
        val user = _uiState.value.user ?: return "No profile data."
        val sb = StringBuilder()
        sb.appendLine("FitLife AI — Cycle Report")
        sb.appendLine("Generated ${java.text.SimpleDateFormat("MMM d, yyyy h:mm a", java.util.Locale.getDefault()).format(java.util.Date())}")
        sb.appendLine()
        sb.appendLine("Profile")
        user.displayName?.let { sb.appendLine("Name: $it") }
        if (user.lastPeriodStart != null && user.lastPeriodStart > 0L) {
            sb.appendLine("Last period start: ${formatReportDate(user.lastPeriodStart)}")
        }
        sb.appendLine("Cycle length: ${user.cycleLength ?: 28} days")
        user.supportMode?.let { sb.appendLine("Support mode: $it") }
        if (isOnBirthControl()) sb.appendLine("Birth control: ${user.birthControl}")
        sb.appendLine()

        val entries = _uiState.value.entries
        if (entries.isNotEmpty()) {
            sb.appendLine("Period history")
            entries.forEach { e ->
                sb.appendLine("- ${formatReportDate(e.startDate)} · ${e.durationDays} days" +
                    (if (e.notes.isNotBlank()) " · note: ${e.notes}" else ""))
            }
            sb.appendLine()
        }

        val logs = _uiState.value.symptomLogs
        if (logs.isNotEmpty()) {
            sb.appendLine("Symptom log")
            logs.take(20).forEach { l ->
                val names = decodeSymptoms(l.symptomsJson)
                if (names.isNotEmpty()) {
                    sb.appendLine("- ${formatReportDate(l.date)}: ${names.joinToString(", ")}")
                }
            }
            sb.appendLine()
        }

        val days = _uiState.value.cycleDays
        if (days.isNotEmpty()) {
            sb.appendLine("Daily notes & weight")
            days.take(30).forEach { d ->
                val parts = mutableListOf<String>()
                d.moodId?.let { parts.add("mood: ${moodName(it)}") }
                d.weightKg?.let { parts.add("weight: $it kg") }
                if (d.note.isNotBlank()) parts.add("note: ${d.note}")
                if (parts.isNotEmpty()) {
                    sb.appendLine("- ${formatReportDate(d.date)}: ${parts.joinToString(" · ")}")
                }
            }
        }
        return sb.toString()
    }

    fun moodName(moodId: String?): String =
        moodId?.let { CycleViewModel.moodOptions.firstOrNull { m -> m.id == it }?.name } ?: "—"

    private fun formatReportDate(millis: Long): String =
        java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(millis))

    fun setCycleLength(days: Int) {
        viewModelScope.launch {
            try {
                val user = _uiState.value.user ?: return@launch
                authRepository.saveProfile(user.copy(cycleLength = days))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun setLastPeriodStart(dateMillis: Long) {
        viewModelScope.launch {
            try {
                val user = _uiState.value.user ?: return@launch
                val day = startOfDay(dateMillis)
                authRepository.saveProfile(user.copy(lastPeriodStart = day))
                val alreadyLogged = _uiState.value.entries.any { startOfDay(it.startDate) == day }
                if (!alreadyLogged) {
                    cycleRepository.upsertEntry(
                        CycleEntryEntity(
                            userId = user.id,
                            startDate = day,
                            durationDays = lastDurationDays()
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun encodeSymptoms(symptoms: List<String>): String =
        symptoms.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"$it\"" }

    private fun decodeSymptoms(json: String): List<String> {
        val trimmed = json.trim()
        if (trimmed.length < 2) return emptyList()
        val inner = trimmed.substring(1, trimmed.length - 1)
        if (inner.isBlank()) return emptyList()
        return inner.split(",").map { it.trim().trim('"') }.filter { it.isNotEmpty() }
    }

    private fun isSameDay(a: Long, b: Long): Boolean = startOfDay(a) == startOfDay(b)

    private fun startOfDay(millis: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun startOfToday(): Long = startOfDay(System.currentTimeMillis())

    companion object {
        const val DAY_MILLIS = 86_400_000L

        val moodOptions = listOf(
            MoodOption("happy", "Happy", "😊"),
            MoodOption("energetic", "Energetic", "⚡"),
            MoodOption("calm", "Calm", "😌"),
            MoodOption("tired", "Tired", "😴"),
            MoodOption("irritable", "Irritable", "😤"),
            MoodOption("anxious", "Anxious", "😰"),
            MoodOption("sad", "Sad", "😢"),
            MoodOption("crampy", "Crampy", "🥴"),
            MoodOption("bloated", "Bloated", "🎈"),
            MoodOption("stressed", "Stressed", "😫")
        )
    }
}

data class MoodOption(
    val id: String,
    val name: String,
    val emoji: String
)
