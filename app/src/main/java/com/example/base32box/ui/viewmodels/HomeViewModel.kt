package com.example.base32box.ui.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.base32box.core.base32.Base32Casing
import com.example.base32box.core.base32.Base32Engine
import com.example.base32box.core.base32.Base32Exception
import com.example.base32box.core.base32.Base32Format
import com.example.base32box.core.base32.Base32Options
import com.example.base32box.data.preferences.OperationMode
import com.example.base32box.data.preferences.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

data class HomeUiState(
    val mode: OperationMode = OperationMode.ENCODE,
    val sourceTab: Int = 0, // 0 = Text, 1 = File
    val format: Base32Format = Base32Format.RFC4648,
    val paddingEnabled: Boolean = true,
    val casing: Base32Casing = Base32Casing.UPPERCASE,
    val ignoreWhitespace: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val clearOnExit: Boolean = false,

    // Text mode state
    val inputText: String = "",
    val outputText: String = "",
    val isOutputUtf8: Boolean = true,
    val inputCharCount: Int = 0,
    val inputByteCount: Int = 0,
    val outputCharCount: Int = 0,
    val outputByteCount: Int = 0,
    val errorMessage: String? = null,
    val errorPosition: Int = -1,
    val errorReason: Base32Exception.Reason? = null,

    // File mode state
    val fileUri: Uri? = null,
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    val fileMimeType: String = "",
    val isFileProcessing: Boolean = false,
    val fileProgressPercent: Int = 0,
    val processedFile: File? = null,
    val processedFileBytes: Long = 0L,
    val suggestedOutputFileName: String = "",
    val fileErrorMessage: String? = null,
    val fileSuccessMessage: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var hasInitializedFromPrefs = false

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.userPreferencesFlow.first()
            if (!hasInitializedFromPrefs) {
                _uiState.update { current ->
                    current.copy(
                        mode = prefs.defaultOperation,
                        format = prefs.defaultFormat,
                        paddingEnabled = prefs.paddingEnabled,
                        casing = prefs.casing,
                        ignoreWhitespace = prefs.ignoreWhitespace,
                        clearOnExit = prefs.clearOnExit,
                        hapticFeedbackEnabled = prefs.hapticFeedbackEnabled
                    )
                }
                hasInitializedFromPrefs = true
            }

            // Keep observing live preference changes for active settings
            preferencesRepository.userPreferencesFlow.collect { updatedPrefs ->
                _uiState.update { current ->
                    current.copy(
                        hapticFeedbackEnabled = updatedPrefs.hapticFeedbackEnabled,
                        clearOnExit = updatedPrefs.clearOnExit
                    )
                }
            }
        }
    }

    fun setMode(mode: OperationMode) {
        _uiState.update { it.copy(mode = mode, errorMessage = null, fileErrorMessage = null) }
        // Re-process if there is input
        if (_uiState.value.inputText.isNotEmpty()) {
            processText()
        }
    }

    fun setSourceTab(tab: Int) {
        _uiState.update { it.copy(sourceTab = tab) }
    }

    fun setFormat(format: Base32Format) {
        _uiState.update { it.copy(format = format) }
        if (_uiState.value.inputText.isNotEmpty()) {
            processText()
        }
    }

    fun setPaddingEnabled(enabled: Boolean) {
        _uiState.update { it.copy(paddingEnabled = enabled) }
        if (_uiState.value.inputText.isNotEmpty()) {
            processText()
        }
    }

    fun setCasing(casing: Base32Casing) {
        _uiState.update { it.copy(casing = casing) }
        if (_uiState.value.inputText.isNotEmpty()) {
            processText()
        }
    }

    fun setIgnoreWhitespace(enabled: Boolean) {
        _uiState.update { it.copy(ignoreWhitespace = enabled) }
        if (_uiState.value.mode == OperationMode.DECODE && _uiState.value.inputText.isNotEmpty()) {
            processText()
        }
    }

    fun updateInputText(newText: String) {
        val inputBytes = newText.toByteArray(StandardCharsets.UTF_8)
        _uiState.update {
            it.copy(
                inputText = newText,
                inputCharCount = newText.length,
                inputByteCount = inputBytes.size,
                errorMessage = null
            )
        }
    }

    fun processText() {
        val state = _uiState.value
        val input = state.inputText
        if (input.isEmpty()) {
            _uiState.update {
                it.copy(
                    outputText = "",
                    outputCharCount = 0,
                    outputByteCount = 0,
                    errorMessage = null
                )
            }
            return
        }

        val options = Base32Options(
            format = state.format,
            padding = state.paddingEnabled,
            casing = state.casing,
            ignoreWhitespace = state.ignoreWhitespace
        )

        try {
            if (state.mode == OperationMode.ENCODE) {
                val encoded = Base32Engine.encodeText(input, options)
                val encodedBytes = encoded.toByteArray(StandardCharsets.US_ASCII)
                _uiState.update {
                    it.copy(
                        outputText = encoded,
                        isOutputUtf8 = true,
                        outputCharCount = encoded.length,
                        outputByteCount = encodedBytes.size,
                        errorMessage = null,
                        errorPosition = -1,
                        errorReason = null
                    )
                }
            } else {
                val decodeResult = Base32Engine.decodeText(input, options)
                _uiState.update {
                    it.copy(
                        outputText = decodeResult.text,
                        isOutputUtf8 = decodeResult.isUtf8,
                        outputCharCount = decodeResult.charCount,
                        outputByteCount = decodeResult.byteCount,
                        errorMessage = null,
                        errorPosition = -1,
                        errorReason = null
                    )
                }
            }
        } catch (e: Base32Exception) {
            _uiState.update {
                it.copy(
                    outputText = "",
                    outputCharCount = 0,
                    outputByteCount = 0,
                    errorMessage = e.message ?: "Decoding error",
                    errorPosition = e.position,
                    errorReason = e.reason
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    outputText = "",
                    outputCharCount = 0,
                    outputByteCount = 0,
                    errorMessage = e.message ?: "An unexpected error occurred",
                    errorPosition = -1,
                    errorReason = null
                )
            }
        }
    }

    fun clearInput() {
        _uiState.update {
            it.copy(
                inputText = "",
                inputCharCount = 0,
                inputByteCount = 0,
                errorMessage = null
            )
        }
    }

    fun clearOutput() {
        _uiState.update {
            it.copy(
                outputText = "",
                outputCharCount = 0,
                outputByteCount = 0,
                errorMessage = null
            )
        }
    }

    fun swapInputOutput() {
        val state = _uiState.value
        if (state.outputText.isNotEmpty()) {
            val newMode = if (state.mode == OperationMode.ENCODE) OperationMode.DECODE else OperationMode.ENCODE
            val newText = state.outputText
            val newBytes = newText.toByteArray(StandardCharsets.UTF_8)
            _uiState.update {
                it.copy(
                    mode = newMode,
                    inputText = newText,
                    inputCharCount = newText.length,
                    inputByteCount = newBytes.size,
                    outputText = "",
                    outputCharCount = 0,
                    outputByteCount = 0,
                    errorMessage = null
                )
            }
            processText()
        }
    }

    fun onFileSelected(uri: Uri, context: Context) {
        var name = "unknown_file"
        var size = 0L
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex)
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {
            name = uri.lastPathSegment ?: "selected_file"
        }

        val suggestedOutput = if (_uiState.value.mode == OperationMode.ENCODE) {
            "$name.b32.txt"
        } else {
            if (name.endsWith(".b32.txt")) {
                name.removeSuffix(".b32.txt")
            } else if (name.endsWith(".b32")) {
                name.removeSuffix(".b32")
            } else {
                "decoded_$name"
            }
        }

        _uiState.update {
            it.copy(
                fileUri = uri,
                fileName = name,
                fileSizeBytes = size,
                fileMimeType = mime,
                fileProgressPercent = 0,
                processedFile = null,
                processedFileBytes = 0L,
                suggestedOutputFileName = suggestedOutput,
                fileErrorMessage = null,
                fileSuccessMessage = null
            )
        }
    }

    fun processSelectedFile(context: Context) {
        val state = _uiState.value
        val uri = state.fileUri ?: return

        _uiState.update {
            it.copy(
                isFileProcessing = true,
                fileProgressPercent = 0,
                fileErrorMessage = null,
                fileSuccessMessage = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val options = Base32Options(
                format = state.format,
                padding = state.paddingEnabled,
                casing = state.casing,
                ignoreWhitespace = state.ignoreWhitespace
            )

            try {
                val cacheDir = context.cacheDir
                val tempOutputFile = File(cacheDir, "b32_${System.currentTimeMillis()}_${state.suggestedOutputFileName}")
                if (tempOutputFile.exists()) tempOutputFile.delete()

                val totalSize = state.fileSizeBytes.coerceAtLeast(1L)

                if (state.mode == OperationMode.ENCODE) {
                    context.contentResolver.openInputStream(uri)?.use { inStream ->
                        FileOutputStream(tempOutputFile).use { outStream ->
                            Base32Engine.encodeStream(inStream, outStream, options) { bytesRead ->
                                val percent = ((bytesRead * 100) / totalSize).toInt().coerceIn(0, 100)
                                _uiState.update { it.copy(fileProgressPercent = percent) }
                            }
                        }
                    } ?: throw IllegalStateException("Unable to open input stream for selected file.")
                } else {
                    context.contentResolver.openInputStream(uri)?.use { inStream ->
                        val reader = InputStreamReader(inStream, StandardCharsets.UTF_8)
                        FileOutputStream(tempOutputFile).use { outStream ->
                            Base32Engine.decodeStream(reader, outStream, options) { charsRead ->
                                val percent = ((charsRead * 100) / totalSize).toInt().coerceIn(0, 100)
                                _uiState.update { it.copy(fileProgressPercent = percent) }
                            }
                        }
                    } ?: throw IllegalStateException("Unable to open input stream for selected file.")
                }

                val finalOutputSize = tempOutputFile.length()
                _uiState.update {
                    it.copy(
                        isFileProcessing = false,
                        fileProgressPercent = 100,
                        processedFile = tempOutputFile,
                        processedFileBytes = finalOutputSize,
                        fileSuccessMessage = if (state.mode == OperationMode.ENCODE) {
                            "Successfully encoded to Base32 (${formatFileSize(finalOutputSize)})"
                        } else {
                            "Successfully decoded binary file (${formatFileSize(finalOutputSize)})"
                        }
                    )
                }
            } catch (e: Base32Exception) {
                _uiState.update {
                    it.copy(
                        isFileProcessing = false,
                        fileProgressPercent = 0,
                        fileErrorMessage = "Base32 Error: ${e.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isFileProcessing = false,
                        fileProgressPercent = 0,
                        fileErrorMessage = "Failed processing file: ${e.message}"
                    )
                }
            }
        }
    }

    fun saveProcessedFileToUri(destinationUri: Uri, context: Context, onResult: (Boolean, String) -> Unit) {
        val processed = _uiState.value.processedFile ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(destinationUri)?.use { outStream ->
                    FileInputStream(processed).use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
                withContext(Dispatchers.Main) {
                    onResult(true, "File saved successfully!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Failed to save file: ${e.message}")
                }
            }
        }
    }

    fun shareProcessedFile(context: Context) {
        val file = _uiState.value.processedFile ?: return
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mimeType = if (_uiState.value.mode == OperationMode.ENCODE) "text/plain" else "*/*"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Base32 File").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            _uiState.update { it.copy(fileErrorMessage = "Failed to share file: ${e.message}") }
        }
    }

    fun clearFile() {
        val tempFile = _uiState.value.processedFile
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete()
        }
        _uiState.update {
            it.copy(
                fileUri = null,
                fileName = "",
                fileSizeBytes = 0L,
                fileMimeType = "",
                isFileProcessing = false,
                fileProgressPercent = 0,
                processedFile = null,
                processedFileBytes = 0L,
                fileErrorMessage = null,
                fileSuccessMessage = null
            )
        }
    }

    fun onAppExitOrClearRequested() {
        if (_uiState.value.clearOnExit) {
            clearInput()
            clearOutput()
            clearFile()
        }
    }

    companion object {
        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(java.util.Locale.US, "%.2f %s", value, units[digitGroups])
        }
    }
}
