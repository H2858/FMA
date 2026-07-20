package com.example.ui

import android.app.Application
import android.content.Context
import java.util.Locale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Core Services & Database
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "ai_coach_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    val supabaseClient: SupabaseClient by lazy {
        SupabaseClient(application, database)
    }

    private val geminiService = GeminiApiService(application)
    private val elevenLabsService = ElevenLabsService(application)

    // 2. Authentication State
    val sessionState: StateFlow<AuthSession?> = supabaseClient.sessionState
    val isLiveMode: StateFlow<Boolean> = supabaseClient.isLiveMode

    // Observed user profile from Room database
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userProfile: StateFlow<ProfileEntity?> = supabaseClient.sessionState
        .flatMapLatest { session ->
            if (session != null) {
                database.profileDao().getProfileById(session.user.id)
            } else {
                database.profileDao().getProfile()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun getCurrentProfile(): ProfileEntity? {
        val session = supabaseClient.sessionState.value
        return if (session != null) {
            database.profileDao().getProfileByIdOnce(session.user.id)
        } else {
            database.profileDao().getProfileOnce()
        }
    }

    // 3. Subscription & Features
    val isPremium: StateFlow<Boolean> = userProfile.map { it?.tier == "Premium" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // 4. Tasks (Observable flow from Room)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<TaskEntity>> = supabaseClient.sessionState
        .flatMapLatest { session ->
            if (session != null) {
                database.taskDao().getAllTasks()
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications (Observable flow from Room)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val notifications: StateFlow<List<NotificationEntity>> = supabaseClient.sessionState
        .flatMapLatest { session ->
            if (session != null) {
                database.notificationDao().getAllNotifications()
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Coaching Interaction State
    private val _currentPersona = MutableStateFlow(GeminiApiService.CoachPersona.MENTOR)
    val currentPersona: StateFlow<GeminiApiService.CoachPersona> = _currentPersona

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val coachingLog: StateFlow<List<CoachingLogEntity>> = supabaseClient.sessionState
        .flatMapLatest { session ->
            if (session != null) {
                database.coachingLogDao().getAllLogs()
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _coachingState = MutableStateFlow<CoachingState>(CoachingState.Idle)
    val coachingState: StateFlow<CoachingState> = _coachingState

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    // Voice Selection engine and states
    private val _selectedVoiceId = MutableStateFlow("eleven_mentor")
    val selectedVoiceId: StateFlow<String> = _selectedVoiceId.asStateFlow()

    private val _isAdminReal = MutableStateFlow(false)
    val isAdminReal: StateFlow<Boolean> = _isAdminReal.asStateFlow()

    fun toggleAdminForce() {
        _isAdminReal.value = !_isAdminReal.value
    }

    private val _isAiChatMode = MutableStateFlow(false)
    val isAiChatMode: StateFlow<Boolean> = _isAiChatMode.asStateFlow()

    fun toggleAiChatMode() {
        val nextValue = !_isAiChatMode.value
        _isAiChatMode.value = nextValue
        val prefs = getApplication<Application>().getSharedPreferences("user_session_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_ai_chat_mode", nextValue).apply()
    }

    // 6. Visual Theme Preference (Default to beautiful Cosmic Emerald matching user screenshot)
    private val _isEmeraldTheme = MutableStateFlow(true)
    val isEmeraldTheme: StateFlow<Boolean> = _isEmeraldTheme.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    val currentLanguage: StateFlow<AppLanguage> = LanguageHelper.currentLanguage

    fun setLanguage(language: AppLanguage) {
        LanguageHelper.setLanguage(language)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentProfile = getCurrentProfile()
            if (currentProfile != null) {
                database.profileDao().saveProfile(currentProfile.copy(isBiometricEnabled = enabled))
            }
        }
    }

    fun setAvatarIndex(index: Int) {
        viewModelScope.launch {
            supabaseClient.updateProfileAvatar(index)
        }
    }

    fun setVoiceId(voiceId: String) {
        _selectedVoiceId.value = voiceId
        val prefs = getApplication<Application>().getSharedPreferences("user_session_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_voice_id", voiceId).apply()
        
        // Auto-change the coach persona based on chosen voice!
        val matchingPersona = when (voiceId) {
            "eleven_spartan" -> GeminiApiService.CoachPersona.SPARTAN
            "eleven_mentor" -> GeminiApiService.CoachPersona.MENTOR
            "eleven_partner" -> GeminiApiService.CoachPersona.PARTNER
            else -> null
        }
        if (matchingPersona != null && _currentPersona.value != matchingPersona) {
            setPersona(matchingPersona)
        }
    }



    init {
        // Initialize dynamic Config values from SharedPreferences
        com.example.Config.getAppName(application)
        com.example.Config.getOpenRouterKey(application)

        // Load saved voice preference
        val prefs = application.getSharedPreferences("user_session_prefs", Context.MODE_PRIVATE)
        val loadedVoice = prefs.getString("selected_voice_id", "eleven_mentor") ?: "eleven_mentor"
        _selectedVoiceId.value = if (loadedVoice.startsWith("eleven_")) loadedVoice else "eleven_mentor"
        _isAiChatMode.value = prefs.getBoolean("is_ai_chat_mode", false)

        // Listen for user profile to sync coach persona
        viewModelScope.launch {
            userProfile.collect { profile ->
                profile?.let {
                    val pEnum = when (it.persona.uppercase()) {
                        "SPARTAN" -> GeminiApiService.CoachPersona.SPARTAN
                        "MENTOR" -> GeminiApiService.CoachPersona.MENTOR
                        "PARTNER" -> GeminiApiService.CoachPersona.PARTNER
                        "GENERAL" -> GeminiApiService.CoachPersona.GENERAL
                        else -> GeminiApiService.CoachPersona.MENTOR
                    }
                    _currentPersona.value = pEnum
                }
            }
        }

        // Keep real admin status updated on session state change
        viewModelScope.launch {
            supabaseClient.sessionState.collect { session ->
                if (session != null) {
                    val isUserAdmin = supabaseClient.checkIfUserIsAdmin(session.user.id, session.user.email ?: "")
                    _isAdminReal.value = isUserAdmin
                } else {
                    _isAdminReal.value = false
                }
            }
        }
    }

    // --- AUTHENTICATION ACTIONS ---

    fun login(email: String, password: String, onResult: (Result<AuthSession>) -> Unit) {
        viewModelScope.launch {
            val res = supabaseClient.signIn(email, password)
            onResult(res)
        }
    }

    fun signUp(email: String, password: String, name: String, onResult: (Result<AuthSession>) -> Unit) {
        viewModelScope.launch {
            val res = supabaseClient.signUp(email, password, name)
            onResult(res)
        }
    }

    fun loginWithGoogle(idToken: String, email: String, name: String, onResult: (Result<AuthSession>) -> Unit) {
        viewModelScope.launch {
            val res = supabaseClient.loginWithGoogle(idToken, email, name)
            onResult(res)
        }
    }

    fun logout() {
        supabaseClient.signOut()
    }

    // --- COACHING ACTIONS ---

    fun setPersona(persona: GeminiApiService.CoachPersona) {
        _currentPersona.value = persona
        viewModelScope.launch {
            supabaseClient.updateProfilePersona(persona.name)
        }
        
        // Auto-change voice to match persona!
        val matchingVoice = when (persona) {
            GeminiApiService.CoachPersona.SPARTAN -> "eleven_spartan"
            GeminiApiService.CoachPersona.MENTOR -> "eleven_mentor"
            GeminiApiService.CoachPersona.PARTNER -> "eleven_partner"
            GeminiApiService.CoachPersona.GENERAL -> "eleven_mentor" // general defaults to mentor voice
        }
        if (_selectedVoiceId.value != matchingVoice) {
            _selectedVoiceId.value = matchingVoice
            val prefs = getApplication<Application>().getSharedPreferences("user_session_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("selected_voice_id", matchingVoice).apply()
        }
    }

    fun submitCoachingQuery(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            _coachingState.value = CoachingState.Thinking
            stopAudio()

            val isChat = _isAiChatMode.value
            val aiResponse = if (isChat) {
                // Ask general chat without any coach identity or task constraints
                geminiService.askChat(query)
            } else {
                // Build dynamic context with outstanding tasks for proactive motivation
                val outstandingTasks = tasks.value.filter { !it.isCompleted }
                val taskContext = if (outstandingTasks.isNotEmpty()) {
                    "\n\nContext - User has these outstanding tasks that require proactive push or reflection:\n" +
                            outstandingTasks.joinToString("\n") { "- ${it.title}: ${it.description}" }
                } else {
                    "\n\nContext - User has completed all their tasks today! Celebrate this victory appropriately."
                }
                val finalPrompt = query + taskContext
                geminiService.askCoach(finalPrompt, _currentPersona.value)
            }

            // Save log to database
            val logEntity = CoachingLogEntity(
                id = UUID.randomUUID().toString(),
                persona = if (isChat) "CHAT" else _currentPersona.value.title,
                userMessage = query,
                aiResponse = aiResponse
            )
            database.coachingLogDao().insertLog(logEntity)

            _coachingState.value = CoachingState.Success(aiResponse)

            // Trigger text to speech
            speakResponse(aiResponse)
        }
    }

    fun generateAIImage(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _coachingState.value = CoachingState.Thinking
            try {
                // Generate a beautiful high-quality image URL using Pollinations AI
                val promptEncoded = java.net.URLEncoder.encode(prompt, "UTF-8")
                val imageUrl = "https://image.pollinations.ai/prompt/${promptEncoded}?width=1024&height=1024&nologo=true"
                
                // Construct a motivational or analytical response alongside the image!
                val aiResponse = if (_isAiChatMode.value) {
                    val lang = try {
                        LanguageHelper.currentLanguage.value.code
                    } catch (e: Exception) {
                        "en"
                    }
                    if (lang == "ar") {
                        "لقد قمت بإنشاء رسم توضيحي إبداعي عالي الجودة يمثل '$prompt' بناءً على طلبك."
                    } else {
                        "I have generated a high-quality creative illustration representing '$prompt' based on your request."
                    }
                } else {
                    when (_currentPersona.value) {
                        GeminiApiService.CoachPersona.SPARTAN -> {
                            "Here is the visual mapping of your spartan discipline for '$prompt': A harsh, powerful reminder of your ultimate goals. Do not let this vision slip away! Go get it!"
                        }
                        GeminiApiService.CoachPersona.MENTOR -> {
                            "I have envisioned a mindful, peaceful symbol of your journey for '$prompt'. Let this image bring you focus, clarity, and inner calmness as you take your next steps."
                        }
                        GeminiApiService.CoachPersona.PARTNER -> {
                            "BOOM! Check this out! Here is an energetic concept art for your goal '$prompt'! This is exactly the vibe we are aiming for! Let's make it happen!"
                        }
                        GeminiApiService.CoachPersona.GENERAL -> {
                            "I have generated a high-quality creative illustration representing '$prompt' based on your creative prompt."
                        }
                    }
                }
                
                val logEntity = CoachingLogEntity(
                    id = UUID.randomUUID().toString(),
                    persona = if (_isAiChatMode.value) "CHAT" else _currentPersona.value.title,
                    userMessage = "Generate Image: $prompt",
                    aiResponse = aiResponse
                )
                database.coachingLogDao().insertLog(logEntity)
                
                _coachingState.value = CoachingState.Success(aiResponse, imageUrl)
            } catch (e: Exception) {
                _coachingState.value = CoachingState.Error("Failed to generate image: ${e.localizedMessage}")
            }
        }
    }

    fun submitCoachingQueryWithImage(query: String, imageUriString: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _coachingState.value = CoachingState.Thinking
            stopAudio()
            
            val isChat = _isAiChatMode.value
            val aiResponse = if (isChat) {
                geminiService.askChat(query + " [Note: User attached an image proof/focus context]")
            } else {
                // Construct a motivational or analytical coach response for the image
                val outstandingTasks = tasks.value.filter { !it.isCompleted }
                val taskContext = if (outstandingTasks.isNotEmpty()) {
                    "\n\nContext - User has these outstanding tasks:\n" +
                            outstandingTasks.joinToString("\n") { "- ${it.title}" }
                } else {
                    "\n\nContext - User has completed all tasks!"
                }
                val finalPrompt = query + " [Note: User attached an image proof/focus context] " + taskContext
                geminiService.askCoach(finalPrompt, _currentPersona.value)
            }
            
            val logEntity = CoachingLogEntity(
                id = UUID.randomUUID().toString(),
                persona = if (isChat) "CHAT" else _currentPersona.value.title,
                userMessage = "$query (Attached Image Proof)",
                aiResponse = aiResponse
            )
            database.coachingLogDao().insertLog(logEntity)
            
            _coachingState.value = CoachingState.Success(aiResponse, imageUriString)
            speakResponse(aiResponse)
        }
    }

    fun speakResponse(text: String) {
        viewModelScope.launch {
            _isSpeaking.value = true
            stopAudio() // Stop any running speech first

            val finalVoiceId = _selectedVoiceId.value
            val voiceToUse = when (finalVoiceId) {
                "eleven_spartan" -> ElevenLabsService.SPARTAN_VOICE
                "eleven_mentor" -> ElevenLabsService.MENTOR_VOICE
                "eleven_partner" -> ElevenLabsService.PARTNER_VOICE
                else -> ElevenLabsService.MENTOR_VOICE
            }

            val success = elevenLabsService.synthesizeAndPlayCustomVoice(text, voiceToUse) {
                _isSpeaking.value = false
            }
            if (!success) {
                android.util.Log.e("MainViewModel", "ElevenLabs speech synthesis failed.")
                _isSpeaking.value = false
            }
        }
    }

    fun stopAudio() {
        elevenLabsService.stopPlayback()
        _isSpeaking.value = false
    }

    // --- SUBSCRIPTION WALL ---

    fun upgradeToPremium(planType: String = "Monthly") {
        viewModelScope.launch {
            supabaseClient.updateProfileTier("Premium", planType)
        }
    }

    fun downgradeToFree() {
        viewModelScope.launch {
            supabaseClient.updateProfileTier("Free")
        }
    }

    fun setVerified(verified: Boolean) {
        viewModelScope.launch {
            val currentProfile = getCurrentProfile()
            if (currentProfile != null) {
                database.profileDao().saveProfile(currentProfile.copy(isVerified = verified))
            }
        }
    }

    // --- TASK PERSISTENCE ---

    fun addTask(title: String, description: String, durationMinutes: Int = 0) {
        viewModelScope.launch {
            val deadlineTime = if (durationMinutes > 0) {
                System.currentTimeMillis() + (durationMinutes.toLong() * 60 * 1000)
            } else {
                0L
            }
            val durationText = when {
                durationMinutes == 1440 -> "Daily (يومية)"
                durationMinutes == 43200 -> "Monthly (شهرية)"
                durationMinutes == 525600 -> "Yearly (سنوية)"
                durationMinutes <= 0 -> ""
                durationMinutes == 1 -> "1 min"
                durationMinutes < 60 -> "$durationMinutes mins"
                durationMinutes % 60 == 0 -> "${durationMinutes / 60} hours"
                else -> "${durationMinutes / 60}h ${durationMinutes % 60}m"
            }
            supabaseClient.saveTask(
                title = title,
                description = description,
                isCompleted = false,
                deadlineTime = deadlineTime,
                durationText = durationText
            )
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val nextState = !task.isCompleted
            supabaseClient.saveTask(
                title = task.title,
                description = task.description,
                isCompleted = nextState,
                id = task.id,
                deadlineTime = task.deadlineTime,
                durationText = task.durationText
            )
            // Award +100 points for completing, or deduct if unchecked
            val currentProfile = getCurrentProfile()
            if (currentProfile != null) {
                val newPoints = if (nextState) currentProfile.xpPoints + 100 else (currentProfile.xpPoints - 100).coerceAtLeast(0)
                database.profileDao().saveProfile(currentProfile.copy(xpPoints = newPoints))
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            supabaseClient.deleteTask(taskId)
        }
    }

    // --- NOTIFICATION MANAGEMENT ---

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            database.notificationDao().markAsRead(id)
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            database.notificationDao().deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            database.notificationDao().clearAllNotifications()
        }
    }

    fun insertNotification(notification: NotificationEntity) {
        viewModelScope.launch {
            database.notificationDao().insertNotification(notification)
        }
    }

    fun generateAINotification() {
        viewModelScope.launch {
            try {
                val persona = _currentPersona.value
                val outstanding = tasks.value.filter { !it.isCompleted && !it.isExpired }
                val prompt = "Generate a short, single-sentence coaching advice or reminder (maximum 15 words) for the user in the style of a ${persona.title} coach. " +
                        "Currently, the user has ${outstanding.size} pending tasks. " +
                        "Make it sound highly personalized, motivating, direct, or blaming if they are neglecting work. " +
                        "Do not use markdown, emojis, or greetings. Just output the raw notification text."
                val messageText = geminiService.askCoach(prompt, persona)

                val id = UUID.randomUUID().toString()
                val notif = NotificationEntity(
                    id = id,
                    title = "${persona.title} Coach",
                    message = messageText,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    type = when {
                        outstanding.isEmpty() -> "motivation"
                        else -> "reminder"
                    }
                )
                database.notificationDao().insertNotification(notif)
            } catch (e: Exception) {
                // Fallback offline notification if keys are not present or network fails
                val id = UUID.randomUUID().toString()
                val fallbackText = when (_currentPersona.value) {
                    GeminiApiService.CoachPersona.SPARTAN -> "Discipline equals freedom. Execute your pending tasks immediately!"
                    GeminiApiService.CoachPersona.MENTOR -> "Reflect on your intentions today. Guard your moments with supreme care."
                    GeminiApiService.CoachPersona.PARTNER -> "Hey! Let's conquer this list together. Pick one tiny task and start!"
                    GeminiApiService.CoachPersona.GENERAL -> "Keep going! Small steps lead to great progress. Let's work on your tasks today!"
                }
                val notif = NotificationEntity(
                    id = id,
                    title = "${_currentPersona.value.title} Coach",
                    message = fallbackText,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    type = "reminder"
                )
                database.notificationDao().insertNotification(notif)
            }
        }
    }

    fun submitTaskReport() {
        viewModelScope.launch {
            val persona = _currentPersona.value
            val currentTasks = tasks.value
            if (currentTasks.isEmpty()) {
                submitCoachingQuery("I don't have any tasks logged today yet. Give me some coaching advice to start.")
                return@launch
            }

            val completed = currentTasks.filter { it.isCompleted }
            val expired = currentTasks.filter { it.isExpired }
            val pending = currentTasks.filter { !it.isCompleted && !it.isExpired }

            val reportPrompt = "User is submitting their final daily task report for your analysis. Here are the tasks with their statuses:\n" +
                    currentTasks.joinToString("\n") { "- Name: ${it.title}, Duration: ${it.durationText}, Status: " +
                            if (it.isCompleted) "COMPLETED" else if (it.isExpired) "FAILED/LOCKED (DEADLINE PASSED)" else "PENDING" } +
                    "\n\nAnalyze these results deeply. Provide critical insights, suggestions for better focus, and celebrate their completions. " +
                    "If they failed tasks, blame or motivate them strictly according to your selected Coach personality style."

            _coachingState.value = CoachingState.Thinking
            stopAudio()

            val aiResponse = geminiService.askCoach(reportPrompt, persona)

            val logEntity = CoachingLogEntity(
                id = UUID.randomUUID().toString(),
                persona = persona.title,
                userMessage = "Submit Task Report: Completed ${completed.size}, Expired ${expired.size}, Pending ${pending.size}",
                aiResponse = aiResponse
            )
            database.coachingLogDao().insertLog(logEntity)

            _coachingState.value = CoachingState.Success(aiResponse)
            speakResponse(aiResponse)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
    }
}

// --- STATE REPRESENTATIONS ---

sealed interface CoachingState {
    object Idle : CoachingState
    object Thinking : CoachingState
    data class Success(val response: String, val imageUrl: String? = null) : CoachingState
    data class Error(val message: String) : CoachingState
}
