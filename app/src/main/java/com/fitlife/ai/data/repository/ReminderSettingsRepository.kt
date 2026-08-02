package com.fitlife.ai.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reminderDataStore by preferencesDataStore(name = "reminder_settings")

data class ReminderSettings(
    val enabled: Boolean = false,
    val intervalHours: Int = 2,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7
)

@Singleton
class ReminderSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val settings: Flow<ReminderSettings> = context.reminderDataStore.data.map { prefs ->
        ReminderSettings(
            enabled = prefs[KEY_ENABLED] ?: false,
            intervalHours = prefs[KEY_INTERVAL] ?: 2,
            quietStartHour = prefs[KEY_QUIET_START] ?: 22,
            quietEndHour = prefs[KEY_QUIET_END] ?: 7
        )
    }

    suspend fun setEnabled(value: Boolean) {
        context.reminderDataStore.edit { it[KEY_ENABLED] = value }
    }

    suspend fun setIntervalHours(value: Int) {
        context.reminderDataStore.edit { it[KEY_INTERVAL] = value }
    }

    suspend fun setQuietHours(start: Int, end: Int) {
        context.reminderDataStore.edit {
            it[KEY_QUIET_START] = start
            it[KEY_QUIET_END] = end
        }
    }

    private companion object {
        val KEY_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("enabled")
        val KEY_INTERVAL: Preferences.Key<Int> = intPreferencesKey("interval_hours")
        val KEY_QUIET_START: Preferences.Key<Int> = intPreferencesKey("quiet_start_hour")
        val KEY_QUIET_END: Preferences.Key<Int> = intPreferencesKey("quiet_end_hour")
    }
}
