package com.example.project_audr.util.record

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_audr.data.local.AppDatabase
import com.example.project_audr.data.local.AudioRecordEntity
import com.example.project_audr.data.repository.AudioRecordRepository
import com.example.project_audr.util.AudioRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class RecordUiState(
    val isRecording: Boolean = false,
    val durationSeconds: Int = 0,
    val error: String? = null,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val pendingFile: File? = null,
    val pendingDuration: Int = 0
)

class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val recorder = AudioRecorder(application)
    private var repository: AudioRecordRepository? = null
    private var timerJob: Job? = null

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val dao = withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(getApplication()).audioRecordDao()
                }
                repository = AudioRecordRepository(dao)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Ошибка инициализации БД"
                )
            }
        }
    }

    fun startRecording() {
        try {
            _uiState.value = RecordUiState(isRecording = true)
            recorder.startRecording()
            _uiState.value = _uiState.value.copy(
                isRecording = true,
                durationSeconds = 0,
                error = null
            )
            startTimer()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Ошибка начала записи: ${e.message}"
            )
        }
    }

    fun stopRecording() {
        try {
            val result = recorder.stopRecording()
            stopTimer()

            if (result == null) {
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    error = "Файл записи не найден"
                )
                return
            }

            val (file, duration) = result
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                durationSeconds = duration.toInt(),
                pendingFile = file,
                pendingDuration = duration.toInt()
            )

        } catch (e: Exception) {
            stopTimer()
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                error = "Ошибка остановки записи: ${e.message}"
            )
        }
    }

    fun saveRecord(file: File, durationSeconds: Long, customTitle: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            try {
                val title = customTitle?.takeIf { it.isNotBlank() }
                    ?: "Запись от ${
                        java.text.SimpleDateFormat(
                            "dd.MM.yyyy HH:mm:ss",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date())
                    }"

                val entity = AudioRecordEntity(
                    title = title,
                    filePath = file.absolutePath,
                    duration = durationSeconds.toInt(),
                    dateCreated = System.currentTimeMillis(),
                    fileSize = file.length(),
                    isImported = false,
                    isFavorite = false,
                    bitrate = 128_000,
                    sampleRate = 44_100
                )

                withContext(Dispatchers.IO) {
                    repository?.insertRecord(entity)
                }
                _uiState.value = RecordUiState(savedSuccessfully = true)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Ошибка сохранения: ${e.message}"
                )
            }
        }
    }

    fun cancelRecording() {
        recorder.cancelRecording()
        stopTimer()
        _uiState.value = RecordUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetSavedFlag() {
        _uiState.value = _uiState.value.copy(savedSuccessfully = false)
    }

    fun cancelPendingRecord() {
        _uiState.value.pendingFile?.delete()
        _uiState.value = RecordUiState()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    durationSeconds = recorder.getCurrentDuration().toInt()
                )
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        if (recorder.isRecording) {
            recorder.cancelRecording()
        }
        stopTimer()
    }
}