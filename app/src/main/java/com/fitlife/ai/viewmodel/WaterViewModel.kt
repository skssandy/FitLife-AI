package com.fitlife.ai.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.ReminderSettings
import com.fitlife.ai.data.repository.ReminderSettingsRepository
import com.fitlife.ai.data.repository.WaterRepository
import com.fitlife.ai.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class WaterUiState(
    val todayMl: Int = 0,
    val logs: List<com.fitlife.ai.data.local.entity.WaterLogEntity> = emptyList(),
    val weeklyData: List<Pair<String, Int>> = emptyList(),
    val targetMl: Int = 2500,
    val reminders: ReminderSettings = ReminderSettings(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class WaterViewModel @Inject constructor(
    private val waterRepository: WaterRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: ReminderSettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(WaterUiState())
    val uiState: StateFlow<WaterUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val user = authRepository.getUserOnce(userId)
                val target = user?.hydrationTargetMl
                    ?: user?.weightKg?.let { (it * 35).toInt() }
                    ?: 2500
                val todayStart = startOfDay()
                val todayEnd = todayStart + 86400000L
                val weekStart = startOfDay(6)

                val reminders = settingsRepository.settings.first()
                if (reminders.enabled) {
                    ReminderScheduler.schedule(context, reminders.intervalHours)
                }

                _uiState.value = _uiState.value.copy(targetMl = target, reminders = reminders)

                waterRepository.getLogs(userId).collect { logs ->
                    val todayMl = logs.filter { it.date in todayStart until todayEnd }.sumOf { it.amountMl }
                    val weekly = buildWeekly(logs, weekStart)
                    _uiState.value = _uiState.value.copy(
                        todayMl = todayMl,
                        logs = logs.filter { it.date in todayStart until todayEnd },
                        weeklyData = weekly,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEnabled(enabled)
            if (enabled) {
                val interval = settingsRepository.settings.first().intervalHours
                ReminderScheduler.schedule(context, interval)
            } else {
                ReminderScheduler.cancel(context)
            }
            _uiState.value = _uiState.value.copy(reminders = settingsRepository.settings.first())
        }
    }

    fun setReminderInterval(intervalHours: Int) {
        viewModelScope.launch {
            settingsRepository.setIntervalHours(intervalHours)
            val settings = settingsRepository.settings.first()
            if (settings.enabled) {
                ReminderScheduler.schedule(context, intervalHours)
            }
            _uiState.value = _uiState.value.copy(reminders = settings)
        }
    }

    fun setQuietHours(start: Int, end: Int) {
        viewModelScope.launch {
            settingsRepository.setQuietHours(start, end)
            _uiState.value = _uiState.value.copy(reminders = settingsRepository.settings.first())
        }
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                waterRepository.addLog(userId, amountMl)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            waterRepository.deleteLog(id)
        }
    }

    private fun buildWeekly(logs: List<com.fitlife.ai.data.local.entity.WaterLogEntity>, weekStart: Long): List<Pair<String, Int>> {
        val dayFormat = java.text.SimpleDateFormat("E", java.util.Locale.getDefault())
        return (0..6).map { offset ->
            val dayStart = weekStart + offset * 86400000L
            val dayEnd = dayStart + 86400000L
            val label = dayFormat.format(java.util.Date(dayStart))
            val total = logs.filter { it.date in dayStart until dayEnd }.sumOf { it.amountMl }
            label to total
        }
    }

    private fun startOfDay(daysAgo: Int = 0): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
