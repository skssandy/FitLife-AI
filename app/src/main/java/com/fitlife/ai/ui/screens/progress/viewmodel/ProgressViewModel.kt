package com.fitlife.ai.ui.screens.progress.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.local.dao.ProgressDao
import com.fitlife.ai.data.local.entity.ProgressEntryEntity
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.health.HealthConnectManager
import com.fitlife.ai.health.HealthData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class ProgressUiState(
    val entries: List<ProgressEntryEntity> = emptyList(),
    val latestWeight: Double? = null,
    val latestBodyFat: Double? = null,
    val healthData: HealthData = HealthData(),
    val selectedTab: Int = 0
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val progressDao: ProgressDao,
    private val authRepository: AuthRepository,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            combine(
                progressDao.getEntries(userId),
                progressDao.getLatestEntry(userId)
            ) { entries, latest ->
                ProgressUiState(
                    entries = entries,
                    latestWeight = latest?.weightKg,
                    latestBodyFat = latest?.bodyFatPercent
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
        viewModelScope.launch {
            if (healthConnectManager.isAvailable && healthConnectManager.hasPermissions()) {
                _uiState.value = _uiState.value.copy(healthData = healthConnectManager.getTodayData())
            }
        }
    }

    fun setTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun logWeight(weight: Double) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            progressDao.upsert(
                ProgressEntryEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    date = LocalDate.now().toString(),
                    weightKg = weight
                )
            )
        }
    }
}
