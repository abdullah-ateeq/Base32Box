package com.example.base32box.core.base32

/**
 * Configuration options for Base32 encoding and decoding.
 */
data class Base32Options(
    val format: Base32Format = Base32Format.RFC4648,
    val padding: Boolean = true,
    val casing: Base32Casing = Base32Casing.UPPERCASE,
    val ignoreWhitespace: Boolean = true
)
