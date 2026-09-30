package com.example.project_audr.util

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val isPrepared: Boolean = false,
    val error: String? = null
)

class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    // Загрузить файл для воспроизведения
    fun loadFile(file: File, scope: CoroutineScope) {
        release()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        currentPositionMs = duration
                    )
                    stopProgressUpdates()
                }
                setOnErrorListener { _, what, extra ->
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        error = "Ошибка MediaPlayer: what=$what, extra=$extra"
                    )
                    true
                }
            }

            _state.value = PlayerState(
                isPrepared = true,
                durationMs = mediaPlayer?.duration ?: 0
            )
        } catch (e: Exception) {
            _state.value = PlayerState(
                error = "Ошибка загрузки: ${e.message}"
            )
        }
    }

    // Воспроизвести (продолжить)
    fun play(scope: CoroutineScope) {
        val player = mediaPlayer ?: return
        if (!_state.value.isPrepared) return

        try {
            if (!player.isPlaying) {
                player.start()
                _state.value = _state.value.copy(isPlaying = true, error = null)
                startProgressUpdates(scope)
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = "Ошибка воспроизведения: ${e.message}")
        }
    }

    // Пауза
    fun pause() {
        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
                _state.value = _state.value.copy(isPlaying = false)
                stopProgressUpdates()
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = "Ошибка паузы: ${e.message}")
        }
    }

    // Перемотка на позицию в записи
    fun seekTo(positionMs: Int) {
        val player = mediaPlayer ?: return
        try {
            player.seekTo(positionMs)
            _state.value = _state.value.copy(currentPositionMs = positionMs)
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = "Ошибка перемотки: ${e.message}")
        }
    }

    // Перемотка записи
    fun skip(offsetMs: Int) {
        val player = mediaPlayer ?: return
        val newPos = (player.currentPosition + offsetMs)
            .coerceIn(0, _state.value.durationMs)
        seekTo(newPos)
    }

    fun release() {
        stopProgressUpdates()
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
            _state.value = PlayerState()
        }
    }

    private fun startProgressUpdates(scope: CoroutineScope) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                delay(500.milliseconds)
                mediaPlayer?.let { player ->
                    try {
                        _state.value = _state.value.copy(
                            currentPositionMs = player.currentPosition
                        )
                    } catch (e: Exception) {
                        // MediaPlayer может быть освобождён
                    }
                }
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }
}