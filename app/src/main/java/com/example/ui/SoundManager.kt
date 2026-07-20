package com.example.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

object SoundManager {
    private val scope = CoroutineScope(Dispatchers.Default)

    // Synthesize a beautiful, modern, professional click sound (a super fast exponential sine wave decay)
    fun playClick() {
        scope.launch {
            try {
                val sampleRate = 44100
                val durationMs = 80
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = FloatArray(numSamples)

                // High-pass dynamic sweep from 1200Hz down to 600Hz for a modern premium click
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    // Exponential decay envelope
                    val envelope = kotlin.math.exp(-t * 80.0)
                    val frequency = 600.0 + 600.0 * envelope
                    buffer[i] = (sin(2.0 * Math.PI * frequency * t) * envelope * 0.15).toFloat()
                }

                playBuffer(buffer, sampleRate)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Synthesize a beautiful, warm, sci-fi chime (e.g. for success or completing a task)
    fun playSuccess() {
        scope.launch {
            try {
                val sampleRate = 44100
                val durationMs = 350
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = FloatArray(numSamples)

                // Two harmonized sine waves with slow decay
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = kotlin.math.exp(-t * 12.0)
                    // Harmonized frequencies (C5 and E5 / G5 for golden ratio harmony)
                    val f1 = 523.25 // C5
                    val f2 = 659.25 // E5
                    val f3 = 783.99 // G5
                    val wave = sin(2.0 * Math.PI * f1 * t) * 0.4 + 
                               sin(2.0 * Math.PI * f2 * t) * 0.3 + 
                               sin(2.0 * Math.PI * f3 * t) * 0.2
                    buffer[i] = (wave * envelope * 0.15).toFloat()
                }

                playBuffer(buffer, sampleRate)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Central function to play floating point audio buffer via AudioTrack
    private fun playBuffer(buffer: FloatArray, sampleRate: Int) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 4)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size, AudioTrack.WRITE_NON_BLOCKING)
            audioTrack.play()

            // Schedule release of track after playback ends
            scope.launch {
                delay(1000)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
