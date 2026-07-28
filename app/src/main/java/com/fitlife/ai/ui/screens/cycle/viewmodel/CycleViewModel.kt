package com.fitlife.ai.ui.screens.cycle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.local.dao.CycleDao
import com.fitlife.ai.data.local.entity.CycleEntryEntity
import com.fitlife.ai.data.remote.api.GeminiService
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class CycleUiState(
    val currentDay: Int = 14,
    val currentPhase: String = "Ovulation",
    val selectedSymptoms: Set<String> = emptySet(),
    val entries: List<CycleEntryEntity> = emptyList(),
    val aiAdvice: String? = null,
    val isLoadingAi: Boolean = false
)

@HiltViewModel
class CycleViewModel @Inject constructor(
    private val cycleDao: CycleDao,
    private val authRepository: AuthRepository,
    private val geminiService: GeminiService,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CycleUiState())
    val uiState: StateFlow<CycleUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            cycleDao.getEntries(userId).collect { entries ->
                _uiState.value = _uiState.value.copy(entries = entries)
                if (entries.isNotEmpty()) {
                    val latest = entries.first()
                    _uiState.value = _uiState.value.copy(
                        currentDay = latest.dayOfCycle,
                        currentPhase = latest.phase
                    )
                }
            }
        }
    }

    fun toggleSymptom(symptom: String) {
        val current = _uiState.value.selectedSymptoms.toMutableSet()
        if (symptom in current) current.remove(symptom) else current.add(symptom)
        _uiState.value = _uiState.value.copy(selectedSymptoms = current)
    }

    fun setDay(day: Int) {
        val phase = calculatePhase(day)
        _uiState.value = _uiState.value.copy(currentDay = day, currentPhase = phase)
    }

    fun logEntry() {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            cycleDao.upsert(
                CycleEntryEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    date = LocalDate.now().toString(),
                    dayOfCycle = _uiState.value.currentDay,
                    phase = _uiState.value.currentPhase,
                    symptoms = kotlinx.serialization.json.Json.encodeToString(
                        kotlinx.serialization.serializer(),
                        _uiState.value.selectedSymptoms.toList()
                    )
                )
            )
        }
    }

    fun getAiAdvice() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAi = true)
            geminiService.getCycleAdvice(
                profileRepository.getAnyProfile().first(),
                _uiState.value.currentDay,
                _uiState.value.currentPhase,
                _uiState.value.selectedSymptoms.toList()
            ).onSuccess { advice ->
                _uiState.value = _uiState.value.copy(aiAdvice = advice, isLoadingAi = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingAi = false)
            }
        }
    }

    private fun calculatePhase(day: Int): String = when {
        day <= 5 -> "Menstrual"
        day <= 13 -> "Follicular"
        day <= 16 -> "Ovulation"
        day <= 28 -> "Luteal"
        else -> "Menstrual"
    }
}
