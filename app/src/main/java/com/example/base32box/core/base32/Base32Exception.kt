package com.example.base32box.core.base32

/**
 * Exception thrown when Base32 decoding or validation fails.
 */
class Base32Exception(
    override val message: String,
    val position: Int = -1,
    val offendingChar: Char? = null,
    val reason: Reason = Reason.OTHER
) : IllegalArgumentException(message) {

    enum class Reason {
        INVALID_CHARACTER,
        INVALID_PADDING,
        INVALID_LENGTH,
        MALFORMED_INPUT,
        UNEXPECTED_DATA_AFTER_PADDING,
        NON_ZERO_PADDING_BITS,
        OTHER
    }
}
