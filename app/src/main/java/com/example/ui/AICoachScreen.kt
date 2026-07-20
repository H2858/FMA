package com.example.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.content.Context
import android.widget.Toast
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GeminiApiService
import com.example.data.NotificationEntity
import com.example.data.TaskEntity
import com.example.ui.theme.*
import coil.compose.AsyncImage
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import android.widget.MediaController
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AICoachScreen(
    viewModel: MainViewModel,
    onNavigateToPricing: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLeaderboard: () -> Unit
) {
    val currentPersona by viewModel.currentPersona.collectAsState()
    val coachingState by viewModel.coachingState.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val isEmerald by viewModel.isEmeraldTheme.collectAsState()
    val selectedVoiceId by viewModel.selectedVoiceId.collectAsState()
    val isAiChatMode by viewModel.isAiChatMode.collectAsState()

    fun text(key: String): String = LanguageHelper.translate(key)

    var userQuery by remember { mutableStateOf("") }
    var showTaskDialog by remember { mutableStateOf(false) }
    var showNotificationInbox by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var screenExpandedImageUrl by remember { mutableStateOf<String?>(null) }
    var attachedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                attachedImageUri = uri
            }
        }
    )

    var isListeningState by remember { mutableStateOf(false) }
    var recognitionText by remember { mutableStateOf("") }

    var showLiveVoiceSession by remember { mutableStateOf(false) }
    var liveMicMuted by remember { mutableStateOf(false) }
    var liveSessionStatus by remember { mutableStateOf("idle") }
    
    val speechRecognizer = remember {
        try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            null
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val spokenText = results?.firstOrNull()
                if (!spokenText.isNullOrBlank()) {
                    viewModel.submitCoachingQuery(spokenText)
                }
            }
        }
    )

    fun startSystemVoiceSession() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, LanguageHelper.currentLanguage.value.code)
                putExtra(RecognizerIntent.EXTRA_PROMPT, text("speak_prompt"))
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Voice Recognition not supported on this device. Please type.", Toast.LENGTH_LONG).show()
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                if (speechRecognizer != null) {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, LanguageHelper.currentLanguage.value.code)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    }
                    try {
                        speechRecognizer.startListening(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        startSystemVoiceSession()
                    }
                } else {
                    startSystemVoiceSession()
                }
            } else {
                Toast.makeText(context, "Permission Denied to Record Audio", Toast.LENGTH_SHORT).show()
            }
        }
    )

    fun startVoiceSession() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            if (speechRecognizer != null) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, LanguageHelper.currentLanguage.value.code)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                try {
                    speechRecognizer.startListening(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    startSystemVoiceSession()
                }
            } else {
                startSystemVoiceSession()
            }
        } else {
            recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    fun startLiveSpeechRecognizer() {
        if (liveMicMuted || !showLiveVoiceSession || viewModel.isSpeaking.value) return
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            if (speechRecognizer != null) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, LanguageHelper.currentLanguage.value.code)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                try {
                    speechRecognizer.cancel() // MUST call cancel first to release system audio focus and clear stale sessions
                    speechRecognizer.startListening(intent)
                    isListeningState = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    // Initialize native listener
    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                isListeningState = true
                recognitionText = "Listening..."
                if (showLiveVoiceSession) {
                    liveSessionStatus = "listening"
                }
            }
            override fun onBeginningOfSpeech() {
                recognitionText = "Recording..."
                if (showLiveVoiceSession) {
                    liveSessionStatus = "speaking"
                }
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListeningState = false
            }
            override fun onError(error: Int) {
                isListeningState = false
                if (showLiveVoiceSession) {
                    liveSessionStatus = "idle"
                    if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                        try {
                            speechRecognizer?.cancel()
                        } catch (e: Exception) {}
                    }
                    if (!liveMicMuted && showLiveVoiceSession) {
                        // Debounce restarts to avoid visual and performance stutter
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (showLiveVoiceSession && !liveMicMuted && !viewModel.isSpeaking.value) {
                                startLiveSpeechRecognizer()
                            }
                        }, 2000)
                    }
                } else {
                    if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                        startSystemVoiceSession()
                    }
                }
            }
            override fun onResults(results: android.os.Bundle?) {
                isListeningState = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (showLiveVoiceSession) {
                    if (!text.isNullOrBlank()) {
                        liveSessionStatus = "thinking"
                        viewModel.submitCoachingQuery(text)
                    } else {
                        liveSessionStatus = "listening"
                        startLiveSpeechRecognizer()
                    }
                } else {
                    if (!text.isNullOrBlank()) {
                        viewModel.submitCoachingQuery(text)
                    }
                }
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    if (!showLiveVoiceSession) {
                        recognitionText = text
                    }
                }
            }
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        }
        speechRecognizer?.setRecognitionListener(listener)
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    LaunchedEffect(isSpeaking) {
        if (!isSpeaking && showLiveVoiceSession && !liveMicMuted) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (showLiveVoiceSession && !liveMicMuted && !viewModel.isSpeaking.value) {
                    startLiveSpeechRecognizer()
                }
            }, 800)
        }
    }

    LaunchedEffect(coachingState, isSpeaking, isListeningState) {
        if (showLiveVoiceSession) {
            if (isSpeaking) {
                liveSessionStatus = "ai_speaking"
            } else if (coachingState is CoachingState.Thinking) {
                liveSessionStatus = "thinking"
            } else if (isListeningState) {
                liveSessionStatus = "listening"
            } else {
                liveSessionStatus = "idle"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Glowing Background Aura
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .align(Alignment.Center)
                .blur(85.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // High-end Dashboard Top Bar Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coach details on Left
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "FOCUS MATE AI",
                        color = EmeraldPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        maxLines = 1
                    )
                    Text(
                        text = if (isAiChatMode) "CHAT Mode Active" else "${currentPersona.title} Active",
                        color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                // Spaced Icons Row on Right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Premium Badge / Crown
                    if (isPremium) {
                        // Golden Professional Premium Indicator with Gold Sparkle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFF1C40F), Color(0xFFF39C12))
                                    )
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CrownIcon(
                                    modifier = Modifier.size(10.dp),
                                    tint = Color.Black
                               )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "PRO",
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    } else {
                        // Clickable gold crown with absolutely NO circle background or border, but compact touch target
                        IconButton(
                            onClick = onNavigateToPricing,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("home_premium_upgrade_crown")
                        ) {
                            CrownIcon(
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFFF1C40F)
                            )
                        }
                    }

                    // 2. Leaderboard Button
                    IconButton(
                        onClick = onNavigateToLeaderboard,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                            .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, CircleShape)
                            .testTag("home_leaderboard_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Leaderboard",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 3. Notification Bell with unread counter
                    val unreadCount = notifications.count { !it.isRead }
                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(
                            onClick = { showNotificationInbox = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                                .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, CircleShape)
                                .testTag("home_notification_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notification Inbox",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                                    .border(1.dp, MaterialTheme.colorScheme.background, CircleShape)
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$unreadCount",
                                    color = Color.White,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 4. Profile Button
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                            .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, CircleShape)
                            .testTag("home_profile_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = EmeraldIconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Title
            Text(
                text = if (isAiChatMode) "CHAT" else text("coach_greeting"),
                color = TextPrimary,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Modern Horizontal Coach Persona Selector (Dynamic Global Standards)
            if (!isAiChatMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        GeminiApiService.CoachPersona.SPARTAN to (Icons.Default.FlashOn to text("spartan")),
                        GeminiApiService.CoachPersona.MENTOR to (Icons.Default.Spa to text("mentor")),
                        GeminiApiService.CoachPersona.PARTNER to (Icons.Default.Whatshot to text("partner"))
                    ).forEach { (persona, pair) ->
                        val (icon, label) = pair
                        val isSelected = currentPersona == persona
                        val isLocked = persona == GeminiApiService.CoachPersona.SPARTAN && !isPremium
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) {
                                        if (isEmerald) EmeraldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    } else {
                                        if (isEmerald) GlassGreenOverlay else GlassOverlay
                                    }
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) {
                                        if (isEmerald) EmeraldPrimary else MaterialTheme.colorScheme.primary
                                    } else {
                                        if (isEmerald) GlassGreenBorder else GlassBorder
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    if (isLocked) {
                                        onNavigateToPricing()
                                    } else {
                                        viewModel.setPersona(persona)
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) {
                                            if (isEmerald) EmeraldPrimary else MaterialTheme.colorScheme.primary
                                        } else {
                                            TextPrimary.copy(alpha = 0.7f)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (isLocked) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Premium Locked",
                                            tint = Color(0xFFF1C40F),
                                            modifier = Modifier
                                                .size(10.dp)
                                                .align(Alignment.TopEnd)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    color = if (isSelected) TextPrimary else TextPrimary.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isAiChatMode) 20.dp else 10.dp))

            // Pulse Aura & Vortex Canvas
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .clickable {
                        if (isSpeaking) {
                            viewModel.stopAudio()
                        } else {
                            if (coachingState is CoachingState.Success) {
                                viewModel.speakResponse((coachingState as CoachingState.Success).response)
                            } else {
                                viewModel.submitCoachingQuery(text("fast_coaching_prompt"))
                            }
                        }
                    }
                    .testTag("central_aura_node"),
                contentAlignment = Alignment.Center
            ) {
                AnimatedPulsingVortex(
                    isSpeaking = isSpeaking,
                    isThinking = coachingState is CoachingState.Thinking,
                    isEmerald = isEmerald
                )

                Icon(
                    imageVector = if (isSpeaking) Icons.Default.VolumeUp else if (coachingState is CoachingState.Thinking) Icons.Default.Autorenew else Icons.Default.GraphicEq,
                    contentDescription = "Voice Indicator",
                    tint = TextPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Glassmorphic Coaching Console Response Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                    .border(
                        1.dp,
                        if (isEmerald) GlassGreenBorder else GlassBorder,
                        RoundedCornerShape(24.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (val state = coachingState) {
                        is CoachingState.Idle -> {
                            Text(
                                text = text("coach_idle"),
                                color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                            )
                        }
                        is CoachingState.Thinking -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Beautiful modern glowing pulsing circle
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 1.0f,
                                    targetValue = 1.3f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "scale"
                                )
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue = 0.4f,
                                    targetValue = 0.9f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "alpha"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            alpha = alpha
                                        )
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Dynamic customized thinking status based on selected persona and language
                                val currentLang = LanguageHelper.currentLanguage.value.code
                                val statusText = when (currentPersona) {
                                    GeminiApiService.CoachPersona.SPARTAN -> {
                                        if (currentLang == "ar") "المدرب الصارم يحلل أداءك..." else "Spartan Coach is analyzing your performance..."
                                    }
                                    GeminiApiService.CoachPersona.MENTOR -> {
                                        if (currentLang == "ar") "المرشد يفكر بسلام وهدوء..." else "Mindful Coach is reflecting peacefully..."
                                    }
                                    GeminiApiService.CoachPersona.PARTNER -> {
                                        if (currentLang == "ar") "شريكك يخطط لخطوتك القادمة..." else "Energizer Coach is planning your next victory..."
                                    }
                                    GeminiApiService.CoachPersona.GENERAL -> {
                                        if (currentLang == "ar") "المساعد الذكي يكتب لك الآن..." else "General Assistant is thinking..."
                                    }
                                }

                                Text(
                                    text = statusText,
                                    color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Modern 3 dots typing indicator
                                TypingIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        is CoachingState.Success -> {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth().padding(end = 40.dp)) {
                                    if (state.imageUrl != null) {
                                        AsyncImage(
                                            model = state.imageUrl,
                                            contentDescription = "AI Concept / Attachment Image",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, RoundedCornerShape(16.dp))
                                                .clickable { screenExpandedImageUrl = state.imageUrl },
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    TypewriterText(
                                        text = state.response,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                
                                // Audio Play/Stop speaker button on the top right of the response card
                                IconButton(
                                    onClick = {
                                        if (isSpeaking) {
                                            viewModel.stopAudio()
                                        } else {
                                            viewModel.speakResponse(state.response)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(if (isSpeaking) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
                                        .testTag("audio_response_playback_button")
                                ) {
                                    Icon(
                                        imageVector = if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                        contentDescription = "Play/Stop response audio",
                                        tint = if (isSpeaking) MaterialTheme.colorScheme.primary else TextPrimary.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        is CoachingState.Error -> {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (attachedImageUri != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                        .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AsyncImage(
                            model = attachedImageUri,
                            contentDescription = "Attached Image",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = "Image Attached Proof",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ready to send with next message",
                                color = if (isEmerald) TextMutedGreen else TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = { attachedImageUri = null },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear attachment",
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sleek Chat/Prompt Input Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                    .border(
                        1.dp,
                        if (isEmerald) GlassGreenBorder else GlassBorder,
                        RoundedCornerShape(24.dp)
                    )
            ) {
                // 1. Text Input Field Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = userQuery,
                        onValueChange = { userQuery = it },
                        placeholder = { Text(text("command_placeholder"), color = if (isEmerald) TextMutedGreen else TextMuted, fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("coaching_input"),
                        maxLines = 4
                    )
                }

                // 2. Bottom Toolbar Row containing action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp, top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // --- LEFT GROUP: Attachments & Options ---
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // A beautiful plus (+) button for image attachments
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (attachedImageUri != null) EmeraldIconColor.copy(alpha = 0.2f)
                                    else Color.Transparent,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Attach Progress Photo",
                                tint = if (attachedImageUri != null) EmeraldIconColor else (if (isEmerald) TextSecondaryGreen else TextSecondary),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Palette icon to Generate AI Image (enabled when text is present)
                        val isQueryPresent = userQuery.isNotBlank()
                        IconButton(
                            onClick = {
                                if (isQueryPresent) {
                                    viewModel.generateAIImage(userQuery)
                                    userQuery = ""
                                }
                            },
                            enabled = isQueryPresent,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Generate AI Image",
                                tint = if (isQueryPresent) EmeraldIconColor else (if (isEmerald) TextMutedGreen else TextMuted),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // --- MIDDLE GROUP: Pill-shaped voice selector ---
                    var showVoiceMenu by remember { mutableStateOf(false) }
                    val voiceDisplayName = when (selectedVoiceId) {
                        "eleven_spartan" -> "Strict Spartan"
                        "eleven_mentor" -> "Wise Mentor"
                        "eleven_partner" -> "Empathetic Partner"
                        else -> "Wise Mentor"
                    }
                    val voiceIcon = when (selectedVoiceId) {
                        "eleven_spartan" -> Icons.Default.Warning
                        "eleven_mentor" -> Icons.Default.Star
                        "eleven_partner" -> Icons.Default.Favorite
                        else -> Icons.Default.Star
                    }

                    Box(contentAlignment = Alignment.Center) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(EmeraldIconColor.copy(alpha = 0.12f))
                                .border(1.dp, EmeraldIconColor.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                                .clickable { showVoiceMenu = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = voiceIcon,
                                contentDescription = null,
                                tint = EmeraldIconColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = voiceDisplayName,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = if (isEmerald) TextSecondaryGreen else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showVoiceMenu,
                            onDismissRequest = { showVoiceMenu = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (selectedVoiceId == "eleven_spartan") EmeraldIconColor else TextSecondaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "Strict Spartan",
                                            color = if (selectedVoiceId == "eleven_spartan") EmeraldIconColor else TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.setVoiceId("eleven_spartan")
                                    showVoiceMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (selectedVoiceId == "eleven_mentor") EmeraldIconColor else TextSecondaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "Wise Mentor",
                                            color = if (selectedVoiceId == "eleven_mentor") EmeraldIconColor else TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.setVoiceId("eleven_mentor")
                                    showVoiceMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = if (selectedVoiceId == "eleven_partner") EmeraldIconColor else TextSecondaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "Empathetic Partner",
                                            color = if (selectedVoiceId == "eleven_partner") EmeraldIconColor else TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.setVoiceId("eleven_partner")
                                    showVoiceMenu = false
                                }
                            )
                        }
                    }

                    // --- RIGHT GROUP: Dynamic Action Buttons (Send or Mic & Live Waves) ---
                    val isInputNotEmpty = userQuery.isNotBlank() || attachedImageUri != null
                    AnimatedContent(
                        targetState = isInputNotEmpty,
                        label = "RightControlsTransition"
                    ) { hasText ->
                        if (hasText) {
                            IconButton(
                                onClick = {
                                    if (attachedImageUri != null) {
                                        viewModel.submitCoachingQueryWithImage(userQuery, attachedImageUri.toString())
                                        attachedImageUri = null
                                    } else {
                                        viewModel.submitCoachingQuery(userQuery)
                                    }
                                    userQuery = ""
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldButtonColor)
                                    .testTag("send_command_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send Command",
                                    tint = if (MaterialTheme.colorScheme.isLight()) Color.White else Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Voice Input / Recorder Button (Mic style)
                                IconButton(
                                    onClick = {
                                        SoundManager.playClick()
                                        startSystemVoiceSession()
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldIconColor.copy(alpha = 0.15f))
                                        .testTag("mic_voice_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice Input Recorder",
                                        tint = EmeraldIconColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Live AI Conversation Icon Button (glowing dynamic waveform style)
                                IconButton(
                                    onClick = {
                                        SoundManager.playClick()
                                        showLiveVoiceSession = true
                                        startLiveSpeechRecognizer()
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldIconColor.copy(alpha = 0.15f))
                                        .testTag("live_voice_mode_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = "Live AI Conversation",
                                        tint = EmeraldIconColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!isAiChatMode) {
                Spacer(modifier = Modifier.height(12.dp))

                // Sub-Dashboard section: Show Quick Add Task trigger
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                        .border(1.dp, if (isEmerald) GlassGreenBorder.copy(alpha = 0.5f) else GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { showTaskDialog = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddTask,
                            contentDescription = "Add Task",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text("sync_tasks"), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            val incomplete = tasks.count { !it.isCompleted && !it.isExpired }
                            val expiredCount = tasks.count { it.isExpired }
                            Text("$incomplete pending | $expiredCount failed/expired", color = if (isEmerald) TextSecondaryGreen else TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = if (isEmerald) TextMutedGreen else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp)) // Leave space for bottom nav bar
        }
    }

    if (screenExpandedImageUrl != null) {
        AlertDialog(
            onDismissRequest = { screenExpandedImageUrl = null },
            title = null,
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    AsyncImage(
                        model = screenExpandedImageUrl,
                        contentDescription = "Expanded View",
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { screenExpandedImageUrl = null }) {
                    Text("CLOSE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Task sync dialog popup (durations and hard locking)
    if (showTaskDialog) {
        TaskManagementDialog(
            tasks = tasks,
            onAddTask = { title, desc, dur -> viewModel.addTask(title, desc, dur) },
            onToggleTask = { viewModel.toggleTaskCompletion(it) },
            onDeleteTask = { viewModel.deleteTask(it) },
            onSubmitReport = { viewModel.submitTaskReport() },
            onDismiss = { showTaskDialog = false },
            isEmerald = isEmerald
        )
    }

    // Notification Inbox dialog modal
    if (showNotificationInbox) {
        NotificationInboxDialog(
            notifications = notifications,
            onMarkRead = { viewModel.markNotificationAsRead(it) },
            onDelete = { viewModel.deleteNotification(it) },
            onClearAll = { viewModel.clearAllNotifications() },
            onTriggerAI = { viewModel.generateAINotification() },
            onDismiss = { showNotificationInbox = false },
            isEmerald = isEmerald
        )
    }

    if (showLiveVoiceSession) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0E17)) // Dark cosmic background
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            val transition = rememberInfiniteTransition(label = "LivePulseTransition")

            // Elegant top bar with Live Indicator and Sound controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Badge with pulsing red dot
                val liveDotAlpha by transition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "LiveDot"
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .graphicsLayer(alpha = liveDotAlpha)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                    Text(
                        text = "LIVE",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Title displaying the current persona being conversed with
                Text(
                    text = currentPersona.title,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Selected Voice badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (selectedVoiceId) {
                            "tts_female" -> "امرأة"
                            "tts_male" -> "رجل"
                            "eleven_spartan" -> "Spartan"
                            "eleven_mentor" -> "Mentor"
                            "eleven_partner" -> "Partner"
                            else -> "صوت"
                        },
                        color = EmeraldPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Central Area - A gorgeous breathing digital representation of AI Core
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Beautiful pulsating glass sphere mimicking AI core
                val corePulse by transition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "CorePulse"
                )
                
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .graphicsLayer(scaleX = corePulse, scaleY = corePulse)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00E676).copy(alpha = 0.15f),
                                    Color(0xFF00B0FF).copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(1.5.dp, Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF00E676).copy(alpha = 0.4f),
                                Color(0xFF00B0FF).copy(alpha = 0.2f)
                            )
                        ), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "AI Core Active",
                        tint = EmeraldIconColor,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                // Status indicator
                Text(
                    text = when (liveSessionStatus) {
                        "listening" -> if (LanguageHelper.currentLanguage.value == AppLanguage.ARABIC) "جاري الاستماع إليك..." else "Listening to you..."
                        "speaking" -> if (LanguageHelper.currentLanguage.value == AppLanguage.ARABIC) "أنت تتحدث..." else "You are speaking..."
                        "thinking" -> if (LanguageHelper.currentLanguage.value == AppLanguage.ARABIC) "جاري التفكير..." else "AI is thinking..."
                        "ai_speaking" -> if (LanguageHelper.currentLanguage.value == AppLanguage.ARABIC) "الذكاء الاصطناعي يتحدث..." else "AI is speaking..."
                        else -> if (LanguageHelper.currentLanguage.value == AppLanguage.ARABIC) "متصل بالبث المباشر" else "Live Connected"
                    },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }

            // Flowing organic fluid waves at the bottom
            LiveVoiceWaves(
                status = liveSessionStatus,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
            )

            // Dynamic controller action bar at the bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 36.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decorator Action Button 1: Camera
                var cameraActive by remember { mutableStateOf(false) }
                IconButton(
                    onClick = {
                        cameraActive = !cameraActive
                        SoundManager.playClick()
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (cameraActive) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f))
                ) {
                    Icon(
                        imageVector = if (cameraActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = "Camera Control",
                        tint = if (cameraActive) EmeraldPrimary else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Decorator Action Button 2: Share / Screen
                IconButton(
                    onClick = {
                        SoundManager.playClick()
                        Toast.makeText(context, "Screen Sharing Aligned", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PresentToAll,
                        contentDescription = "Share Screen",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Interactive Primary Mic Control (Mute/Unmute)
                IconButton(
                    onClick = {
                        SoundManager.playClick()
                        liveMicMuted = !liveMicMuted
                        if (liveMicMuted) {
                            try {
                                speechRecognizer?.stopListening()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            liveSessionStatus = "idle"
                        } else {
                            startLiveSpeechRecognizer()
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (liveMicMuted) Color.Red.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f))
                ) {
                    Icon(
                        imageVector = if (liveMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Toggle Mute",
                        tint = if (liveMicMuted) Color.Red else EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Crucial Close / End Call button (Red X button)
                IconButton(
                    onClick = {
                        SoundManager.playClick()
                        showLiveVoiceSession = false
                        viewModel.stopAudio()
                        try {
                            speechRecognizer?.cancel()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Live Session",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LiveVoiceWaves(status: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "WaveTransition")
    
    // Animate the wave phases
    val phase1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase1"
    )
    
    val phase2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase2"
    )
    
    val phase3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase3"
    )

    // Animate amplitude multipliers based on the state for organic responsiveness
    val targetAmplitudeMultiplier = when (status) {
        "listening" -> 1.0f
        "speaking" -> 2.5f   // User speaking -> tall active waves
        "thinking" -> 1.5f   // Breathing wave
        "ai_speaking" -> 3.0f // AI speaking -> highly active, dancing waves
        else -> 0.8f
    }
    
    val amplitudeMultiplier by animateFloatAsState(
        targetValue = targetAmplitudeMultiplier,
        animationSpec = tween(600),
        label = "AmplitudeMultiplier"
    )

    // Breathing pulse for neon aura
    val auraScale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraScale"
    )

    Box(modifier = modifier.fillMaxWidth().height(280.dp)) {
        // Glowing Background Aura
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerPoint = center.copy(y = size.height * 0.8f)
            val glowRadius = size.width * 0.45f * auraScale
            
            val brushColor = when (status) {
                "listening" -> Color(0xFF00E676)
                "speaking" -> Color(0xFF00B0FF)
                "thinking" -> Color(0xFFFF9100)
                "ai_speaking" -> Color(0xFFD500F9)
                else -> Color(0xFF00E676)
            }
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        brushColor.copy(alpha = 0.25f),
                        brushColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = centerPoint,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = centerPoint
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Base Y coordinates
            val baseLineY = height * 0.7f
            
            // Wave 1: Deep Blue (Sapphire/Purple base)
            val path1 = androidx.compose.ui.graphics.Path()
            path1.moveTo(0f, height)
            for (x in 0..width.toInt() step 5) {
                val xFloat = x.toFloat()
                // Wave equation
                val angle = (xFloat / width) * 2 * Math.PI.toFloat() * 1.1f + phase1
                val y = baseLineY + Math.sin(angle.toDouble()).toFloat() * 18.dp.toPx() * amplitudeMultiplier
                path1.lineTo(xFloat, y)
            }
            path1.lineTo(width, height)
            path1.close()
            drawPath(
                path = path1,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E).copy(alpha = 0.5f),
                        Color(0xFF0D47A1).copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    startY = baseLineY - 20.dp.toPx(),
                    endY = height
                )
            )

            // Wave 2: Neon Emerald / Teal (Dynamic mid-layer)
            val path2 = androidx.compose.ui.graphics.Path()
            path2.moveTo(0f, height)
            for (x in 0..width.toInt() step 5) {
                val xFloat = x.toFloat()
                val angle = (xFloat / width) * 2 * Math.PI.toFloat() * 1.6f + phase2
                val y = baseLineY + 8.dp.toPx() + Math.cos(angle.toDouble()).toFloat() * 24.dp.toPx() * amplitudeMultiplier
                path2.lineTo(xFloat, y)
            }
            path2.lineTo(width, height)
            path2.close()
            drawPath(
                path = path2,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00B0FF).copy(alpha = 0.45f),
                        Color(0xFF00E676).copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    startY = baseLineY - 30.dp.toPx(),
                    endY = height
                )
            )

            // Wave 3: Glowing Cyan / Indigo Accent (Vibrant foreground wave)
            val path3 = androidx.compose.ui.graphics.Path()
            path3.moveTo(0f, height)
            for (x in 0..width.toInt() step 5) {
                val xFloat = x.toFloat()
                val angle = (xFloat / width) * 2 * Math.PI.toFloat() * 2.2f + phase3
                val y = baseLineY - 6.dp.toPx() + Math.sin(angle.toDouble()).toFloat() * 14.dp.toPx() * (amplitudeMultiplier * 1.2f)
                path3.lineTo(xFloat, y)
            }
            path3.lineTo(width, height)
            path3.close()
            drawPath(
                path = path3,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00E676).copy(alpha = 0.6f),
                        Color(0xFF1DE9B6).copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    startY = baseLineY - 40.dp.toPx(),
                    endY = height
                )
            )
        }
    }
}

@Composable
fun NotificationInboxDialog(
    notifications: List<NotificationEntity>,
    onMarkRead: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    onTriggerAI: () -> Unit,
    onDismiss: () -> Unit,
    isEmerald: Boolean
) {
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }
    var expandedVideoUrl by remember { mutableStateOf<String?>(null) }

    if (expandedImageUrl != null) {
        AlertDialog(
            onDismissRequest = { expandedImageUrl = null },
            title = null,
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = expandedImageUrl,
                        contentDescription = "Expanded Image View",
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { expandedImageUrl = null }) {
                    Text("CLOSE", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.Black.copy(alpha = 0.95f),
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (expandedVideoUrl != null) {
        AlertDialog(
            onDismissRequest = { expandedVideoUrl = null },
            title = {
                Text("WATCH VIDEO", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val mc = MediaController(ctx)
                                mc.setAnchorView(this)
                                setMediaController(mc)
                                setVideoPath(expandedVideoUrl)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { expandedVideoUrl = null }) {
                    Text("CLOSE", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NOTIFICATION INBOX",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                if (notifications.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("CLEAR ALL", color = MaterialTheme.colorScheme.tertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Interactive trigger button to generate AI Coach message
                Button(
                    onClick = onTriggerAI,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("trigger_ai_notif_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GENERATE AI COACH MESSAGE", color = MaterialTheme.colorScheme.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(modifier = Modifier.height(300.dp)) {
                    if (notifications.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Your AI notification inbox is empty.",
                                color = if (isEmerald) TextMutedGreen else TextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(notifications) { notification ->
                                // Auto-mark read on render
                                LaunchedEffect(notification.id) {
                                    if (!notification.isRead) {
                                        onMarkRead(notification.id)
                                    }
                                }

                                val icon = when (notification.type) {
                                    "motivation" -> Icons.Default.Favorite
                                    "blame" -> Icons.Default.Warning
                                    "guidance" -> Icons.Default.Info
                                    else -> Icons.Default.Notifications
                                }

                                val iconColor = when (notification.type) {
                                    "motivation" -> EmeraldPrimary
                                    "blame" -> Color.Red
                                    "guidance" -> MaterialTheme.colorScheme.secondary
                                    else -> TextPrimary
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (notification.isRead) Color.Transparent else (if (isEmerald) GlassGreenOverlay else GlassOverlay))
                                        .border(1.dp, if (notification.isRead) (if (isEmerald) GlassGreenBorder.copy(alpha = 0.3f) else GlassBorder.copy(alpha = 0.3f)) else EmeraldPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Row(modifier = Modifier.weight(1f)) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = notification.type,
                                                tint = iconColor,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .padding(top = 2.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = notification.title,
                                                    color = TextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = notification.message,
                                                    color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                                                    fontSize = 11.sp,
                                                    lineHeight = 15.sp
                                                )

                                                if (!notification.imageUrl.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Card(
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(130.dp)
                                                            .clickable { expandedImageUrl = notification.imageUrl }
                                                    ) {
                                                        AsyncImage(
                                                            model = notification.imageUrl,
                                                            contentDescription = "Notification Attachment Image",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                }

                                                if (!notification.videoUrl.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(130.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.Black.copy(alpha = 0.6f))
                                                            .clickable { expandedVideoUrl = notification.videoUrl },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(
                                                                imageVector = Icons.Default.PlayCircle,
                                                                contentDescription = "Play Video Attachment",
                                                                tint = EmeraldIconColor,
                                                                modifier = Modifier.size(44.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = "WATCH VIDEO",
                                                                color = Color.White,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = { onDelete(notification.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete notification",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = MaterialTheme.colorScheme.secondary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, RoundedCornerShape(24.dp))
    )
}

@Composable
fun TaskManagementDialog(
    tasks: List<TaskEntity>,
    onAddTask: (String, String, Int) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteTask: (String) -> Unit,
    onSubmitReport: () -> Unit,
    onDismiss: () -> Unit,
    isEmerald: Boolean
) {
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDesc by remember { mutableStateOf("") }
    var newTaskDuration by remember { mutableStateOf("") } // duration in minutes

    // Custom recurrence parameters
    var selectedRecurrence by remember { mutableStateOf("daily") }
    var dailyFreqDays by remember { mutableStateOf("1") }
    var dailyDeadlineHour by remember { mutableStateOf("21") }
    var monthlyDay by remember { mutableStateOf("1") }
    var monthlyInterval by remember { mutableStateOf("1") }
    var yearlyMonth by remember { mutableStateOf("1") }
    var yearlyDay by remember { mutableStateOf("1") }
    var enforcementMode by remember { mutableStateOf("Moderate Spartan") }

    fun text(key: String): String = LanguageHelper.translate(key)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text("task_queue_sync"),
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Report Submission Button
                Button(
                    onClick = {
                        onSubmitReport()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("submit_report_button")
                ) {
                    Text("SUBMIT REPORT", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = text("task_queue_desc") + " Enter a duration (minutes) to enforce a strict locking deadline.",
                    color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Add Task Panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, if (isEmerald) GlassGreenBorder.copy(alpha = 0.3f) else GlassBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text(text("add_focus_action"), fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_task_title")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newTaskDesc,
                        onValueChange = { newTaskDesc = it },
                        label = { Text(text("details_target"), fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_task_desc")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = text("recurrence_label"),
                        color = TextPrimary.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "daily" to text("recurrence_daily"),
                            "monthly" to text("recurrence_monthly"),
                            "yearly" to text("recurrence_yearly"),
                            "custom" to text("recurrence_custom")
                        ).forEach { (key, label) ->
                            val isSel = selectedRecurrence == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else (if (isEmerald) GlassGreenOverlay else GlassOverlay))
                                    .border(
                                        width = if (isSel) 1.5.dp else 1.dp,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else (if (isEmerald) GlassGreenBorder else GlassBorder),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedRecurrence = key }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) TextPrimary else TextPrimary.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isEmerald) GlassGreenOverlay.copy(alpha = 0.5f) else GlassOverlay.copy(alpha = 0.5f))
                            .border(1.dp, if (isEmerald) GlassGreenBorder.copy(alpha = 0.5f) else GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "CUSTOMIZE RECURRENCE PARAMETERS",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            
                            when (selectedRecurrence) {
                                "daily" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = dailyFreqDays,
                                            onValueChange = { dailyFreqDays = it },
                                            label = { Text("Repeat Every (Days)", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = dailyDeadlineHour,
                                            onValueChange = { dailyDeadlineHour = it },
                                            label = { Text("Deadline Hour (0-23)", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                "monthly" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = monthlyDay,
                                            onValueChange = { monthlyDay = it },
                                            label = { Text("Day of Month (1-31)", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = monthlyInterval,
                                            onValueChange = { monthlyInterval = it },
                                            label = { Text("Interval (Months)", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                "yearly" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = yearlyMonth,
                                            onValueChange = { yearlyMonth = it },
                                            label = { Text("Month (1-12)", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = yearlyDay,
                                            onValueChange = { yearlyDay = it },
                                            label = { Text("Day (1-31)", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                "custom" -> {
                                    OutlinedTextField(
                                        value = newTaskDuration,
                                        onValueChange = { newTaskDuration = it },
                                        label = { Text("One-Shot Timer (Minutes)", fontSize = 10.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "ENFORCEMENT STRENGTH MODE",
                                color = TextPrimary.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Gentle Checking", "Moderate Spartan", "Strict Lockdown").forEach { mode ->
                                    val isSel = enforcementMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent)
                                            .border(1.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .clickable { enforcementMode = mode }
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode,
                                            color = if (isSel) TextPrimary else TextPrimary.copy(alpha = 0.6f),
                                            fontSize = 9.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (newTaskTitle.isNotBlank()) {
                                val customDesc = buildString {
                                    append(newTaskDesc)
                                    if (newTaskDesc.isNotEmpty()) append(" | ")
                                    append("Recurrence: ${selectedRecurrence.uppercase()} (")
                                    when (selectedRecurrence) {
                                        "daily" -> append("Every $dailyFreqDays days, Deadline: $dailyDeadlineHour:00")
                                        "monthly" -> append("Every $monthlyInterval months on Day $monthlyDay")
                                        "yearly" -> append("Annual Goal on Month $yearlyMonth/Day $yearlyDay")
                                        else -> append("One-shot")
                                    }
                                    append(") | Enforcement: $enforcementMode")
                                }
                                val dur = when (selectedRecurrence) {
                                    "daily" -> {
                                        val freq = dailyFreqDays.toIntOrNull() ?: 1
                                        freq * 1440
                                    }
                                    "monthly" -> {
                                        val interval = monthlyInterval.toIntOrNull() ?: 1
                                        interval * 43200
                                    }
                                    "yearly" -> 525600
                                    else -> newTaskDuration.toIntOrNull() ?: 60
                                }
                                onAddTask(newTaskTitle, customDesc, dur)
                                newTaskTitle = ""
                                newTaskDesc = ""
                                newTaskDuration = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("add_dialog_task_button")
                    ) {
                        Text(text("queue_action"), color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tasks List
                Text(
                    text = text("sync_queue_status") + " ✨",
                    color = if (isEmerald) EmeraldPrimary else MaterialTheme.colorScheme.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (tasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.15f))
                                .border(1.dp, if (isEmerald) GlassGreenBorder.copy(alpha = 0.3f) else GlassBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (isEmerald) TextMutedGreen.copy(alpha = 0.6f) else TextMuted.copy(alpha = 0.6f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = text("no_actions_queued"),
                                    color = if (isEmerald) TextMutedGreen else TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        tasks.forEach { task ->
                            ModernTaskCard(
                                task = task,
                                isEmerald = isEmerald,
                                onToggleTask = onToggleTask,
                                onDeleteTask = onDeleteTask
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = text("dismiss"),
                    color = if (isEmerald) EmeraldPrimary else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.border(
            width = 1.5.dp,
            color = if (isEmerald) GlassGreenBorder else GlassBorder,
            shape = RoundedCornerShape(28.dp)
        )
    )
}

@Composable
fun ModernTaskCard(
    task: TaskEntity,
    isEmerald: Boolean,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    val isExpired = task.isExpired
    val isCompleted = task.isCompleted

    val infiniteTransition = rememberInfiniteTransition(label = "TaskCardGlow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScaleFactor"
    )

    val flickerTransition = rememberInfiniteTransition(label = "TaskFlicker")
    val flickerAlpha by flickerTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 220, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlickerAlpha"
    )

    val lowerDesc = task.description.lowercase()
    val recurrenceIcon = when {
        lowerDesc.contains("recurrence: daily") -> Icons.Default.DateRange
        lowerDesc.contains("recurrence: monthly") -> Icons.Default.CalendarToday
        lowerDesc.contains("recurrence: yearly") -> Icons.Default.EmojiEvents
        else -> Icons.Default.Schedule
    }

    val cardBg = when {
        isExpired -> Color(0x22D32F2F)
        isCompleted -> if (isEmerald) EmeraldPrimary.copy(alpha = 0.04f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f)
        else -> if (isEmerald) GlassGreenOverlay.copy(alpha = 0.25f) else GlassOverlay.copy(alpha = 0.25f)
    }

    val cardBorderColor = when {
        isExpired -> Color.Red.copy(alpha = 0.35f)
        isCompleted -> if (isEmerald) EmeraldPrimary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
        else -> if (isEmerald) EmeraldPrimary.copy(alpha = 0.12f + 0.22f * pulseAlpha) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f + 0.22f * pulseAlpha)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(
                width = if (!isCompleted && !isExpired) 1.5.dp else 1.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Category Recurrence Icon with modern professional neon flicker
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isCompleted) Color.Gray.copy(alpha = 0.08f)
                            else if (isExpired) Color.Red.copy(alpha = 0.1f)
                            else (if (isEmerald) EmeraldPrimary else MaterialTheme.colorScheme.primary).copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = recurrenceIcon,
                        contentDescription = "Recurrence Type",
                        tint = if (isCompleted) Color.Gray.copy(alpha = 0.7f)
                               else if (isExpired) Color.Red
                               else (if (isEmerald) EmeraldPrimary else MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer(alpha = if (isCompleted) 1f else flickerAlpha)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = task.title,
                            color = when {
                                isExpired -> Color.Red
                                isCompleted -> if (isEmerald) TextMutedGreen else TextMuted
                                else -> TextPrimary
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            style = if (isCompleted) androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle()
                        )

                        // Elegant Experience Points Badge with a sparkling Star icon
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isCompleted) Color.Gray.copy(alpha = 0.1f)
                                    else if (isEmerald) EmeraldPrimary.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "XP",
                                    tint = if (isCompleted) Color.Gray else if (isEmerald) EmeraldPrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(10.dp)
                                        .graphicsLayer(
                                            scaleX = if (isCompleted) 1f else scaleFactor,
                                            scaleY = if (isCompleted) 1f else scaleFactor
                                        )
                                )
                                Text(
                                    text = "+150 XP",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isCompleted) Color.Gray else TextPrimary
                                )
                            }
                        }
                    }

                    if (task.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        // Clean recurrence format parser to present beautiful visual details
                        val displayDesc = if (lowerDesc.contains(" | recurrence:")) {
                            task.description.substringBefore(" | recurrence:")
                        } else {
                            task.description
                        }
                        if (displayDesc.isNotEmpty()) {
                            Text(
                                text = displayDesc,
                                color = if (isExpired) Color.Red.copy(alpha = 0.75f) else (if (isEmerald) TextMutedGreen else TextMuted),
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }

                    if (task.durationText.isNotEmpty() || isExpired) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (task.durationText.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.25f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "⏱️ " + task.durationText,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEmerald) EmeraldPrimary else Color.LightGray
                                    )
                                }
                            }
                            if (isExpired) {
                                Text(
                                    text = "🔒 LOCKED (DEADLINE EXPIRED)",
                                    color = Color.Red,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Interactive Checkbox / Expired Lock with beautiful glowing halos
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) {
                                if (isEmerald) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            } else if (isExpired) {
                                Color.Red.copy(alpha = 0.15f)
                            } else {
                                if (isEmerald) EmeraldPrimary.copy(alpha = 0.1f * pulseAlpha) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f * pulseAlpha)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isCompleted) {
                                (if (isEmerald) EmeraldPrimary else MaterialTheme.colorScheme.secondary).copy(alpha = 0.4f)
                            } else if (isExpired) {
                                Color.Red.copy(alpha = 0.4f)
                            } else {
                                (if (isEmerald) EmeraldPrimary else MaterialTheme.colorScheme.primary).copy(alpha = 0.3f * pulseAlpha)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isExpired) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Expired Locked",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Checkbox(
                            checked = isCompleted,
                            onCheckedChange = { onToggleTask(task) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = if (isEmerald) EmeraldPrimary else MaterialTheme.colorScheme.secondary,
                                uncheckedColor = Color.Transparent,
                                checkmarkColor = Color.Black
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Cloud Sync Indicator with professional fade breathing effect
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (task.synced) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                        contentDescription = "Sync Status",
                        tint = if (task.synced) EmeraldPrimary else Color.Gray,
                        modifier = Modifier
                            .size(13.dp)
                            .graphicsLayer(alpha = if (task.synced) 1f else pulseAlpha)
                    )
                }

                IconButton(
                    onClick = { onDeleteTask(task.id) },
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Task",
                        tint = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedPulsingVortex(isSpeaking: Boolean, isThinking: Boolean, isEmerald: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "AuraVortex")

    // Ambient Breathing Multipliers
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteTransitionSpec(duration = 3500),
        label = "Layer1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.22f,
        animationSpec = infiniteTransitionSpec(duration = 5000),
        label = "Layer2"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteTransitionSpec(duration = if (isSpeaking) 4000 else 12000, easing = LinearEasing),
        label = "Rotation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.width / 2

        // Radial backdrop glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = if (isSpeaking) 0.5f else if (isThinking) 0.4f else 0.25f),
                    primaryColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = center,
                radius = maxRadius * 0.95f
            ),
            radius = maxRadius * 0.95f
        )

        // Draw multiple overlapping rotating ellipses to replicate the stunning glowing ring in the screenshot exactly
        val strokeWidth = if (isSpeaking) 3.5.dp.toPx() else 2.dp.toPx()
        
        // Loop to draw 4 glowing nested orbital layers
        for (i in 0 until 4) {
            val angleOffset = i * 45f
            val currentAngle = rotationAngle + angleOffset
            val scaleX = 0.95f - (i * 0.07f)
            val scaleY = 0.65f + (i * 0.05f)
            
            withTransform({
                rotate(currentAngle, center)
                scale(scaleX * pulseScale1, scaleY * pulseScale2, center)
            }) {
                drawOval(
                    color = primaryColor.copy(alpha = if (isSpeaking) 0.85f else if (isThinking) 0.65f else 0.45f - (i * 0.06f)),
                    style = Stroke(
                        width = strokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    ),
                    topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                    size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2)
                )
            }
        }
        
        // Inner pulsing orb core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.75f),
                    secondaryColor.copy(alpha = 0.35f),
                    Color.Transparent
                ),
                center = center,
                radius = maxRadius * 0.25f * pulseScale1
            ),
            radius = maxRadius * 0.25f * pulseScale1
        )
    }
}

private fun <T> infiniteTransitionSpec(duration: Int, easing: Easing = FastOutSlowInEasing): InfiniteRepeatableSpec<T> {
    return infiniteRepeatable(
        animation = tween(durationMillis = duration, easing = easing),
        repeatMode = RepeatMode.Reverse
    )
}

@Composable
fun CrownIcon(modifier: Modifier = Modifier, tint: Color = Color(0xFFF1C40F)) {
    Canvas(modifier = modifier.size(24.dp)) {
        val path = androidx.compose.ui.graphics.Path().apply {
            // Draw a crown bottom base line and five key anchor peaks
            moveTo(size.width * 0.15f, size.height * 0.85f)
            lineTo(size.width * 0.85f, size.height * 0.85f)
            lineTo(size.width * 0.92f, size.height * 0.32f)
            lineTo(size.width * 0.68f, size.height * 0.55f)
            lineTo(size.width * 0.5f, size.height * 0.22f)
            lineTo(size.width * 0.32f, size.height * 0.55f)
            lineTo(size.width * 0.08f, size.height * 0.32f)
            close()
        }
        drawPath(path, color = tint)
        
        // Draw elegant mini circles (pearls) on the crown peaks
        drawCircle(color = tint, radius = 2.dp.toPx(), center = Offset(size.width * 0.08f, size.height * 0.32f))
        drawCircle(color = tint, radius = 2.dp.toPx(), center = Offset(size.width * 0.5f, size.height * 0.22f))
        drawCircle(color = tint, radius = 2.dp.toPx(), center = Offset(size.width * 0.92f, size.height * 0.32f))
        
        // Draw a sleek base line accent
        drawLine(
            color = Color.Black.copy(alpha = 0.3f),
            start = Offset(size.width * 0.2f, size.height * 0.78f),
            end = Offset(size.width * 0.8f, size.height * 0.78f),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

@Composable
fun SoundWaveVoiceButton(
    isListening: Boolean,
    isSpeaking: Boolean,
    onClick: () -> Unit,
    isEmerald: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "soundwave_pulse")
    
    // Animate height multipliers for 4 soundwave bars
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val bar4Height by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = if (isEmerald) {
                        listOf(EmeraldPrimary, Color(0xFF00E5FF))
                    } else {
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    }
                )
            )
            .clickable { onClick() }
            .testTag("soundwave_voice_button"),
        contentAlignment = Alignment.Center
    ) {
        val active = isListening || isSpeaking
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val h1 = if (active) bar1Height else 0.4f
            val h2 = if (active) bar2Height else 0.7f
            val h3 = if (active) bar3Height else 0.5f
            val h4 = if (active) bar4Height else 0.3f

            Box(modifier = Modifier.size(2.5.dp, (14.dp * h1)).clip(CircleShape).background(Color.Black))
            Box(modifier = Modifier.size(2.5.dp, (14.dp * h2)).clip(CircleShape).background(Color.Black))
            Box(modifier = Modifier.size(2.5.dp, (14.dp * h3)).clip(CircleShape).background(Color.Black))
            Box(modifier = Modifier.size(2.5.dp, (14.dp * h4)).clip(CircleShape).background(Color.Black))
        }
    }
}

@Composable
fun TypingIndicator(modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val actualColor = if (color == Color.Unspecified) EmeraldPrimary else color
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    @Composable
    fun PulsingDot(delayMillis: Int) {
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 600
                    0.4f at 0 with LinearEasing
                    1.0f at 200 with FastOutSlowInEasing
                    0.4f at 400 with LinearEasing
                    0.4f at 600 with LinearEasing
                },
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(delayMillis)
            ),
            label = "pulsingDot"
        )
        
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 600
                    0.3f at 0 with LinearEasing
                    1.0f at 200 with FastOutSlowInEasing
                    0.3f at 400 with LinearEasing
                    0.3f at 600 with LinearEasing
                },
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(delayMillis)
            ),
            label = "alphaDot"
        )

        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    alpha = alpha
                )
                .background(color = actualColor, shape = CircleShape)
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PulsingDot(0)
        PulsingDot(150)
        PulsingDot(300)
    }
}

@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null
) {
    var displayedText by remember(text) { mutableStateOf("") }
    var isTypingComplete by remember(text) { mutableStateOf(false) }

    LaunchedEffect(text) {
        displayedText = ""
        isTypingComplete = false
        if (text.isEmpty()) return@LaunchedEffect

        // Determine step sizing dynamically for a smooth typewriter effect
        val charsPerStep = when {
            text.length > 800 -> 5
            text.length > 400 -> 3
            text.length > 200 -> 2
            else -> 1
        }
        val delayMs = 15L // 15ms delay per step

        var currentLength = 0
        while (currentLength < text.length && !isTypingComplete) {
            currentLength = (currentLength + charsPerStep).coerceAtMost(text.length)
            displayedText = text.substring(0, currentLength)
            kotlinx.coroutines.delay(delayMs)
        }
        displayedText = text
        isTypingComplete = true
    }

    // Pulsing cursor alpha
    val infiniteTransition = rememberInfiniteTransition(label = "CursorTransition")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CursorAlpha"
    )

    // Append a custom blinking indicator block if typing is not complete
    val annotatedText = remember(displayedText, isTypingComplete, cursorAlpha) {
        androidx.compose.ui.text.buildAnnotatedString {
            append(displayedText)
            if (!isTypingComplete) {
                // Add a small green cursor character styled elegantly
                pushStyle(androidx.compose.ui.text.SpanStyle(
                    color = EmeraldPrimary.copy(alpha = cursorAlpha),
                    fontWeight = FontWeight.Bold
                ))
                append(" ▋")
                pop()
            }
        }
    }

    Text(
        text = annotatedText,
        modifier = modifier.clickable(
            indication = null,
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        ) {
            // Instantly complete typing on click
            if (!isTypingComplete) {
                isTypingComplete = true
                displayedText = text
            }
        },
        color = color,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = fontWeight
    )
}


