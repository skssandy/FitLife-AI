package com.fitlife.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.ProgramDay
import com.fitlife.ai.data.ProgramExercise
import com.fitlife.ai.data.ProgramSeedData
import com.fitlife.ai.data.WorkoutProgramTemplate
import com.fitlife.ai.data.local.entity.WorkoutProgramEntity
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.WorkoutProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class ProgramsUiState(
    val programs: List<WorkoutProgramEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProgramsViewModel @Inject constructor(
    private val programRepository: WorkoutProgramRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgramsUiState())
    val uiState: StateFlow<ProgramsUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        loadPrograms()
    }

    private fun loadPrograms() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val userId = authRepository.getCurrentUserId()
                programRepository.getPrograms(userId).collect { programs ->
                    _uiState.value = _uiState.value.copy(programs = programs, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun presets(): List<WorkoutProgramTemplate> = ProgramSeedData.templates

    fun addPreset(template: WorkoutProgramTemplate) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val entity = WorkoutProgramEntity(
                    userId = userId,
                    name = template.name,
                    description = template.description,
                    goal = template.goal,
                    daysJson = json.encodeToString(template.days)
                )
                programRepository.addProgram(entity)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteProgram(id: Long) {
        viewModelScope.launch {
            programRepository.deleteProgram(id)
        }
    }

    fun parseDays(daysJson: String): List<ProgramDay> = try {
        json.decodeFromString<List<ProgramDay>>(daysJson)
    } catch (_: Exception) {
        emptyList()
    }

    fun exerciseLabel(e: ProgramExercise): String = "${e.name} · ${e.sets} × ${e.reps}"
}
