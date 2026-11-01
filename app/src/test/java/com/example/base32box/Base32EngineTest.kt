package com.example.base32box

import com.example.base32box.core.base32.Base32Casing
import com.example.base32box.core.base32.Base32Engine
import com.example.base32box.core.base32.Base32Exception
import com.example.base32box.core.base32.Base32Format
import com.example.base32box.core.base32.Base32Options
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Random

class Base32EngineTest {

    // RFC 4648 Section 10 Test Vectors for Standard Base32
    private val rfc4648Vectors = listOf(
        "" to "",
        "f" to "MY======",
        "fo" to "MZXQ====",
        "foo" to "MZXW6===",
        "foob" to "MZXW6YQ=",
        "fooba" to "MZXW6YTB",
        "foobar" to "MZXW6YTBOI======"
    )

    // RFC 4648 Section 10 Test Vectors for Base32 Extended Hex
    private val rfc4648HexVectors = listOf(
        "" to "",
        "f" to "CO======",
        "fo" to "CPNG====",
        "foo" to "CPNMU===",
        "foob" to "CPNMUOG=",
        "fooba" to "CPNMUOJ1",
        "foobar" to "CPNMUOJ1E8======"
    )

    @Test
    fun testRfc4648StandardVectors() {
        val options = Base32Options(format = Base32Format.RFC4648, padding = true)
        for ((plain, expectedEncoded) in rfc4648Vectors) {
            val encoded = Base32Engine.encodeText(plain, options)
            assertEquals("Encoding '$plain'", expectedEncoded, encoded)

            val decoded = Base32Engine.decodeText(encoded, options)
            assertEquals("Decoding '$expectedEncoded'", plain, decoded.text)
            assertTrue(decoded.isUtf8)
        }
    }

    @Test
    fun testRfc4648HexVectors() {
        val options = Base32Options(format = Base32Format.BASE32_HEX, padding = true)
        for ((plain, expectedEncoded) in rfc4648HexVectors) {
            val encoded = Base32Engine.encodeText(plain, options)
            assertEquals("Encoding '$plain' with Base32Hex", expectedEncoded, encoded)

            val decoded = Base32Engine.decodeText(encoded, options)
            assertEquals("Decoding '$expectedEncoded' with Base32Hex", plain, decoded.text)
            assertTrue(decoded.isUtf8)
        }
    }

    @Test
    fun testEmptyInput() {
        val emptyBytes = ByteArray(0)
        assertEquals("", Base32Engine.encode(emptyBytes))
        assertArrayEquals(emptyBytes, Base32Engine.decode(""))
        assertEquals("", Base32Engine.encodeText(""))
        val result = Base32Engine.decodeText("")
        assertEquals("", result.text)
        assertEquals(0, result.byteCount)
        assertEquals(0, result.charCount)
        assertTrue(result.isUtf8)
    }

    @Test
    fun testPaddingDisabled() {
        val unpaddedOptions = Base32Options(format = Base32Format.RFC4648, padding = false)
        val expectedUnpadded = listOf(
            "" to "",
            "f" to "MY",
            "fo" to "MZXQ",
            "foo" to "MZXW6",
            "foob" to "MZXW6YQ",
            "fooba" to "MZXW6YTB",
            "foobar" to "MZXW6YTBOI"
        )

        for ((plain, expected) in expectedUnpadded) {
            val encoded = Base32Engine.encodeText(plain, unpaddedOptions)
            assertEquals(expected, encoded)

            // Can decode unpadded strings
            val decoded = Base32Engine.decodeText(encoded, unpaddedOptions)
            assertEquals(plain, decoded.text)
        }
    }

    @Test
    fun testCasingHandling() {
        val lowerOptions = Base32Options(
            format = Base32Format.RFC4648,
            padding = true,
            casing = Base32Casing.LOWERCASE
        )
        val encoded = Base32Engine.encodeText("foobar", lowerOptions)
        assertEquals("mzxw6ytboi======", encoded)

        // Case-insensitive decoding
        val decodedFromLower = Base32Engine.decodeText("mzxw6ytboi======", lowerOptions)
        assertEquals("foobar", decodedFromLower.text)

        val decodedFromMixed = Base32Engine.decodeText("MzXw6YtBoI======", lowerOptions)
        assertEquals("foobar", decodedFromMixed.text)
    }

    @Test
    fun testWhitespaceTolerance() {
        val inputWithWhitespace = "  MZXW\t\n6YTB\r\nOI======  \n"
        // When ignoreWhitespace is true
        val decoded = Base32Engine.decodeText(inputWithWhitespace, Base32Options(ignoreWhitespace = true))
        assertEquals("foobar", decoded.text)

        // When ignoreWhitespace is false
        val ex = assertThrows(Base32Exception::class.java) {
            Base32Engine.decodeText(inputWithWhitespace, Base32Options(ignoreWhitespace = false))
        }
        assertEquals(Base32Exception.Reason.INVALID_CHARACTER, ex.reason)
    }

    @Test
    fun testInvalidCharacters() {
        // '8', '9', '0', '1' are not valid in standard RFC 4648
        val invalidInputs = listOf("M8======", "MY9=====", "MZX0====", "MZX1====")
        for (inv in invalidInputs) {
            val ex = assertThrows(Base32Exception::class.java) {
                Base32Engine.decode(inv, Base32Options(format = Base32Format.RFC4648))
            }
            assertEquals(Base32Exception.Reason.INVALID_CHARACTER, ex.reason)
        }

        // 'W', 'Z' are not valid in Base32 Hex (only 0-9 and A-V)
        val invalidHex = "CPNW===="
        val hexEx = assertThrows(Base32Exception::class.java) {
            Base32Engine.decode(invalidHex, Base32Options(format = Base32Format.BASE32_HEX))
        }
        assertEquals(Base32Exception.Reason.INVALID_CHARACTER, hexEx.reason)
    }

    @Test
    fun testMalformedPaddingAndDataAfterPadding() {
        // Data after padding
        val exTrailing = assertThrows(Base32Exception::class.java) {
            Base32Engine.decode("MY======A")
        }
        assertEquals(Base32Exception.Reason.UNEXPECTED_DATA_AFTER_PADDING, exTrailing.reason)

        // Invalid number of padding characters (e.g., 2 '=' pads or 5 '=' pads)
        val exTwoPads = assertThrows(Base32Exception::class.java) {
            Base32Engine.decode("MZXW6Y==") // 6 chars + 2 pads = invalid RFC 4648
        }
        assertEquals(Base32Exception.Reason.INVALID_PADDING, exTwoPads.reason)

        val exFivePads = assertThrows(Base32Exception::class.java) {
            Base32Engine.decode("MZX=====") // 3 chars + 5 pads = invalid RFC 4648
        }
        assertEquals(Base32Exception.Reason.INVALID_PADDING, exFivePads.reason)

        // Invalid unpadded lengths (1, 3, 6 data characters)
        val exSingleChar = assertThrows(Base32Exception::class.java) {
            Base32Engine.decode("M")
        }
        assertEquals(Base32Exception.Reason.INVALID_LENGTH, exSingleChar.reason)

        val exThreeChars = assertThrows(Base32Exception::class.java) {
            Base32Engine.decode("MZX")
        }
        assertEquals(Base32Exception.Reason.INVALID_LENGTH, exThreeChars.reason)
    }

    @Test
    fun testNonZeroPaddingBits() {
        // RFC 4648 Section 3.5 requires unused bits in the final quantum to be zero.
        // For 'f' -> "MY======". 'M' = 12 (01100), 'Y' = 24 (11000). The last 2 bits of Y are 00.
        // If we change 'Y' to 'Z' (25 = 11001), the lowest bit is 1. That must be rejected!
        val ex = assertThrows(Base32Exception::class.java) {
            Base32Engine.decode("MZ======")
        }
        assertEquals(Base32Exception.Reason.NON_ZERO_PADDING_BITS, ex.reason)
    }

    @Test
    fun testUtf8MultiByteEncoding() {
        val testStrings = listOf(
            "Hello, 世界! 🌍",
            "مرحبا بالعالم",
            "Café au lait & résumé",
            "København, 日本語, 한국어, 🚀🎉📦"
        )

        for (str in testStrings) {
            for (format in listOf(Base32Format.RFC4648, Base32Format.BASE32_HEX)) {
                for (padding in listOf(true, false)) {
                    val opts = Base32Options(format = format, padding = padding)
                    val encoded = Base32Engine.encodeText(str, opts)
                    val result = Base32Engine.decodeText(encoded, opts)
                    assertEquals(str, result.text)
                    assertTrue(result.isUtf8)
                    assertEquals(str.toByteArray(StandardCharsets.UTF_8).size, result.byteCount)
                }
            }
        }
    }

    @Test
    fun testBinaryRoundTripIntegrity() {
        val random = Random(42)
        for (size in listOf(1, 2, 3, 4, 5, 7, 8, 15, 16, 17, 33, 100, 255, 1024, 8192)) {
            val bytes = ByteArray(size)
            random.nextBytes(bytes)

            for (format in listOf(Base32Format.RFC4648, Base32Format.BASE32_HEX)) {
                for (padding in listOf(true, false)) {
                    val opts = Base32Options(format = format, padding = padding)
                    val encoded = Base32Engine.encode(bytes, opts)
                    val decoded = Base32Engine.decode(encoded, opts)
                    assertArrayEquals("Failed for size=$size, format=$format, padding=$padding", bytes, decoded)
                }
            }
        }
    }

    @Test
    fun testStreamEncodeAndDecode() {
        val random = Random(1337)
        // 500 KB test buffer for streaming
        val data = ByteArray(500 * 1024)
        random.nextBytes(data)

        for (format in listOf(Base32Format.RFC4648, Base32Format.BASE32_HEX)) {
            for (padding in listOf(true, false)) {
                val opts = Base32Options(format = format, padding = padding)

                // Stream encode
                val inStream = ByteArrayInputStream(data)
                val outStream = ByteArrayOutputStream()
                var progressBytes = 0L
                Base32Engine.encodeStream(inStream, outStream, opts) {
                    progressBytes = it
                }
                assertEquals(data.size.toLong(), progressBytes)
                val encodedBytes = outStream.toByteArray()

                // Verify encoded string matches in-memory encode
                val memoryEncoded = Base32Engine.encode(data, opts)
                assertEquals(memoryEncoded, String(encodedBytes, StandardCharsets.US_ASCII))

                // Stream decode
                val reader = InputStreamReader(ByteArrayInputStream(encodedBytes), StandardCharsets.US_ASCII)
                val decodedOut = ByteArrayOutputStream()
                Base32Engine.decodeStream(reader, decodedOut, opts)
                val decodedData = decodedOut.toByteArray()

                assertArrayEquals("Stream roundtrip failed for format=$format, pad=$padding", data, decodedData)
            }
        }
    }
}
