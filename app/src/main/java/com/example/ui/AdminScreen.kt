package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Config
import com.example.data.NotificationEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AdminScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isEmerald by viewModel.isEmeraldTheme.collectAsState()

    var notificationImage by remember { mutableStateOf("") }
    var notificationVideo by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                notificationImage = uri.toString()
            }
        }
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                notificationVideo = uri.toString()
            }
        }
    )
    
    var activeTab by remember { mutableStateOf(0) }
    
    var notificationTitle by remember { mutableStateOf("") }
    var notificationBody by remember { mutableStateOf("") }
    
    var apiKeyInput by remember { mutableStateOf(Config.getOpenRouterKey(context)) }
    var elevenLabsApiKeyInput by remember { mutableStateOf(Config.getElevenLabsKey(context)) }
    var appNameInput by remember { mutableStateOf(Config.getAppName(context)) }
    
    var userSearchQuery by remember { mutableStateOf("") }
    val mockUsers = remember {
        mutableStateListOf(
            Pair("lovablehibo@gmail.com", false),
            Pair("admin_test@focusmate.ai", false),
            Pair("user_alpha@gmail.com", false),
            Pair("user_beta@gmail.com", false),
            Pair("user_gamma@gmail.com", true)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                        .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, CircleShape)
                        .testTag("admin_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "ADMIN PORTAL",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "SECURE EXECUTIVE NODE CONTROL",
                        color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                    .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val tabs = listOf("ALERTS", "API", "USERS", "BRAND")
                tabs.forEachIndexed { index, title ->
                    val isSelected = activeTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) (if (isEmerald) EmeraldPrimary else MaterialTheme.colorScheme.primary) else Color.Transparent)
                            .clickable { activeTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.Black else (if (isEmerald) TextSecondaryGreen else TextSecondary),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (activeTab) {
                    0 -> {
                        Text(
                            text = "BROADCAST SYSTEM NOTIFICATION",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                                .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, RoundedCornerShape(24.dp))
                                .padding(24.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedTextField(
                                    value = notificationTitle,
                                    onValueChange = { notificationTitle = it },
                                    label = { Text("Notification Title", color = TextSecondaryGreen) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = notificationBody,
                                    onValueChange = { notificationBody = it },
                                    label = { Text("Body Message Content", color = TextSecondaryGreen) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = notificationImage,
                                    onValueChange = { notificationImage = it },
                                    label = { Text("Image Attachment URL / File", color = TextSecondaryGreen) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                imagePickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoLibrary,
                                                contentDescription = "Upload Photo",
                                                tint = EmeraldIconColor
                                            )
                                        }
                                    }
                                )

                                OutlinedTextField(
                                    value = notificationVideo,
                                    onValueChange = { notificationVideo = it },
                                    label = { Text("Video Attachment URL / File", color = TextSecondaryGreen) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                videoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                                )
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VideoLibrary,
                                                contentDescription = "Upload Video",
                                                tint = EmeraldIconColor
                                            )
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Direct Gallery Upload Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            imagePickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimary.copy(alpha = 0.15f),
                                            contentColor = EmeraldPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Upload,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("UPLOAD PHOTO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            videoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimary.copy(alpha = 0.15f),
                                            contentColor = EmeraldPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.UploadFile,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("UPLOAD VIDEO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (notificationTitle.isBlank() || notificationBody.isBlank()) {
                                            Toast.makeText(context, "Please enter both Title and Body", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        viewModel.insertNotification(
                                            NotificationEntity(
                                                id = UUID.randomUUID().toString(),
                                                title = notificationTitle,
                                                message = notificationBody,
                                                timestamp = System.currentTimeMillis(),
                                                isRead = false,
                                                type = "guidance",
                                                imageUrl = if (notificationImage.isBlank()) null else notificationImage,
                                                videoUrl = if (notificationVideo.isBlank()) null else notificationVideo
                                            )
                                        )
                                        Toast.makeText(context, "System alert broadcasted successfully!", Toast.LENGTH_LONG).show()
                                        notificationTitle = ""
                                        notificationBody = ""
                                        notificationImage = ""
                                        notificationVideo = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("TRANSMIT BROADCAST", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    1 -> {
                        Text(
                            text = "MANAGE REASONING INTEGRATION KEYS",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                                .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, RoundedCornerShape(24.dp))
                                .padding(24.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedTextField(
                                    value = apiKeyInput,
                                    onValueChange = { apiKeyInput = it },
                                    label = { Text("OpenRouter API Key", color = TextSecondaryGreen) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        if (apiKeyInput.isBlank()) {
                                            Toast.makeText(context, "API Key cannot be blank", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        Config.setOpenRouterKey(context, apiKeyInput.trim())
                                        Toast.makeText(context, "OpenRouter API Key updated and persistent!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("SYNCHRONIZE OPENROUTER KEY", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                HorizontalDivider(color = if (isEmerald) GlassGreenBorder else GlassBorder, thickness = 1.dp)

                                OutlinedTextField(
                                    value = elevenLabsApiKeyInput,
                                    onValueChange = { elevenLabsApiKeyInput = it },
                                    label = { Text("ElevenLabs API Key", color = TextSecondaryGreen) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        if (elevenLabsApiKeyInput.isBlank()) {
                                            Toast.makeText(context, "ElevenLabs Key cannot be blank", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        Config.setElevenLabsKey(context, elevenLabsApiKeyInput.trim())
                                        Toast.makeText(context, "ElevenLabs API Key updated and persistent!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("SYNCHRONIZE ELEVENLABS KEY", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    2 -> {
                        Text(
                            text = "USER ACCESS DIRECTORY",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = userSearchQuery,
                            onValueChange = { userSearchQuery = it },
                            placeholder = { Text("Search system users...", color = TextSecondaryGreen) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldIconColor) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val filteredUsers = mockUsers.filter { it.first.contains(userSearchQuery, ignoreCase = true) }
                            if (filteredUsers.isEmpty()) {
                                Text(
                                    text = "No system users found.",
                                    color = TextSecondaryGreen,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                                )
                            } else {
                                filteredUsers.forEach { userPair ->
                                    val (email, isBanned) = userPair
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                                            .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, RoundedCornerShape(16.dp))
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = email,
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (isBanned) "STATUS: BANNED / BLOCK ACTIVE" else "STATUS: ACTIVE / ALIGNED",
                                                color = if (isBanned) MaterialTheme.colorScheme.error else EmeraldPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                val index = mockUsers.indexOfFirst { it.first == email }
                                                if (index != -1) {
                                                    mockUsers[index] = Pair(email, !isBanned)
                                                    val stateStr = if (!isBanned) "banned" else "restored"
                                                    Toast.makeText(context, "User $email has been $stateStr!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isBanned) EmeraldPrimary else MaterialTheme.colorScheme.error
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (isBanned) "UNBAN" else "BAN",
                                                color = Color.Black,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        Text(
                            text = "BRAND IDENTITY SETTINGS",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                                .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, RoundedCornerShape(24.dp))
                                .padding(24.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedTextField(
                                    value = appNameInput,
                                    onValueChange = { appNameInput = it },
                                    label = { Text("Application Display Name", color = TextSecondaryGreen) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        if (appNameInput.isBlank()) {
                                            Toast.makeText(context, "Display Name cannot be empty", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        Config.setAppName(context, appNameInput.trim())
                                        Toast.makeText(context, "Application branded name synchronized successfully!", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("SYNCHRONIZE BRAND", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
