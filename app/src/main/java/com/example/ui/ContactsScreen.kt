package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GeminiApiService
import com.example.ui.theme.*

@Composable
fun ContactsScreen(viewModel: MainViewModel, onNavigateToPricing: () -> Unit) {
    val currentPersona by viewModel.currentPersona.collectAsState()
    val coachingLog by viewModel.coachingLog.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val isEmerald by viewModel.isEmeraldTheme.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    fun text(key: String): String = LanguageHelper.translate(key)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp)
    ) {
        // Title
        Text(
            text = text("personas_history_title"),
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
        )
        Text(
            text = text("personas_history_subtitle"),
            color = if (isEmerald) TextSecondaryGreen else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Persona Selection Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GeminiApiService.CoachPersona.values().forEach { persona ->
                val isSelected = currentPersona == persona
                // Spartan is locked for Free users to drive upgrade conversions
                val isLocked = persona == GeminiApiService.CoachPersona.SPARTAN && !isPremium

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else (if (isEmerald) GlassGreenOverlay else GlassOverlay))
                        .border(
                            1.5.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            if (isLocked) {
                                onNavigateToPricing()
                            } else {
                                viewModel.setPersona(persona)
                            }
                        }
                        .padding(12.dp)
                        .testTag("persona_box_${persona.name}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (persona) {
                                    GeminiApiService.CoachPersona.SPARTAN -> Icons.Default.ElectricBolt
                                    GeminiApiService.CoachPersona.MENTOR -> Icons.Default.Psychology
                                    GeminiApiService.CoachPersona.PARTNER -> Icons.Default.Star
                                    GeminiApiService.CoachPersona.GENERAL -> Icons.Default.History
                                },
                                contentDescription = persona.title,
                                tint = if (isSelected) (if (isEmerald) Color.Black else Color.White) else (if (isLocked) (if (isEmerald) TextMutedGreen else TextMuted) else TextPrimary),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = persona.title,
                            color = if (isSelected) TextPrimary else (if (isEmerald) TextSecondaryGreen else TextSecondary),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (isLocked) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.tertiary)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "LOCKED",
                                    color = if (isEmerald) Color.Black else Color.White,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // History Log Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text("aligned_logs_title"),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (coachingLog.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text("aligned_logs_empty"),
                        color = if (isEmerald) TextMutedGreen else TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(coachingLog) { log ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                                .border(
                                    1.dp,
                                    if (isEmerald) GlassGreenBorder else GlassBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ARCHIVE: ${log.persona.uppercase()}",
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp)),
                                        color = if (isEmerald) TextMutedGreen else TextMuted,
                                        fontSize = 10.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Q: ${log.userMessage}",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = log.aiResponse,
                                    color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
