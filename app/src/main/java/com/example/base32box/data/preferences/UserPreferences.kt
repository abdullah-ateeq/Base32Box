package com.example.base32box.data.preferences

import com.example.base32box.core.base32.Base32Casing
import com.example.base32box.core.base32.Base32Format

/**
 * Data model for persisted user preferences.
 */
data class UserPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val defaultOperation: OperationMode = OperationMode.ENCODE,
    val defaultFormat: Base32Format = Base32Format.RFC4648,
    val paddingEnabled: Boolean = true,
    val casing: Base32Casing = Base32Casing.UPPERCASE,
    val ignoreWhitespace: Boolean = true,
    val clearOnExit: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true
)
