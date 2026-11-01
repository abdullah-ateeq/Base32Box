package com.example.base32box.core.base32

import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * High-performance, RFC 4648 compliant Base32 and Base32 Hex engine.
 */
object Base32Engine {

    private const val RFC4648_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private const val BASE32_HEX_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUV"

    // Lookup tables for 0..127 ASCII
    private val RFC4648_DECODE_TABLE = IntArray(128) { -1 }
    private val BASE32_HEX_DECODE_TABLE = IntArray(128) { -1 }

    init {
        for (i in RFC4648_ALPHABET.indices) {
            val c = RFC4648_ALPHABET[i]
            RFC4648_DECODE_TABLE[c.code] = i
            RFC4648_DECODE_TABLE[c.lowercaseChar().code] = i
        }
        for (i in BASE32_HEX_ALPHABET.indices) {
            val c = BASE32_HEX_ALPHABET[i]
            BASE32_HEX_DECODE_TABLE[c.code] = i
            if (c.isLetter()) {
                BASE32_HEX_DECODE_TABLE[c.lowercaseChar().code] = i
            }
        }
    }

    private fun getAlphabet(format: Base32Format, casing: Base32Casing): String {
        val base = when (format) {
            Base32Format.RFC4648 -> RFC4648_ALPHABET
            Base32Format.BASE32_HEX -> BASE32_HEX_ALPHABET
        }
        return if (casing == Base32Casing.LOWERCASE) base.lowercase() else base
    }

    private fun getDecodeTable(format: Base32Format): IntArray {
        return when (format) {
            Base32Format.RFC4648 -> RFC4648_DECODE_TABLE
            Base32Format.BASE32_HEX -> BASE32_HEX_DECODE_TABLE
        }
    }

    /**
     * Encode byte array to Base32 string.
     */
    fun encode(bytes: ByteArray, options: Base32Options = Base32Options()): String {
        if (bytes.isEmpty()) return ""

        val alphabet = getAlphabet(options.format, options.casing)
        val pad = options.padding
        val outputLength = if (pad) {
            ((bytes.size + 4) / 5) * 8
        } else {
            val fullBlocks = bytes.size / 5
            val rem = bytes.size % 5
            val remChars = when (rem) {
                0 -> 0
                1 -> 2
                2 -> 4
                3 -> 5
                4 -> 7
                else -> 0
            }
            fullBlocks * 8 + remChars
        }

        val sb = StringBuilder(outputLength)
        var i = 0
        val len = bytes.size

        while (i + 4 < len) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = bytes[i + 1].toInt() and 0xFF
            val b2 = bytes[i + 2].toInt() and 0xFF
            val b3 = bytes[i + 3].toInt() and 0xFF
            val b4 = bytes[i + 4].toInt() and 0xFF

            sb.append(alphabet[(b0 ushr 3) and 0x1F])
            sb.append(alphabet[((b0 and 0x07) shl 2) or ((b1 ushr 6) and 0x03)])
            sb.append(alphabet[(b1 ushr 1) and 0x1F])
            sb.append(alphabet[((b1 and 0x01) shl 4) or ((b2 ushr 4) and 0x0F)])
            sb.append(alphabet[((b2 and 0x0F) shl 1) or ((b3 ushr 7) and 0x01)])
            sb.append(alphabet[(b3 ushr 2) and 0x1F])
            sb.append(alphabet[((b3 and 0x03) shl 3) or ((b4 ushr 5) and 0x07)])
            sb.append(alphabet[b4 and 0x1F])

            i += 5
        }

        val remaining = len - i
        if (remaining > 0) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = if (remaining > 1) bytes[i + 1].toInt() and 0xFF else 0
            val b2 = if (remaining > 2) bytes[i + 2].toInt() and 0xFF else 0
            val b3 = if (remaining > 3) bytes[i + 3].toInt() and 0xFF else 0

            when (remaining) {
                1 -> {
                    sb.append(alphabet[(b0 ushr 3) and 0x1F])
                    sb.append(alphabet[(b0 and 0x07) shl 2])
                    if (pad) sb.append("======")
                }
                2 -> {
                    sb.append(alphabet[(b0 ushr 3) and 0x1F])
                    sb.append(alphabet[((b0 and 0x07) shl 2) or ((b1 ushr 6) and 0x03)])
                    sb.append(alphabet[(b1 ushr 1) and 0x1F])
                    sb.append(alphabet[(b1 and 0x01) shl 4])
                    if (pad) sb.append("====")
                }
                3 -> {
                    sb.append(alphabet[(b0 ushr 3) and 0x1F])
                    sb.append(alphabet[((b0 and 0x07) shl 2) or ((b1 ushr 6) and 0x03)])
                    sb.append(alphabet[(b1 ushr 1) and 0x1F])
                    sb.append(alphabet[((b1 and 0x01) shl 4) or ((b2 ushr 4) and 0x0F)])
                    sb.append(alphabet[(b2 and 0x0F) shl 1])
                    if (pad) sb.append("===")
                }
                4 -> {
                    sb.append(alphabet[(b0 ushr 3) and 0x1F])
                    sb.append(alphabet[((b0 and 0x07) shl 2) or ((b1 ushr 6) and 0x03)])
                    sb.append(alphabet[(b1 ushr 1) and 0x1F])
                    sb.append(alphabet[((b1 and 0x01) shl 4) or ((b2 ushr 4) and 0x0F)])
                    sb.append(alphabet[((b2 and 0x0F) shl 1) or ((b3 ushr 7) and 0x01)])
                    sb.append(alphabet[(b3 ushr 2) and 0x1F])
                    sb.append(alphabet[(b3 and 0x03) shl 3])
                    if (pad) sb.append("=")
                }
            }
        }

        return sb.toString()
    }

    /**
     * Decode Base32 string to raw byte array.
     */
    fun decode(input: String, options: Base32Options = Base32Options()): ByteArray {
        if (input.isEmpty()) return ByteArray(0)

        val table = getDecodeTable(options.format)
        val validChars = IntArray(8)
        var validCount = 0
        var padCount = 0
        var hasSeenPad = false

        // Pre-allocate approximate output buffer
        val approxBytes = (input.length * 5) / 8 + 8
        val outBuf = ByteArray(approxBytes)
        var outPos = 0

        var originalIndex = 0
        while (originalIndex < input.length) {
            val ch = input[originalIndex]
            if (options.ignoreWhitespace && ch.isWhitespace()) {
                originalIndex++
                continue
            }

            if (!options.ignoreWhitespace && ch.isWhitespace()) {
                throw Base32Exception(
                    message = "Whitespace not allowed at character index $originalIndex",
                    position = originalIndex,
                    offendingChar = ch,
                    reason = Base32Exception.Reason.INVALID_CHARACTER
                )
            }

            if (ch == '=') {
                hasSeenPad = true
                padCount++
            } else {
                if (hasSeenPad) {
                    throw Base32Exception(
                        message = "Unexpected character '$ch' found after '=' padding at index $originalIndex",
                        position = originalIndex,
                        offendingChar = ch,
                        reason = Base32Exception.Reason.UNEXPECTED_DATA_AFTER_PADDING
                    )
                }

                val code = ch.code
                val value = if (code in 0..127) table[code] else -1
                if (value == -1) {
                    throw Base32Exception(
                        message = "Invalid character '$ch' for ${options.format.displayName} at index $originalIndex",
                        position = originalIndex,
                        offendingChar = ch,
                        reason = Base32Exception.Reason.INVALID_CHARACTER
                    )
                }
                validChars[validCount] = value
                validCount++
            }

            if (validCount + padCount == 8) {
                outPos = decodeQuantum(validChars, validCount, padCount, outBuf, outPos, originalIndex)
                validCount = 0
                padCount = 0
            }

            originalIndex++
        }

        if (validCount > 0 || padCount > 0) {
            if (padCount > 0) {
                // Incomplete quantum with padding
                throw Base32Exception(
                    message = "Incomplete padded block of ${validCount + padCount} characters (expected 8)",
                    position = originalIndex - 1,
                    reason = Base32Exception.Reason.INVALID_PADDING
                )
            } else {
                // Unpadded quantum at end
                outPos = decodeUnpaddedTail(validChars, validCount, outBuf, outPos, originalIndex - 1)
            }
        }

        return outBuf.copyOf(outPos)
    }

    private fun decodeQuantum(
        v: IntArray,
        validCount: Int,
        padCount: Int,
        outBuf: ByteArray,
        outPos: Int,
        errorPos: Int
    ): Int {
        var pos = outPos
        when (padCount) {
            0 -> {
                outBuf[pos++] = (((v[0] shl 3) or (v[1] ushr 2)) and 0xFF).toByte()
                outBuf[pos++] = (((v[1] shl 6) or (v[2] shl 1) or (v[3] ushr 4)) and 0xFF).toByte()
                outBuf[pos++] = (((v[3] shl 4) or (v[4] ushr 1)) and 0xFF).toByte()
                outBuf[pos++] = (((v[4] shl 7) or (v[5] shl 2) or (v[6] ushr 3)) and 0xFF).toByte()
                outBuf[pos++] = (((v[6] shl 5) or v[7]) and 0xFF).toByte()
            }
            1 -> { // 7 data chars -> 4 bytes
                if ((v[6] and 0x07) != 0) {
                    throw Base32Exception(
                        message = "Non-zero padding bits detected in character 7",
                        position = errorPos,
                        reason = Base32Exception.Reason.NON_ZERO_PADDING_BITS
                    )
                }
                outBuf[pos++] = (((v[0] shl 3) or (v[1] ushr 2)) and 0xFF).toByte()
                outBuf[pos++] = (((v[1] shl 6) or (v[2] shl 1) or (v[3] ushr 4)) and 0xFF).toByte()
                outBuf[pos++] = (((v[3] shl 4) or (v[4] ushr 1)) and 0xFF).toByte()
                outBuf[pos++] = (((v[4] shl 7) or (v[5] shl 2) or (v[6] ushr 3)) and 0xFF).toByte()
            }
            3 -> { // 5 data chars -> 3 bytes
                if ((v[4] and 0x01) != 0) {
                    throw Base32Exception(
                        message = "Non-zero padding bits detected in character 5",
                        position = errorPos,
                        reason = Base32Exception.Reason.NON_ZERO_PADDING_BITS
                    )
                }
                outBuf[pos++] = (((v[0] shl 3) or (v[1] ushr 2)) and 0xFF).toByte()
                outBuf[pos++] = (((v[1] shl 6) or (v[2] shl 1) or (v[3] ushr 4)) and 0xFF).toByte()
                outBuf[pos++] = (((v[3] shl 4) or (v[4] ushr 1)) and 0xFF).toByte()
            }
            4 -> { // 4 data chars -> 2 bytes
                if ((v[3] and 0x0F) != 0) {
                    throw Base32Exception(
                        message = "Non-zero padding bits detected in character 4",
                        position = errorPos,
                        reason = Base32Exception.Reason.NON_ZERO_PADDING_BITS
                    )
                }
                outBuf[pos++] = (((v[0] shl 3) or (v[1] ushr 2)) and 0xFF).toByte()
                outBuf[pos++] = (((v[1] shl 6) or (v[2] shl 1) or (v[3] ushr 4)) and 0xFF).toByte()
            }
            6 -> { // 2 data chars -> 1 byte
                if ((v[1] and 0x03) != 0) {
                    throw Base32Exception(
                        message = "Non-zero padding bits detected in character 2",
                        position = errorPos,
                        reason = Base32Exception.Reason.NON_ZERO_PADDING_BITS
                    )
                }
                outBuf[pos++] = (((v[0] shl 3) or (v[1] ushr 2)) and 0xFF).toByte()
            }
            else -> {
                throw Base32Exception(
                    message = "Invalid padding count: $padCount '=' (RFC 4648 only permits 1, 3, 4, or 6 '=' pads)",
                    position = errorPos,
                    reason = Base32Exception.Reason.INVALID_PADDING
                )
            }
        }
        return pos
    }

    private fun decodeUnpaddedTail(
        v: IntArray,
        count: Int,
        outBuf: ByteArray,
        outPos: Int,
        errorPos: Int
    ): Int {
        var pos = outPos
        when (count) {
            2 -> {
                if ((v[1] and 0x03) != 0) {
                    throw Base32Exception(
                        message = "Non-zero padding bits detected in trailing character",
                        position = errorPos,
                        reason = Base32Exception.Reason.NON_ZERO_PADDING_BITS
                    )
                }
                outBuf[pos++] = (((v[0] shl 3) or (v[1] ushr 2)) and 0xFF).toByte()
            }
            4 -> {
                if ((v[3] and 0x0F) != 0) {
                    throw Base32Exception(
                        message = "Non-zero padding bits detected in trailing character",
                        position = errorPos,
                        reason = Base32Exception.Reason.NON_ZERO_PADDING_BITS
                    )
                }
                outBuf[pos++] = (((v[0] shl 3) or (v[1] ushr 2)) and 0xFF).toByte()
                outBuf[pos++] = (((v[1] shl 6) or (v[2] shl 1) or (v[3] ushr 4)) and 0xFF).toByte()
            }
            5 -> {
                if ((v[4] and 0x01) != 0) {
                    throw Base32Exception(
                        message = "Non-zero padding bits detected in trailing character",
                        position = errorPos,
                        reason = Base32Exception.Reason.NON_ZERO_PADDING_BITS
                    )
                }
                outBuf[pos++] = (((v[0] shl 3) or (v[1] ushr 2)) and 0xFF).toByte()
                outBuf[pos++] = (((v[1] shl 6) or (v[2] shl 1) or (v[3] ushr 4)) and 0xFF).toByte()
                outBuf[pos++] = (((v[3] shl 4) or (v[4] ushr 1)) and 0xFF).toByte()
            }
            7 -> {
                if ((v[6] and 0x07) != 0) {
                    throw Base32Exception(
                        message = "Non-zero padding bits detected in trailing character",
                        position = errorPos,
                        reason = Base32Exception.Reason.NON_ZERO_PADDING_BITS
                    )
                }
                outBuf[pos++] = (((v[0] shl 3) or (v[1] ushr 2)) and 0xFF).toByte()
                outBuf[pos++] = (((v[1] shl 6) or (v[2] shl 1) or (v[3] ushr 4)) and 0xFF).toByte()
                outBuf[pos++] = (((v[3] shl 4) or (v[4] ushr 1)) and 0xFF).toByte()
                outBuf[pos++] = (((v[4] shl 7) or (v[5] shl 2) or (v[6] ushr 3)) and 0xFF).toByte()
            }
            else -> {
                throw Base32Exception(
                    message = "Malformed Base32 input: Trailing fragment of $count characters is invalid (valid lengths modulo 8 are 0, 2, 4, 5, 7)",
                    position = errorPos,
                    reason = Base32Exception.Reason.INVALID_LENGTH
                )
            }
        }
        return pos
    }

    /**
     * Encode UTF-8 text to Base32 string.
     */
    fun encodeText(text: String, options: Base32Options = Base32Options()): String {
        return encode(text.toByteArray(StandardCharsets.UTF_8), options)
    }

    /**
     * Result of decoding Base32 to text.
     */
    data class TextDecodeResult(
        val text: String,
        val rawBytes: ByteArray,
        val isUtf8: Boolean,
        val charCount: Int,
        val byteCount: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as TextDecodeResult
            return text == other.text && rawBytes.contentEquals(other.rawBytes) && isUtf8 == other.isUtf8
        }

        override fun hashCode(): Int {
            var result = text.hashCode()
            result = 31 * result + rawBytes.contentHashCode()
            result = 31 * result + isUtf8.hashCode()
            return result
        }
    }

    /**
     * Decode Base32 string to text. Accurately verifies UTF-8 compliance.
     */
    fun decodeText(input: String, options: Base32Options = Base32Options()): TextDecodeResult {
        val rawBytes = decode(input, options)
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)

        return try {
            val charBuffer = decoder.decode(ByteBuffer.wrap(rawBytes))
            val text = charBuffer.toString()
            TextDecodeResult(
                text = text,
                rawBytes = rawBytes,
                isUtf8 = true,
                charCount = text.length,
                byteCount = rawBytes.size
            )
        } catch (_: Exception) {
            // Not valid UTF-8 text (e.g. arbitrary binary)
            val fallbackText = String(rawBytes, StandardCharsets.ISO_8859_1)
            TextDecodeResult(
                text = fallbackText,
                rawBytes = rawBytes,
                isUtf8 = false,
                charCount = fallbackText.length,
                byteCount = rawBytes.size
            )
        }
    }

    /**
     * Stream-encode an InputStream to an OutputStream with buffered chunks.
     */
    fun encodeStream(
        input: InputStream,
        output: OutputStream,
        options: Base32Options = Base32Options(),
        onProgress: ((bytesRead: Long) -> Unit)? = null
    ): Long {
        val alphabet = getAlphabet(options.format, options.casing)
        // Read buffer must be a multiple of 5 for block alignment
        val bufferSize = 8192 * 5 // 40,960 bytes
        val buffer = ByteArray(bufferSize)
        val outCharBuffer = CharArray(bufferSize / 5 * 8)

        var totalBytesRead = 0L
        var bytesInBlock = 0
        val singleBlock = ByteArray(5)

        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            totalBytesRead += read
            var offset = 0

            // If we had leftover bytes from previous chunk
            if (bytesInBlock > 0) {
                while (offset < read && bytesInBlock < 5) {
                    singleBlock[bytesInBlock++] = buffer[offset++]
                }
                if (bytesInBlock == 5) {
                    val encoded5 = encode(singleBlock, options.copy(padding = false))
                    output.write(encoded5.toByteArray(StandardCharsets.US_ASCII))
                    bytesInBlock = 0
                }
            }

            // Process full 5-byte multiples
            val remaining = read - offset
            val fullBlocks = remaining / 5
            val fullBytes = fullBlocks * 5

            var charPos = 0
            var i = offset
            val end = offset + fullBytes
            while (i < end) {
                val b0 = buffer[i].toInt() and 0xFF
                val b1 = buffer[i + 1].toInt() and 0xFF
                val b2 = buffer[i + 2].toInt() and 0xFF
                val b3 = buffer[i + 3].toInt() and 0xFF
                val b4 = buffer[i + 4].toInt() and 0xFF

                outCharBuffer[charPos++] = alphabet[(b0 ushr 3) and 0x1F]
                outCharBuffer[charPos++] = alphabet[((b0 and 0x07) shl 2) or ((b1 ushr 6) and 0x03)]
                outCharBuffer[charPos++] = alphabet[(b1 ushr 1) and 0x1F]
                outCharBuffer[charPos++] = alphabet[((b1 and 0x01) shl 4) or ((b2 ushr 4) and 0x0F)]
                outCharBuffer[charPos++] = alphabet[((b2 and 0x0F) shl 1) or ((b3 ushr 7) and 0x01)]
                outCharBuffer[charPos++] = alphabet[(b3 ushr 2) and 0x1F]
                outCharBuffer[charPos++] = alphabet[((b3 and 0x03) shl 3) or ((b4 ushr 5) and 0x07)]
                outCharBuffer[charPos++] = alphabet[b4 and 0x1F]

                i += 5
            }

            if (charPos > 0) {
                val charsBytes = String(outCharBuffer, 0, charPos).toByteArray(StandardCharsets.US_ASCII)
                output.write(charsBytes)
            }

            // Save leftover bytes
            offset = end
            while (offset < read) {
                singleBlock[bytesInBlock++] = buffer[offset++]
            }

            onProgress?.invoke(totalBytesRead)
        }

        // Handle final trailing bytes
        if (bytesInBlock > 0) {
            val leftover = singleBlock.copyOf(bytesInBlock)
            val tailEncoded = encode(leftover, options)
            output.write(tailEncoded.toByteArray(StandardCharsets.US_ASCII))
        }

        output.flush()
        return totalBytesRead
    }

    /**
     * Stream-decode a Base32 character Reader or stream to an OutputStream.
     */
    fun decodeStream(
        reader: Reader,
        output: OutputStream,
        options: Base32Options = Base32Options(),
        onProgress: ((charsRead: Long) -> Unit)? = null
    ): Long {
        val table = getDecodeTable(options.format)
        val validChars = IntArray(8)
        var validCount = 0
        var padCount = 0
        var hasSeenPad = false

        val readBuffer = CharArray(8192)
        val outBuf = ByteArray(5)
        var totalCharsRead = 0L
        var globalIndex = 0

        var charsRead: Int
        while (reader.read(readBuffer).also { charsRead = it } != -1) {
            totalCharsRead += charsRead
            for (i in 0 until charsRead) {
                val ch = readBuffer[i]
                if (options.ignoreWhitespace && ch.isWhitespace()) {
                    globalIndex++
                    continue
                }

                if (!options.ignoreWhitespace && ch.isWhitespace()) {
                    throw Base32Exception(
                        message = "Whitespace not allowed at character index $globalIndex",
                        position = globalIndex,
                        offendingChar = ch,
                        reason = Base32Exception.Reason.INVALID_CHARACTER
                    )
                }

                if (ch == '=') {
                    hasSeenPad = true
                    padCount++
                } else {
                    if (hasSeenPad) {
                        throw Base32Exception(
                            message = "Unexpected character '$ch' found after '=' padding at index $globalIndex",
                            position = globalIndex,
                            offendingChar = ch,
                            reason = Base32Exception.Reason.UNEXPECTED_DATA_AFTER_PADDING
                        )
                    }

                    val code = ch.code
                    val value = if (code in 0..127) table[code] else -1
                    if (value == -1) {
                        throw Base32Exception(
                            message = "Invalid character '$ch' for ${options.format.displayName} at index $globalIndex",
                            position = globalIndex,
                            offendingChar = ch,
                            reason = Base32Exception.Reason.INVALID_CHARACTER
                        )
                    }
                    validChars[validCount] = value
                    validCount++
                }

                if (validCount + padCount == 8) {
                    val written = decodeQuantum(validChars, validCount, padCount, outBuf, 0, globalIndex)
                    output.write(outBuf, 0, written)
                    validCount = 0
                    padCount = 0
                }

                globalIndex++
            }
            onProgress?.invoke(totalCharsRead)
        }

        if (validCount > 0 || padCount > 0) {
            if (padCount > 0) {
                throw Base32Exception(
                    message = "Incomplete padded block of ${validCount + padCount} characters (expected 8)",
                    position = globalIndex - 1,
                    reason = Base32Exception.Reason.INVALID_PADDING
                )
            } else {
                val written = decodeUnpaddedTail(validChars, validCount, outBuf, 0, globalIndex - 1)
                output.write(outBuf, 0, written)
            }
        }

        output.flush()
        return totalCharsRead
    }
}
