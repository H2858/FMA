package com.example.data

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.example.Config
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

// --- ELEVENLABS REQUEST MODELS ---

data class ElevenLabsVoiceSettings(
    val stability: Float = 0.5f,
    val similarity_boost: Float = 0.75f
)

data class ElevenLabsRequest(
    val text: String,
    val model_id: String = "eleven_multilingual_v2",
    val voice_settings: ElevenLabsVoiceSettings = ElevenLabsVoiceSettings()
)

// --- RETROFIT INTERFACE ---

interface ElevenLabsApi {
    @POST("v1/text-to-speech/{voice_id}")
    suspend fun textToSpeech(
        @Path("voice_id") voiceId: String,
        @Header("xi-api-key") apiKey: String,
        @Body request: ElevenLabsRequest
    ): ResponseBody
}

class ElevenLabsService(private val context: Context) {
    private val TAG = "ElevenLabsService"
    private var api: ElevenLabsApi? = null
    private var mediaPlayer: MediaPlayer? = null

    // Default voice IDs for the Coach personas
    companion object {
        const val SPARTAN_VOICE = "pNInz6obpg75FLg73gww" // Adam (Deep male)
        const val MENTOR_VOICE = "IKne3meq5aSn9XLyUdCD"  // Charlie (Calm older male)
        const val PARTNER_VOICE = "EXAVITQu4vr4xnSDxMaL" // Bella (Friendly female)
    }

    init {
        initializeApi()
    }

    private fun initializeApi() {
        try {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.elevenlabs.io/")
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            api = retrofit.create(ElevenLabsApi::class.java)
            Log.i(TAG, "ElevenLabs API Client initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ElevenLabs API Client: ${e.message}")
        }
    }

    /**
     * Synthesizes text to speech using ElevenLabs and plays the resulting audio.
     */
    suspend fun synthesizeAndPlay(text: String, persona: GeminiApiService.CoachPersona, onPlaybackComplete: () -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        val currentApi = api
        val apiKey = Config.getElevenLabsKey(context)
        val voiceId = when (persona) {
            GeminiApiService.CoachPersona.SPARTAN -> SPARTAN_VOICE
            GeminiApiService.CoachPersona.MENTOR -> MENTOR_VOICE
            GeminiApiService.CoachPersona.PARTNER -> PARTNER_VOICE
            GeminiApiService.CoachPersona.GENERAL -> MENTOR_VOICE
        }

        if (currentApi == null || apiKey.isEmpty()) {
            Log.w(TAG, "ElevenLabs is unconfigured or unavailable. Fallback: text speech skipped.")
            return@withContext false
        }

        // Clean text to avoid speech synthesis noise
        val cleanText = text.replace(Regex("[*#_`~]"), "")

        try {
            stopPlayback() // Stop any running speech first

            val request = ElevenLabsRequest(text = cleanText)
            val responseBody = currentApi.textToSpeech(voiceId, apiKey, request)

            // Save response stream to cache directory
            val tempFile = File(context.cacheDir, "coach_speech.mp3")
            if (tempFile.exists()) tempFile.delete()

            responseBody.byteStream().use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Play the file using MediaPlayer on the Main Thread
            withContext(Dispatchers.Main) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        onPlaybackComplete()
                        it.release()
                        mediaPlayer = null
                    }
                    setOnErrorListener { mp, _, _ ->
                        onPlaybackComplete()
                        mp.release()
                        mediaPlayer = null
                        true
                    }
                }
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "ElevenLabs speech synthesis failure: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Synthesizes text to speech using ElevenLabs and a specific voice ID.
     */
    suspend fun synthesizeAndPlayCustomVoice(text: String, voiceId: String, onPlaybackComplete: () -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        val currentApi = api
        val apiKey = Config.getElevenLabsKey(context)

        if (currentApi == null || apiKey.isEmpty()) {
            Log.w(TAG, "ElevenLabs is unconfigured or unavailable. Fallback: text speech skipped.")
            return@withContext false
        }

        // Clean text to avoid speech synthesis noise
        val cleanText = text.replace(Regex("[*#_`~]"), "")

        try {
            stopPlayback() // Stop any running speech first

            val request = ElevenLabsRequest(text = cleanText)
            val responseBody = currentApi.textToSpeech(voiceId, apiKey, request)

            // Save response stream to cache directory
            val tempFile = File(context.cacheDir, "coach_speech.mp3")
            if (tempFile.exists()) tempFile.delete()

            responseBody.byteStream().use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Play the file using MediaPlayer on the Main Thread
            withContext(Dispatchers.Main) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        onPlaybackComplete()
                        it.release()
                        mediaPlayer = null
                    }
                    setOnErrorListener { mp, _, _ ->
                        onPlaybackComplete()
                        mp.release()
                        mediaPlayer = null
                        true
                    }
                }
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "ElevenLabs custom voice speech synthesis failure: ${e.message}")
            return@withContext false
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player: ${e.message}")
        }
    }
}
