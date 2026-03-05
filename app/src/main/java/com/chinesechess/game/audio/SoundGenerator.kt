package com.chinesechess.game.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.sin

/**
 * Generates simple sound effect WAV files for the chess game.
 * These are created at first launch if the raw resources are missing.
 */
object SoundGenerator {

    fun ensureSoundsExist(context: Context) {
        val dir = File(context.filesDir, "sounds")
        if (!dir.exists()) dir.mkdirs()

        generateIfMissing(dir, "move.wav") { generateMoveSound() }
        generateIfMissing(dir, "capture.wav") { generateCaptureSound() }
        generateIfMissing(dir, "check.wav") { generateCheckSound() }
        generateIfMissing(dir, "win.wav") { generateWinSound() }
        generateIfMissing(dir, "select.wav") { generateSelectSound() }
    }

    private fun generateIfMissing(dir: File, name: String, generator: () -> ByteArray) {
        val file = File(dir, name)
        if (!file.exists()) {
            val pcm = generator()
            writeWav(file, pcm, 44100)
        }
    }

    private fun generateMoveSound(): ByteArray {
        // Short click sound
        return generateTone(800.0, 0.08, 0.6) + generateTone(600.0, 0.04, 0.3)
    }

    private fun generateCaptureSound(): ByteArray {
        // Impact sound
        return generateTone(400.0, 0.05, 0.8) + generateTone(300.0, 0.1, 0.6) + generateTone(200.0, 0.05, 0.3)
    }

    private fun generateCheckSound(): ByteArray {
        // Alert sound
        return generateTone(1000.0, 0.1, 0.7) + generateTone(800.0, 0.05, 0.3) + generateTone(1200.0, 0.15, 0.8)
    }

    private fun generateWinSound(): ByteArray {
        // Victory fanfare
        return generateTone(523.0, 0.15, 0.7) + generateTone(659.0, 0.15, 0.7) +
                generateTone(784.0, 0.15, 0.7) + generateTone(1047.0, 0.3, 0.9)
    }

    private fun generateSelectSound(): ByteArray {
        // Soft click
        return generateTone(1200.0, 0.03, 0.4)
    }

    private fun generateTone(freq: Double, durationSec: Double, volume: Double): ByteArray {
        val sampleRate = 44100
        val numSamples = (durationSec * sampleRate).toInt()
        val samples = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = if (i < numSamples / 10) i.toDouble() / (numSamples / 10)
            else (numSamples - i).toDouble() / (numSamples * 0.7)
            val clampedEnvelope = envelope.coerceIn(0.0, 1.0)
            val sample = (sin(2.0 * PI * freq * t) * volume * clampedEnvelope * Short.MAX_VALUE).toInt().toShort()
            samples[i * 2] = (sample.toInt() and 0xff).toByte()
            samples[i * 2 + 1] = (sample.toInt() shr 8 and 0xff).toByte()
        }
        return samples
    }

    private fun writeWav(file: File, pcmData: ByteArray, sampleRate: Int) {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcmData.size
        val fileSize = 36 + dataSize

        FileOutputStream(file).use { fos ->
            // RIFF header
            fos.write("RIFF".toByteArray())
            fos.write(intToBytes(fileSize))
            fos.write("WAVE".toByteArray())
            // fmt chunk
            fos.write("fmt ".toByteArray())
            fos.write(intToBytes(16))
            fos.write(shortToBytes(1)) // PCM
            fos.write(shortToBytes(channels))
            fos.write(intToBytes(sampleRate))
            fos.write(intToBytes(byteRate))
            fos.write(shortToBytes(blockAlign))
            fos.write(shortToBytes(bitsPerSample))
            // data chunk
            fos.write("data".toByteArray())
            fos.write(intToBytes(dataSize))
            fos.write(pcmData)
        }
    }

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xff).toByte(),
            (value shr 8 and 0xff).toByte(),
            (value shr 16 and 0xff).toByte(),
            (value shr 24 and 0xff).toByte()
        )
    }

    private fun shortToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xff).toByte(),
            (value shr 8 and 0xff).toByte()
        )
    }
}
