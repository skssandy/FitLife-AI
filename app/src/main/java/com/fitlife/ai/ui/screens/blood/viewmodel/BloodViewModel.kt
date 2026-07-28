package com.fitlife.ai.ui.screens.blood.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.local.dao.BloodAnalysisDao
import com.fitlife.ai.data.local.entity.BloodAnalysisEntity
import com.fitlife.ai.data.local.entity.BloodMarkerEntity
import com.fitlife.ai.data.remote.api.GeminiService
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class BloodUiState(
    val analyses: List<BloodAnalysisEntity> = emptyList(),
    val selectedAnalysis: BloodAnalysisEntity? = null,
    val markers: List<BloodMarkerEntity> = emptyList(),
    val aiAnalysis: String? = null,
    val isAnalyzing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BloodViewModel @Inject constructor(
    private val bloodDao: BloodAnalysisDao,
    private val authRepository: AuthRepository,
    private val geminiService: GeminiService,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BloodUiState())
    val uiState: StateFlow<BloodUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            bloodDao.getAnalyses(userId).collect { analyses ->
                _uiState.value = _uiState.value.copy(analyses = analyses)
            }
        }
    }

    fun selectAnalysis(analysisId: String) {
        viewModelScope.launch {
            bloodDao.getAnalysis(analysisId).collect { analysis ->
                _uiState.value = _uiState.value.copy(selectedAnalysis = analysis)
            }
            bloodDao.getMarkers(analysisId).collect { markers ->
                _uiState.value = _uiState.value.copy(markers = markers)
            }
        }
    }

    fun analyzeReport(fileName: String, reportText: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)
            val analysisId = UUID.randomUUID().toString()
            val entity = BloodAnalysisEntity(
                id = analysisId,
                userId = userId,
                fileName = fileName,
                status = "analyzing"
            )
            bloodDao.upsert(entity)

            geminiService.analyzeBloodReport(
                profileRepository.getAnyProfile().first(),
                reportText
            ).onSuccess { analysis ->
                bloodDao.upsert(entity.copy(
                    overallSummary = analysis,
                    status = "completed"
                ))
                _uiState.value = _uiState.value.copy(isAnalyzing = false)
            }.onFailure { e ->
                bloodDao.upsert(entity.copy(status = "failed"))
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    error = e.message
                )
            }
        }
    }
}
