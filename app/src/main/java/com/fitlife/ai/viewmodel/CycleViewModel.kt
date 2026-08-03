package com.fitlife.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val nextPeriodMillis: Long?,
    val fertileStartMillis: Long?,
    val fertileEndMillis: Long?
)

data class CycleUiState(
    val user: UserEntity? = null,
    val entries: List<CycleEntryEntity> = emptyList(),
    val symptomLogs: List<SymptomLogEntity> = emptyList(),
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
        val day = CycleCalculator.cycleDay(today, lastPeriod, cycleLength)
        val phase = CycleCalculator.phaseForDay(day)
        val nextPeriod = CycleCalculator.nextPeriodStartMillis(lastPeriod, cycleLength, today)
        val fertile = CycleCalculator.fertileWindow(lastPeriod, cycleLength)
        return PhaseSnapshot(
            phase = phase,
            day = day,
            cycleLength = cycleLength,
            nextPeriodMillis = nextPeriod,
            fertileStartMillis = fertile.first,
            fertileEndMillis = fertile.second
        )
    }

    fun supportMode(): SupportMode = SupportMode.from(_uiState.value.user?.supportMode)

    fun logPeriod(startDateMillis: Long, flowLevel: String, symptoms: List<String>, notes: String) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val user = _uiState.value.user ?: return@launch
                val sanitized = startOfDay(startDateMillis)
                val entry = CycleEntryEntity(
                    userId = userId,
                    startDate = sanitized,
                    flowLevel = flowLevel,
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
}
