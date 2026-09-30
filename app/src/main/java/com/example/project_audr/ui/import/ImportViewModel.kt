package com.example.project_audr.ui.import

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_audr.data.local.AppDatabase
import com.example.project_audr.data.local.AudioRecordEntity
import com.example.project_audr.data.repository.AudioRecordRepository
import com.example.project_audr.util.AudioConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ImportUiState(
    val isProcessing: Boolean = false,
    val progress: Int = 0,
    val totalFiles: Int = 0,
    val processedFiles: Int = 0,
    val error: String? = null,
    val successCount: Int = 0,
    val done: Boolean = false
)

class ImportViewModel(application: Application) : AndroidViewModel(application) {

    private var repository: AudioRecordRepository? = null

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val dao = withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(getApplication()).audioRecordDao()
                }
                repository = AudioRecordRepository(dao)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun importFiles(uris: List<Uri>, convertToMono: Boolean = false) {
        if (uris.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = ImportUiState(
                isProcessing = true,
                totalFiles = uris.size,
                processedFiles = 0
            )

            var successCount = 0

            for ((index, uri) in uris.withIndex()) {
                try {
                    _uiState.value = _uiState.value.copy(
                        progress = (index * 100) / uris.size,
                        processedFiles = index
                    )

                    val success = importSingleFile(uri)
                    if (success) successCount++

                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = "Ошибка импорта: ${e.message}"
                    )
                }
            }

            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                progress = 100,
                processedFiles = uris.size,
                successCount = successCount,
                done = true
            )
        }
    }

    private suspend fun importSingleFile(uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val fileName = getFileName(uri) ?: "imported_${System.currentTimeMillis()}"
                val tempFile = File(context.cacheDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val outputFile = AudioConverter.importWithoutConversion(
                    inputFile = tempFile,
                    outputDir = context.filesDir
                )

                tempFile.delete()

                if (outputFile == null) {
                    return@withContext false
                }

                val duration = AudioConverter.getAudioDuration(outputFile)

                val title = outputFile.nameWithoutExtension
                val entity = AudioRecordEntity(
                    title = title,
                    filePath = outputFile.absolutePath,
                    duration = duration,
                    dateCreated = System.currentTimeMillis(),
                    fileSize = outputFile.length(),
                    isImported = true,
                    isFavorite = false
                )

                repository?.insertRecord(entity)
                true

            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        val context = getApplication<Application>()
        var name: String? = null

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
            }
        }

        return name
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun reset() {
        _uiState.value = ImportUiState()
    }
}