package com.example

import android.util.Log

/**
 * Global Configuration secrets for the application.
 * Values are injected at compile time via the Secrets Gradle Plugin from the .env file.
 */
object Config {
    private const val TAG = "Config"

    val SUPABASE_URL: String = try {
        BuildConfig.SUPABASE_URL
    } catch (e: Exception) {
        ""
    }

    val SUPABASE_ANON_KEY: String = try {
        BuildConfig.SUPABASE_ANON_KEY
    } catch (e: Exception) {
        ""
    }

    val SUPABASE_SERVICE_ROLE_KEY: String = try {
        BuildConfig.SUPABASE_SERVICE_ROLE_KEY
    } catch (e: Exception) {
        ""
    }

    val ELEVENLABS_API_KEY: String = try {
        BuildConfig.ELEVENLABS_API_KEY
    } catch (e: Exception) {
        ""
    }

    val OPENROUTER_API_KEY: String = try {
        val key = BuildConfig.OPENROUTER_API_KEY.trim()
        if (key.isEmpty() || key == "MY_OPENROUTER_API_KEY" || key == "placeholder" || key.startsWith("MY_")) {
            "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"
        } else {
            key
        }
    } catch (e: Exception) {
        "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"
    }

    private var customOpenRouterKey: String? = null
    private var customElevenLabsKey: String? = null
    private var customAppName: String? = null

    fun getOpenRouterKey(context: android.content.Context): String {
        if (customOpenRouterKey == null) {
            val prefs = context.getSharedPreferences("admin_prefs", android.content.Context.MODE_PRIVATE)
            customOpenRouterKey = prefs.getString("openrouter_api_key", OPENROUTER_API_KEY) ?: OPENROUTER_API_KEY
        }
        return customOpenRouterKey ?: OPENROUTER_API_KEY
    }

    fun setOpenRouterKey(context: android.content.Context, key: String) {
        val prefs = context.getSharedPreferences("admin_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("openrouter_api_key", key).apply()
        customOpenRouterKey = key
    }

    fun getElevenLabsKey(context: android.content.Context): String {
        if (customElevenLabsKey == null) {
            val prefs = context.getSharedPreferences("admin_prefs", android.content.Context.MODE_PRIVATE)
            customElevenLabsKey = prefs.getString("elevenlabs_api_key", ELEVENLABS_API_KEY) ?: ELEVENLABS_API_KEY
        }
        return (customElevenLabsKey ?: ELEVENLABS_API_KEY).trim()
    }

    fun setElevenLabsKey(context: android.content.Context, key: String) {
        val prefs = context.getSharedPreferences("admin_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("elevenlabs_api_key", key).apply()
        customElevenLabsKey = key
    }

    fun getAppName(context: android.content.Context): String {
        if (customAppName == null) {
            val prefs = context.getSharedPreferences("admin_prefs", android.content.Context.MODE_PRIVATE)
            customAppName = prefs.getString("app_name", "FOCUS MATE AI") ?: "FOCUS MATE AI"
        }
        return customAppName ?: "FOCUS MATE AI"
    }

    fun getAppNameWithoutContext(): String? {
        return customAppName
    }

    fun setAppName(context: android.content.Context, name: String) {
        val prefs = context.getSharedPreferences("admin_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("app_name", name).apply()
        customAppName = name
    }

    init {
        // Log status of keys on startup (without leaking the actual keys)
        Log.d(TAG, "SUPABASE_URL configured: ${SUPABASE_URL.isNotEmpty() && !SUPABASE_URL.contains("MY_")}")
        Log.d(TAG, "SUPABASE_ANON_KEY configured: ${SUPABASE_ANON_KEY.isNotEmpty() && !SUPABASE_ANON_KEY.contains("MY_")}")
        Log.d(TAG, "SUPABASE_SERVICE_ROLE_KEY configured: ${SUPABASE_SERVICE_ROLE_KEY.isNotEmpty() && !SUPABASE_SERVICE_ROLE_KEY.contains("MY_")}")
        Log.d(TAG, "ELEVENLABS_API_KEY configured: ${ELEVENLABS_API_KEY.isNotEmpty() && !ELEVENLABS_API_KEY.contains("MY_")}")
        Log.d(TAG, "OPENROUTER_API_KEY configured: ${OPENROUTER_API_KEY.isNotEmpty() && !OPENROUTER_API_KEY.contains("MY_")}")
    }

    /**
     * Helper to verify if all essential services are configured.
     */
    fun hasSupabaseConfig(): Boolean = SUPABASE_URL.isNotEmpty() && !SUPABASE_URL.contains("MY_")
    fun hasOpenRouterConfig(): Boolean {
        val k = OPENROUTER_API_KEY.trim()
        return k.isNotEmpty() && k != "MY_OPENROUTER_API_KEY" && k != "placeholder" && !k.startsWith("MY_")
    }
    fun hasElevenLabsConfig(context: android.content.Context? = null): Boolean {
        val key = if (context != null) getElevenLabsKey(context) else ELEVENLABS_API_KEY
        return key.isNotEmpty() && !key.contains("MY_") && key != "placeholder" && key.isNotBlank()
    }
}
