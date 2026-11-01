package com.example.base32box.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.base32box.core.base32.Base32Casing
import com.example.base32box.core.base32.Base32Format
import com.example.base32box.data.preferences.AppTheme
import com.example.base32box.data.preferences.OperationMode
import com.example.base32box.data.preferences.PreferencesRepository
import com.example.base32box.data.preferences.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PreferencesRepository(application.applicationContext)

    val preferences: StateFlow<UserPreferences> = repository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            repository.updateTheme(theme)
        }
    }

    fun setDefaultOperation(operation: OperationMode) {
        viewModelScope.launch {
            repository.updateDefaultOperation(operation)
        }
    }

    fun setDefaultFormat(format: Base32Format) {
        viewModelScope.launch {
            repository.updateDefaultFormat(format)
        }
    }

    fun setPaddingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePaddingEnabled(enabled)
        }
    }

    fun setCasing(casing: Base32Casing) {
        viewModelScope.launch {
            repository.updateCasing(casing)
        }
    }

    fun setIgnoreWhitespace(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateIgnoreWhitespace(enabled)
        }
    }

    fun setClearOnExit(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateClearOnExit(enabled)
        }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateHapticFeedback(enabled)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
        }
    }
}
