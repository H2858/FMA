package com.example.data

import android.content.Context
import android.util.Log
import com.example.Config
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.UUID

// --- SUPABASE API MODELS ---

data class AuthRequest(
    val email: String,
    val password: String,
    val data: Map<String, String>? = null
)

data class AuthUser(
    val id: String,
    val email: String?
)

data class AuthSession(
    val access_token: String,
    val token_type: String,
    val expires_in: Long,
    val user: AuthUser
)

data class AuthResponse(
    val access_token: String?,
    val user: AuthUser?
)

data class SupabaseTaskDto(
    val id: String,
    val user_id: String,
    val title: String,
    val description: String,
    val is_completed: Boolean,
    val updated_at: Long
)

data class SupabaseProfileDto(
    val id: String,
    val name: String,
    val email: String,
    val tier: String,
    val persona: String,
    val avatar_index: Int? = 0,
    val xp_points: Int? = 150,
    val plan_type: String? = "Monthly"
)

data class IdTokenRequest(
    val provider: String = "google",
    val id_token: String
)

// --- RETROFIT INTERFACE ---

interface SupabaseApi {
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Body request: AuthRequest
    ): AuthSession

    @POST("auth/v1/token")
    suspend fun signIn(
        @Header("apikey") apiKey: String,
        @Query("grant_type") grantType: String = "password",
        @Body request: AuthRequest
    ): AuthSession

    @POST("auth/v1/token")
    suspend fun signInWithIdToken(
        @Header("apikey") apiKey: String,
        @Query("grant_type") grantType: String = "id_token",
        @Body request: IdTokenRequest
    ): AuthSession

    @GET("rest/v1/tasks")
    suspend fun getTasks(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("user_id") userIdFilter: String? = null
    ): List<SupabaseTaskDto>

    @POST("rest/v1/tasks")
    suspend fun upsertTasks(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body tasks: List<SupabaseTaskDto>
    ): List<SupabaseTaskDto>

    @DELETE("rest/v1/tasks")
    suspend fun deleteTask(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String
    ): retrofit2.Response<Unit>

    @GET("rest/v1/profiles")
    suspend fun getProfiles(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String? = null
    ): List<SupabaseProfileDto>

    @POST("rest/v1/profiles")
    suspend fun upsertProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body profile: List<SupabaseProfileDto>
    ): List<SupabaseProfileDto>

    @GET("rest/v1/admin")
    suspend fun checkAdminStatus(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String
    ): List<Map<String, String>>

    @GET("rest/v1/admin")
    suspend fun checkAdminStatusByEmail(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("email") emailFilter: String
    ): List<Map<String, String>>
}

// --- SUPABASE CLIENT SERVICE ---

class SupabaseClient(private val context: Context, private val database: AppDatabase) {
    private val TAG = "SupabaseClient"
    private val prefs = context.getSharedPreferences("user_session_prefs", Context.MODE_PRIVATE)

    // Authentication session state observable by viewmodels
    private val _sessionState = MutableStateFlow<AuthSession?>(null)
    val sessionState: StateFlow<AuthSession?> = _sessionState

    // Live mode vs local offline fallback flag
    private val _isLiveMode = MutableStateFlow(false)
    val isLiveMode: StateFlow<Boolean> = _isLiveMode

    private var api: SupabaseApi? = null

    init {
        initializeClient()
        restoreSession()
    }

    private fun saveSession(session: AuthSession) {
        prefs.edit().apply {
            putString("access_token", session.access_token)
            putString("token_type", session.token_type)
            putLong("expires_in", session.expires_in)
            putString("user_id", session.user.id)
            putString("user_email", session.user.email)
            apply()
        }
    }

    private fun clearSession() {
        prefs.edit().clear().apply()
    }

    private fun restoreSession() {
        val accessToken = prefs.getString("access_token", null)
        val userId = prefs.getString("user_id", null)
        val userEmail = prefs.getString("user_email", null)
        if (accessToken != null && userId != null && userEmail != null) {
            val session = AuthSession(
                access_token = accessToken,
                token_type = prefs.getString("token_type", "bearer") ?: "bearer",
                expires_in = prefs.getLong("expires_in", 3600),
                user = AuthUser(id = userId, email = userEmail)
            )
            _sessionState.value = session
        }
    }

    private fun initializeClient() {
        var url = Config.SUPABASE_URL.trim()
        if (url.endsWith("/rest/v1/")) {
            url = url.substringBefore("/rest/v1/")
        } else if (url.endsWith("/rest/v1")) {
            url = url.substringBefore("/rest/v1")
        }
        val anonKey = Config.SUPABASE_ANON_KEY

        if (Config.hasSupabaseConfig()) {
            try {
                // Prepend protocol if needed, though Config should contain the full URL
                val baseUrl = if (url.endsWith("/")) url else "$url/"

                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }

                val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()

                api = retrofit.create(SupabaseApi::class.java)
                _isLiveMode.value = true
                Log.i(TAG, "Supabase Client initialized successfully in Live Mode")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Supabase Client, falling back to Local Mode: ${e.message}")
                _isLiveMode.value = false
            }
        } else {
            Log.w(TAG, "Supabase URL/Key missing in secrets. Falling back to local offline sandbox.")
            _isLiveMode.value = false
        }
    }

    // --- AUTHENTICATION FLOW ---

    suspend fun signUp(email: String, password: String, name: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        val currentApi = api
        val anonKey = Config.SUPABASE_ANON_KEY

        if (!_isLiveMode.value || currentApi == null || anonKey.isEmpty()) {
            // Local sandbox sign up fallback
            val dummyId = UUID.randomUUID().toString()
            val session = AuthSession(
                access_token = "local_sandbox_token_${dummyId}",
                token_type = "bearer",
                expires_in = 3600,
                user = AuthUser(id = dummyId, email = email)
            )
            val localProfile = ProfileEntity(id = dummyId, name = name, email = email, tier = "Free")
            database.profileDao().saveProfile(localProfile)
            _sessionState.value = session
            saveSession(session)
            return@withContext Result.success(session)
        }

        try {
            val signupData = mapOf("name" to name)
            val session = currentApi.signUp(anonKey, AuthRequest(email, password, signupData))
            
            // Sync user profile to Supabase profiles table
            val profileDto = SupabaseProfileDto(
                id = session.user.id,
                name = name,
                email = email,
                tier = "Free",
                persona = "Mentor",
                avatar_index = 0,
                xp_points = 150,
                plan_type = "Monthly"
            )
            try {
                currentApi.upsertProfile(anonKey, "Bearer ${session.access_token}", "resolution=merge-duplicates", listOf(profileDto))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upsert cloud profile during signup: ${e.message}")
            }

            // Save local profile cache
            database.profileDao().saveProfile(ProfileEntity(id = session.user.id, name = name, email = email, tier = "Free"))
            
            _sessionState.value = session
            saveSession(session)
            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Supabase Sign Up Error: ${e.message}")
            if (_isLiveMode.value) {
                Result.failure(Exception(getErrorMessageFromException(e)))
            } else {
                // Elegant offline fallback so user is never blocked in local mode
                val dummyId = UUID.randomUUID().toString()
                val session = AuthSession(
                    access_token = "local_sandbox_token_${dummyId}",
                    token_type = "bearer",
                    expires_in = 3600,
                    user = AuthUser(id = dummyId, email = email)
                )
                val localProfile = ProfileEntity(id = dummyId, name = name, email = email, tier = "Free")
                database.profileDao().saveProfile(localProfile)
                _sessionState.value = session
                saveSession(session)
                Result.success(session)
            }
        }
    }

    suspend fun signIn(email: String, password: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        val currentApi = api
        val anonKey = Config.SUPABASE_ANON_KEY

        if (!_isLiveMode.value || currentApi == null || anonKey.isEmpty()) {
            // Local sandbox login fallback
            val cachedProfile = database.profileDao().getProfileByEmailOnce(email)
            val uid = cachedProfile?.id ?: UUID.randomUUID().toString()
            val uName = cachedProfile?.name ?: email.substringBefore("@")
            val session = AuthSession(
                access_token = "local_sandbox_token_$uid",
                token_type = "bearer",
                expires_in = 3600,
                user = AuthUser(id = uid, email = email)
            )
            if (cachedProfile == null) {
                database.profileDao().saveProfile(ProfileEntity(id = uid, name = uName, email = email, tier = "Free"))
            }
            _sessionState.value = session
            saveSession(session)
            return@withContext Result.success(session)
        }

        try {
            val session = currentApi.signIn(anonKey, "password", AuthRequest(email, password))
            
            // Sync / retrieve profile
            val profiles = currentApi.getProfiles(anonKey, "Bearer ${session.access_token}", "eq.${session.user.id}")
            if (profiles.isNotEmpty()) {
                val p = profiles.first()
                database.profileDao().saveProfile(
                    ProfileEntity(
                        id = p.id,
                        name = p.name,
                        email = p.email,
                        tier = p.tier,
                        persona = p.persona,
                        avatarIndex = p.avatar_index ?: 0,
                        xpPoints = p.xp_points ?: 150,
                        isVerified = (p.tier == "Premium"),
                        planType = p.plan_type ?: "Monthly"
                    )
                )
            } else {
                // If profile missing in DB, create it
                val defaultProfile = SupabaseProfileDto(
                    id = session.user.id,
                    name = email.substringBefore("@"),
                    email = email,
                    tier = "Free",
                    persona = "Mentor",
                    avatar_index = 0,
                    xp_points = 150,
                    plan_type = "Monthly"
                )
                currentApi.upsertProfile(anonKey, "Bearer ${session.access_token}", "resolution=merge-duplicates", listOf(defaultProfile))
                database.profileDao().saveProfile(
                    ProfileEntity(
                        id = defaultProfile.id,
                        name = defaultProfile.name,
                        email = defaultProfile.email,
                        tier = "Free",
                        persona = "Mentor",
                        avatarIndex = 0,
                        xpPoints = 150,
                        planType = "Monthly"
                    )
                )
            }

            _sessionState.value = session
            saveSession(session)
            // Sync tasks
            syncTasksWithCloud(session)
            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Supabase Sign In Error: ${e.message}")
            if (_isLiveMode.value) {
                Result.failure(Exception(getErrorMessageFromException(e)))
            } else {
                // Fallback to local sandbox session on network error or wrong credentials in offline-first
                val cachedProfile = database.profileDao().getProfileByEmailOnce(email)
                val uid = cachedProfile?.id ?: UUID.randomUUID().toString()
                val uName = cachedProfile?.name ?: email.substringBefore("@")
                val session = AuthSession(
                    access_token = "local_sandbox_token_$uid",
                    token_type = "bearer",
                    expires_in = 3600,
                    user = AuthUser(id = uid, email = email)
                )
                if (cachedProfile == null) {
                    database.profileDao().saveProfile(ProfileEntity(id = uid, name = uName, email = email, tier = "Free"))
                }
                _sessionState.value = session
                saveSession(session)
                Result.success(session)
            }
        }
    }

    private fun getErrorMessageFromException(e: Exception): String {
        return if (e is retrofit2.HttpException) {
            try {
                val errorBody = e.response()?.errorBody()?.string() ?: ""
                val descRegex = """"error_description"\s*:\s*"([^"]+)"""".toRegex()
                val msgRegex = """"message"\s*:\s*"([^"]+)"""".toRegex()
                
                descRegex.find(errorBody)?.groupValues?.get(1)
                    ?: msgRegex.find(errorBody)?.groupValues?.get(1)
                    ?: when (e.code()) {
                        400 -> "Invalid login credentials or request format."
                        401 -> "Invalid credentials. Please verify your email and password."
                        422 -> "Unprocessable Entity. The email format might be invalid."
                        else -> "Server error (${e.code()}). Please try again."
                    }
            } catch (ex: Exception) {
                "Authentication error: ${e.message}"
            }
        } else {
            "Connection failed: Please verify your internet connection and Supabase URL."
        }
    }

    suspend fun loginWithGoogle(idToken: String, email: String, name: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        val currentApi = api
        val anonKey = Config.SUPABASE_ANON_KEY
        
        if (idToken == "biometric_autologin_token") {
            val userId = UUID.nameUUIDFromBytes(email.toByteArray()).toString()
            val existing = database.profileDao().getProfileByEmailOnce(email) ?: database.profileDao().getProfileOnce()
            val finalUserId = existing?.id ?: userId
            val session = AuthSession(
                access_token = "biometric_token_$finalUserId",
                token_type = "bearer",
                expires_in = 3600,
                user = AuthUser(id = finalUserId, email = existing?.email ?: email)
            )
            val localProfile = existing?.copy(isBiometricEnabled = true) ?: ProfileEntity(
                id = finalUserId,
                name = name,
                email = email,
                tier = "Free",
                isBiometricEnabled = true
            )
            database.profileDao().saveProfile(localProfile)
            
            _sessionState.value = session
            saveSession(session)
            return@withContext Result.success(session)
        }

        if (_isLiveMode.value && currentApi != null && anonKey.isNotEmpty()) {
            try {
                val session = currentApi.signInWithIdToken(
                    apiKey = anonKey,
                    request = IdTokenRequest(id_token = idToken)
                )
                val userId = session.user.id
                val userEmail = session.user.email ?: email
                
                // Save to local DB cache
                val localProfile = ProfileEntity(id = userId, name = name, email = userEmail, tier = "Free")
                database.profileDao().saveProfile(localProfile)
                
                // Sync profile in Supabase to keep them in sync
                try {
                    val profileDto = SupabaseProfileDto(
                        id = userId,
                        name = name,
                        email = userEmail,
                        tier = "Free",
                        persona = "Mentor",
                        avatar_index = 0,
                        xp_points = 150,
                        plan_type = "Monthly"
                    )
                    currentApi.upsertProfile(anonKey, "Bearer ${session.access_token}", "resolution=merge-duplicates", listOf(profileDto))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync Google profile to Supabase: ${e.message}")
                }
                
                _sessionState.value = session
                saveSession(session)
                Result.success(session)
            } catch (e: Exception) {
                Log.e(TAG, "Failed real Supabase Google Sign-In: ${e.message}")
                Result.failure(Exception(getErrorMessageFromException(e)))
            }
        } else {
            // Local sandbox mode
            val userId = UUID.nameUUIDFromBytes(email.toByteArray()).toString()
            val session = AuthSession(
                access_token = "google_token_$userId",
                token_type = "bearer",
                expires_in = 3600,
                user = AuthUser(id = userId, email = email)
            )
            
            // Save to local DB cache
            val localProfile = ProfileEntity(id = userId, name = name, email = email, tier = "Free")
            database.profileDao().saveProfile(localProfile)
            
            _sessionState.value = session
            saveSession(session)
            Result.success(session)
        }
    }

    fun signOut() {
        _sessionState.value = null
        clearSession()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                database.clearAllTables()
                Log.i(TAG, "Local database cleared on sign out for absolute privacy.")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing local database on sign out: ${e.message}")
            }
        }
    }

    /**
     * Checks if the user is registered in the 'admin' table in Supabase.
     */
    suspend fun checkIfUserIsAdmin(userId: String, email: String): Boolean = withContext(Dispatchers.IO) {
        val currentApi = api
        val anonKey = Config.SUPABASE_ANON_KEY
        val session = _sessionState.value

        if (!_isLiveMode.value || currentApi == null || anonKey.isEmpty() || session == null) {
            // Local fallback in sandbox/offline mode
            return@withContext email == "lovablehibo@gmail.com" || email.contains("admin")
        }

        try {
            // 1. Try checking by ID in the admin table
            val response = currentApi.checkAdminStatus(anonKey, "Bearer ${session.access_token}", "eq.$userId")
            if (response.isNotEmpty()) {
                return@withContext true
            }
            // 2. Try checking by email in the admin table
            val responseEmail = currentApi.checkAdminStatusByEmail(anonKey, "Bearer ${session.access_token}", "eq.$email")
            if (responseEmail.isNotEmpty()) {
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking admin status in Supabase: ${e.message}")
        }
        return@withContext false
    }

    // --- DATABASE SYNC FLOW ---

    suspend fun syncTasksWithCloud(session: AuthSession): Unit = withContext(Dispatchers.IO) {
        val currentApi = api ?: return@withContext
        val anonKey = Config.SUPABASE_ANON_KEY

        try {
            // 1. Fetch remote tasks
            val remoteTasks = currentApi.getTasks(anonKey, "Bearer ${session.access_token}", "eq.${session.user.id}")
            val dbTasks = remoteTasks.map {
                TaskEntity(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    isCompleted = it.is_completed,
                    updatedAt = it.updated_at,
                    synced = true
                )
            }
            database.taskDao().insertTasks(dbTasks)

            // 2. Upload any unsynced local tasks
            val unsynced = database.taskDao().getUnsyncedTasks()
            if (unsynced.isNotEmpty()) {
                val dtos = unsynced.map {
                    SupabaseTaskDto(
                        id = it.id,
                        user_id = session.user.id,
                        title = it.title,
                        description = it.description,
                        is_completed = it.isCompleted,
                        updated_at = it.updatedAt
                    )
                }
                val uploaded = currentApi.upsertTasks(anonKey, "Bearer ${session.access_token}", "resolution=merge-duplicates", dtos)
                val syncedLocal = unsynced.map { it.copy(synced = true) }
                database.taskDao().insertTasks(syncedLocal)
                Log.i(TAG, "Successfully synced ${uploaded.size} local tasks to cloud.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Task sync error: ${e.message}")
        }
    }

    suspend fun saveTask(title: String, description: String, isCompleted: Boolean = false, id: String? = null, deadlineTime: Long = 0L, durationText: String = ""): TaskEntity = withContext(Dispatchers.IO) {
        val taskId = id ?: UUID.randomUUID().toString()
        val session = _sessionState.value
        val userId = session?.user?.id ?: "local_user"

        val task = TaskEntity(
            id = taskId,
            title = title,
            description = description,
            isCompleted = isCompleted,
            updatedAt = System.currentTimeMillis(),
            synced = false,
            deadlineTime = deadlineTime,
            durationText = durationText
        )
        database.taskDao().insertTask(task)

        // Attempt cloud save
        val currentApi = api
        val anonKey = Config.SUPABASE_ANON_KEY
        if (session != null && currentApi != null && anonKey.isNotEmpty()) {
            try {
                val dto = SupabaseTaskDto(
                    id = task.id,
                    user_id = userId,
                    title = task.title,
                    description = task.description,
                    is_completed = task.isCompleted,
                    updated_at = task.updatedAt
                )
                currentApi.upsertTasks(anonKey, "Bearer ${session.access_token}", "resolution=merge-duplicates", listOf(dto))
                database.taskDao().insertTask(task.copy(synced = true))
                Log.d(TAG, "Task synced to Supabase instantly.")
            } catch (e: Exception) {
                Log.e(TAG, "Task saved locally only. Sync failed: ${e.message}")
            }
        }
        return@withContext task
    }

    suspend fun deleteTask(id: String) = withContext(Dispatchers.IO) {
        database.taskDao().deleteTaskById(id)

        val session = _sessionState.value
        val currentApi = api
        val anonKey = Config.SUPABASE_ANON_KEY
        if (session != null && currentApi != null && anonKey.isNotEmpty()) {
            try {
                currentApi.deleteTask(anonKey, "Bearer ${session.access_token}", "eq.$id")
                Log.d(TAG, "Task deleted from Supabase.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete task from cloud: ${e.message}")
            }
        }
    }

    suspend fun updateProfileTier(tier: String, planType: String? = null) = withContext(Dispatchers.IO) {
        val session = _sessionState.value
        val profile = if (session != null) {
            database.profileDao().getProfileByIdOnce(session.user.id)
        } else {
            database.profileDao().getProfileOnce()
        } ?: return@withContext

        val isVerifiedValue = (tier == "Premium")
        val updated = profile.copy(tier = tier, planType = planType ?: profile.planType, isVerified = isVerifiedValue)
        database.profileDao().saveProfile(updated)

        val currentApi = api
        val anonKey = Config.SUPABASE_ANON_KEY
        if (session != null && currentApi != null && anonKey.isNotEmpty()) {
            try {
                val dto = SupabaseProfileDto(
                    id = updated.id,
                    name = updated.name,
                    email = updated.email,
                    tier = updated.tier,
                    persona = updated.persona,
                    avatar_index = updated.avatarIndex,
                    xp_points = updated.xpPoints,
                    plan_type = updated.planType
                )
                currentApi.upsertProfile(anonKey, "Bearer ${session.access_token}", "resolution=merge-duplicates", listOf(dto))
            } catch (e: Exception) {
                Log.e(TAG, "Cloud profile tier update failed: ${e.message}")
            }
        }
    }

    suspend fun updateProfilePersona(persona: String) = withContext(Dispatchers.IO) {
        val session = _sessionState.value
        val profile = if (session != null) {
            database.profileDao().getProfileByIdOnce(session.user.id)
        } else {
            database.profileDao().getProfileOnce()
        } ?: return@withContext

        val updated = profile.copy(persona = persona)
        database.profileDao().saveProfile(updated)

        val currentApi = api
        val anonKey = Config.SUPABASE_ANON_KEY
        if (session != null && currentApi != null && anonKey.isNotEmpty()) {
            try {
                val dto = SupabaseProfileDto(
                    id = updated.id,
                    name = updated.name,
                    email = updated.email,
                    tier = updated.tier,
                    persona = updated.persona,
                    avatar_index = updated.avatarIndex,
                    xp_points = updated.xpPoints,
                    plan_type = updated.planType
                )
                currentApi.upsertProfile(anonKey, "Bearer ${session.access_token}", "resolution=merge-duplicates", listOf(dto))
            } catch (e: Exception) {
                Log.e(TAG, "Cloud profile persona update failed: ${e.message}")
            }
        }
    }

    suspend fun updateProfileAvatar(avatarIndex: Int) = withContext(Dispatchers.IO) {
        val session = _sessionState.value
        val profile = if (session != null) {
            database.profileDao().getProfileByIdOnce(session.user.id)
        } else {
            database.profileDao().getProfileOnce()
        } ?: return@withContext

        val updated = profile.copy(avatarIndex = avatarIndex)
        database.profileDao().saveProfile(updated)

        val currentApi = api
        val anonKey = Config.SUPABASE_ANON_KEY
        if (session != null && currentApi != null && anonKey.isNotEmpty()) {
            try {
                val dto = SupabaseProfileDto(
                    id = updated.id,
                    name = updated.name,
                    email = updated.email,
                    tier = updated.tier,
                    persona = updated.persona,
                    avatar_index = updated.avatarIndex,
                    xp_points = updated.xpPoints,
                    plan_type = updated.planType
                )
                currentApi.upsertProfile(anonKey, "Bearer ${session.access_token}", "resolution=merge-duplicates", listOf(dto))
            } catch (e: Exception) {
                Log.e(TAG, "Cloud profile avatar update failed: ${e.message}")
            }
        }
    }

    suspend fun updateProfileXp(xpPoints: Int) = withContext(Dispatchers.IO) {
        val session = _sessionState.value
        val profile = if (session != null) {
            database.profileDao().getProfileByIdOnce(session.user.id)
        } else {
            database.profileDao().getProfileOnce()
        } ?: return@withContext

        val updated = profile.copy(xpPoints = xpPoints)
        database.profileDao().saveProfile(updated)

        val currentApi = api
        val anonKey = Config.SUPABASE_ANON_KEY
        if (session != null && currentApi != null && anonKey.isNotEmpty()) {
            try {
                val dto = SupabaseProfileDto(
                    id = updated.id,
                    name = updated.name,
                    email = updated.email,
                    tier = updated.tier,
                    persona = updated.persona,
                    avatar_index = updated.avatarIndex,
                    xp_points = updated.xpPoints,
                    plan_type = updated.planType
                )
                currentApi.upsertProfile(anonKey, "Bearer ${session.access_token}", "resolution=merge-duplicates", listOf(dto))
            } catch (e: Exception) {
                Log.e(TAG, "Cloud profile XP update failed: ${e.message}")
            }
        }
    }
}
