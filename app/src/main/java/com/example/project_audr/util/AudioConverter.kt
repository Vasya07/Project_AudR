package com.example.project_audr.util

import android.media.MediaPlayer
import android.util.Log
import java.io.File

object AudioConverter {

    private const val TAG = "AudioConverter"

    // Импорт аудиофайла
    fun importWithoutConversion(
        inputFile: File,
        outputDir: File
    ): File? {
        return try {
            if (!inputFile.exists()) {
                Log.e(TAG, "Файл не существует: ${inputFile.absolutePath}")
                return null
            }

            // Уникальное имя
            val baseName = inputFile.nameWithoutExtension
            val extension = inputFile.extension.ifBlank { "m4a" }
            var outputFile = File(outputDir, "$baseName.$extension")
            var counter = 1
            while (outputFile.exists()) {
                outputFile = File(outputDir, "$baseName($counter).$extension")
                counter++
            }

            // Копируем файл
            inputFile.copyTo(outputFile, overwrite = false)
            Log.d(TAG, "Файл скопирован: ${outputFile.absolutePath}")
            outputFile

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка копирования", e)
            null
        }
    }

    // Получить длительность аудиофайла в секундах
    fun getAudioDuration(file: File): Int {
        return try {
            val player = MediaPlayer()
            player.setDataSource(file.absolutePath)
            player.prepare()
            val duration = player.duration / 1000  // Из мс в сек
            player.release()
            duration
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения длительности", e)
            0
        }
    }
}