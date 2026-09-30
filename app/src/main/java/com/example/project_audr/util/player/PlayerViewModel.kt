package com.example.project_audr.util.player

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_audr.data.local.AppDatabase
import com.example.project_audr.data.local.AudioRecordEntity
import com.example.project_audr.data.repository.AudioRecordRepository
import com.example.project_audr.util.AudioPlayer
import com.example.project_audr.util.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val player = AudioPlayer(application)
    private var repository: AudioRecordRepository? = null

    private val _record = MutableStateFlow<AudioRecordEntity?>(null)
    val record: StateFlow<AudioRecordEntity?> = _record.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Состояние плеера (из AudioPlayer)
    val playerState: StateFlow<PlayerState> = player.state

    init {
        viewModelScope.launch {
            try {
                val dao = withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(getApplication()).audioRecordDao()
                }
                repository = AudioRecordRepository(dao)
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка инициализации БД"
            }
        }
    }

    // Загрузить запись по её ID
    fun loadRecord(recordId: Int) {
        viewModelScope.launch {
            try {
                val rec = withContext(Dispatchers.IO) {
                    repository?.getRecordById(recordId)
                }
                if (rec == null) {
                    _error.value = "Запись не найдена"
                    return@launch
                }

                _record.value = rec

                val file = File(rec.filePath)
                if (!file.exists()) {
                    _error.value = "Файл не найден: ${rec.filePath}"
                    return@launch
                }

                player.loadFile(file, viewModelScope)
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки: ${e.message}"
            }
        }
    }

    fun playPause() {
        if (playerState.value.isPlaying) {
            player.pause()
        } else {
            player.play(viewModelScope)
        }
    }

    fun seekTo(positionMs: Int) {
        player.seekTo(positionMs)
    }

    fun skipForward() {
        player.skip(5_000)
    }

    fun skipBackward() {
        player.skip(-5_000)
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val rec = _record.value ?: return@launch
            try {
                repository?.toggleFavorite(rec.id, !rec.isFavorite)
                _record.value = rec.copy(isFavorite = !rec.isFavorite)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun renameRecord(newTitle: String) {
        viewModelScope.launch {
            val rec = _record.value ?: return@launch
            try {
                val updated = rec.copy(title = newTitle)
                repository?.updateRecord(updated)
                _record.value = updated
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteRecord(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val rec = _record.value ?: return@launch
            try {
                repository?.deleteRecord(rec)
                File(rec.filePath).delete()
                onDeleted()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
    fun stopAndRelease() {
        player.release()
    }

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}