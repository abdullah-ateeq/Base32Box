package com.example.base32box.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.base32box.core.base32.Base32Casing
import com.example.base32box.core.base32.Base32Format
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "base32box_preferences")

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val DEFAULT_OPERATION = stringPreferencesKey("default_operation")
        val DEFAULT_FORMAT = stringPreferencesKey("default_format")
        val PADDING_ENABLED = booleanPreferencesKey("padding_enabled")
        val CASING = stringPreferencesKey("casing")
        val IGNORE_WHITESPACE = booleanPreferencesKey("ignore_whitespace")
        val CLEAR_ON_EXIT = booleanPreferencesKey("clear_on_exit")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME] ?: AppTheme.SYSTEM.name
            val theme = try {
                AppTheme.valueOf(themeName)
            } catch (_: Exception) {
                AppTheme.SYSTEM
            }

            val operationName = preferences[PreferencesKeys.DEFAULT_OPERATION] ?: OperationMode.ENCODE.name
            val defaultOperation = try {
                OperationMode.valueOf(operationName)
            } catch (_: Exception) {
                OperationMode.ENCODE
            }

            val formatName = preferences[PreferencesKeys.DEFAULT_FORMAT] ?: Base32Format.RFC4648.name
            val defaultFormat = try {
                Base32Format.valueOf(formatName)
            } catch (_: Exception) {
                Base32Format.RFC4648
            }

            val casingName = preferences[PreferencesKeys.CASING] ?: Base32Casing.UPPERCASE.name
            val casing = try {
                Base32Casing.valueOf(casingName)
            } catch (_: Exception) {
                Base32Casing.UPPERCASE
            }

            val paddingEnabled = preferences[PreferencesKeys.PADDING_ENABLED] ?: true
            val ignoreWhitespace = preferences[PreferencesKeys.IGNORE_WHITESPACE] ?: true
            val clearOnExit = preferences[PreferencesKeys.CLEAR_ON_EXIT] ?: false
            val hapticFeedbackEnabled = preferences[PreferencesKeys.HAPTIC_FEEDBACK] ?: true

            UserPreferences(
                theme = theme,
                defaultOperation = defaultOperation,
                defaultFormat = defaultFormat,
                paddingEnabled = paddingEnabled,
                casing = casing,
                ignoreWhitespace = ignoreWhitespace,
                clearOnExit = clearOnExit,
                hapticFeedbackEnabled = hapticFeedbackEnabled
            )
        }

    suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun updateDefaultOperation(operation: OperationMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_OPERATION] = operation.name
        }
    }

    suspend fun updateDefaultFormat(format: Base32Format) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_FORMAT] = format.name
        }
    }

    suspend fun updatePaddingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PADDING_ENABLED] = enabled
        }
    }

    suspend fun updateCasing(casing: Base32Casing) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CASING] = casing.name
        }
    }

    suspend fun updateIgnoreWhitespace(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IGNORE_WHITESPACE] = enabled
        }
    }

    suspend fun updateClearOnExit(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLEAR_ON_EXIT] = enabled
        }
    }

    suspend fun updateHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_FEEDBACK] = enabled
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
