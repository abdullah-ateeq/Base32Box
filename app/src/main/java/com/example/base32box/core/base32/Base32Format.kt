package com.example.base32box.core.base32

/**
 * Supported Base32 encoding specifications.
 */
enum class Base32Format(
    val displayName: String,
    val alphabetDescription: String,
    val alphabet: String
) {
    /**
     * Standard Base32 specified in RFC 4648 Section 6.
     * Uses uppercase letters A-Z followed by digits 2-7.
     */
    RFC4648(
        displayName = "RFC 4648 Standard",
        alphabetDescription = "A–Z, 2–7",
        alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    ),

    /**
     * Base32 Extended Hex specified in RFC 4648 Section 7.
     * Uses digits 0-9 followed by letters A-V, preserving sort order of binary data.
     */
    BASE32_HEX(
        displayName = "Base32 Hex (Extended)",
        alphabetDescription = "0–9, A–V",
        alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUV"
    )
}
