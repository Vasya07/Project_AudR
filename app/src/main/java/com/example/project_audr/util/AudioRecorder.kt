package com.example.project_audr.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTime: Long = 0

    val isRecording: Boolean
        get() = mediaRecorder != null

    // Начать запись
    fun startRecording(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "AUD_${timestamp}.m4a"
        val file = File(context.filesDir, fileName)

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44_100)
            setAudioEncodingBitRate(128_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        currentFile = file
        startTime = System.currentTimeMillis()
        return file
    }

    // Остановить запись
    fun stopRecording(): Pair<File, Long>? {
        val file = currentFile ?: return null
        val duration = (System.currentTimeMillis() - startTime) / 1000

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
        }

        return Pair(file, duration)
    }

    // Отменить запись и удалить файл
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            currentFile?.delete()
            currentFile = null
        }
    }

    // Получить длительность записи в секундах
    fun getCurrentDuration(): Long {
        return if (isRecording) {
            (System.currentTimeMillis() - startTime) / 1000
        } else {
            0
        }
    }
}