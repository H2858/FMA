package com.example.ui

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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    val isEmerald by viewModel.isEmeraldTheme.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var showLanguageDropdown by remember { mutableStateOf(false) }

    fun text(key: String): String = LanguageHelper.translate(key)

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
            // Header
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
                        .testTag("settings_back_btn")
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
                        text = text("settings").uppercase(),
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    var secretClickCount by remember { mutableStateOf(0) }
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Text(
                        text = "SYSTEM PREFERENCES ONLY",
                        color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.clickable {
                            secretClickCount++
                            if (secretClickCount >= 5) {
                                viewModel.toggleAdminForce()
                                secretClickCount = 0
                                android.widget.Toast.makeText(context, "Admin rights toggled!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // 1. Theme Toggle (Dark/Light mode)
                Text(
                    text = "VISUAL MODE",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                        .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, RoundedCornerShape(20.dp))
                        .clickable { viewModel.toggleDarkMode() }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Theme Icon",
                                tint = EmeraldIconColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isDarkMode) text("dark_mode") else text("light_mode"),
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Toggle system visual illumination theme.",
                                    color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldIconColor,
                                checkedTrackColor = EmeraldDark,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // CHAT Mode Toggle
                val isAiChatMode by viewModel.isAiChatMode.collectAsState()
                Text(
                    text = if (currentLanguage == AppLanguage.ARABIC) "نمط التطبيق" else "APPLICATION MODE",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                        .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, RoundedCornerShape(20.dp))
                        .clickable { viewModel.toggleAiChatMode() }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = "CHAT Mode Icon",
                                tint = EmeraldIconColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isAiChatMode) "CHAT" else (if (currentLanguage == AppLanguage.ARABIC) "المدرب الذكي" else "Smart Coach Mode"),
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (currentLanguage == AppLanguage.ARABIC) 
                                        "التحويل بين المحادثة العامة (CHAT) وتطبيق الكوتش الذكي." 
                                        else "Switch between general AI conversation (CHAT) and coaching personas.",
                                    color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = isAiChatMode,
                            onCheckedChange = { viewModel.toggleAiChatMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldIconColor,
                                checkedTrackColor = EmeraldDark,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.testTag("ai_chat_mode_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Language Selection
                Text(
                    text = "SYSTEM LANGUAGE",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                        .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, RoundedCornerShape(20.dp))
                        .clickable { showLanguageDropdown = true }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language Selector",
                                tint = EmeraldIconColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = when (currentLanguage) {
                                        AppLanguage.ENGLISH -> "English"
                                        AppLanguage.ARABIC -> "العربية (Arabic)"
                                        AppLanguage.FRENCH -> "Français"
                                        AppLanguage.SPANISH -> "Español"
                                        AppLanguage.PORTUGUESE -> "Português"
                                        AppLanguage.HINDI -> "हिन्दी"
                                        AppLanguage.GERMAN -> "Deutsch"
                                    },
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Switch current localized UI texts.",
                                    color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Box {
                            Button(
                                onClick = { showLanguageDropdown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldButtonColor),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("language_button_trigger")
                            ) {
                                Text(
                                    text = "CHANGE",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            DropdownMenu(
                                expanded = showLanguageDropdown,
                                onDismissRequest = { showLanguageDropdown = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("العربية", color = TextPrimary) },
                                    onClick = {
                                        viewModel.setLanguage(AppLanguage.ARABIC)
                                        showLanguageDropdown = false
                                    },
                                    modifier = Modifier.testTag("lang_ar")
                                )
                                DropdownMenuItem(
                                    text = { Text("English", color = TextPrimary) },
                                    onClick = {
                                        viewModel.setLanguage(AppLanguage.ENGLISH)
                                        showLanguageDropdown = false
                                    },
                                    modifier = Modifier.testTag("lang_en")
                                )
                                DropdownMenuItem(
                                    text = { Text("Français", color = TextPrimary) },
                                    onClick = {
                                        viewModel.setLanguage(AppLanguage.FRENCH)
                                        showLanguageDropdown = false
                                    },
                                    modifier = Modifier.testTag("lang_fr")
                                )
                                DropdownMenuItem(
                                    text = { Text("Español", color = TextPrimary) },
                                    onClick = {
                                        viewModel.setLanguage(AppLanguage.SPANISH)
                                        showLanguageDropdown = false
                                    },
                                    modifier = Modifier.testTag("lang_es")
                                )
                            }
                        }
                    }
                }

                val isAdminReal by viewModel.isAdminReal.collectAsState()

                if (isAdminReal) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "ADMINISTRATIVE CONTROL",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                            .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, RoundedCornerShape(20.dp))
                            .clickable { onNavigateToAdmin() }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Panel",
                                    tint = EmeraldIconColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Admin Dashboard",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Manage API keys, user privileges, alerts & brand.",
                                        color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Navigate",
                                tint = EmeraldIconColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
