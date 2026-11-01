package com.example.base32box

import com.example.base32box.core.base32.Base32Casing
import com.example.base32box.core.base32.Base32Format
import com.example.base32box.data.preferences.AppTheme
import com.example.base32box.data.preferences.OperationMode
import com.example.base32box.data.preferences.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPreferencesTest {

    @Test
    fun testDefaultUserPreferences() {
        val prefs = UserPreferences()
        assertEquals(AppTheme.SYSTEM, prefs.theme)
        assertEquals(OperationMode.ENCODE, prefs.defaultOperation)
        assertEquals(Base32Format.RFC4648, prefs.defaultFormat)
        assertTrue(prefs.paddingEnabled)
        assertEquals(Base32Casing.UPPERCASE, prefs.casing)
        assertTrue(prefs.ignoreWhitespace)
        assertFalse(prefs.clearOnExit)
        assertTrue(prefs.hapticFeedbackEnabled)
    }

    @Test
    fun testCustomUserPreferences() {
        val prefs = UserPreferences(
            theme = AppTheme.DARK,
            defaultOperation = OperationMode.DECODE,
            defaultFormat = Base32Format.BASE32_HEX,
            paddingEnabled = false,
            casing = Base32Casing.LOWERCASE,
            ignoreWhitespace = false,
            clearOnExit = true,
            hapticFeedbackEnabled = false
        )

        assertEquals(AppTheme.DARK, prefs.theme)
        assertEquals(OperationMode.DECODE, prefs.defaultOperation)
        assertEquals(Base32Format.BASE32_HEX, prefs.defaultFormat)
        assertFalse(prefs.paddingEnabled)
        assertEquals(Base32Casing.LOWERCASE, prefs.casing)
        assertFalse(prefs.ignoreWhitespace)
        assertTrue(prefs.clearOnExit)
        assertFalse(prefs.hapticFeedbackEnabled)
    }
}
