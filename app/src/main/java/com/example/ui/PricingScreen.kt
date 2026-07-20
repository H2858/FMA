package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun PricingScreen(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val isPremium by viewModel.isPremium.collectAsState()
    val isEmerald by viewModel.isEmeraldTheme.collectAsState()
    var selectedPlanIndex by remember { mutableStateOf(1) } // Default to annual

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Glowing background accents
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .align(Alignment.TopCenter)
                .blur(110.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dismiss Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                        .border(
                            1.dp,
                            if (isEmerald) GlassGreenBorder else GlassBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .testTag("pricing_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Wall",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "TRANSCEND FRICTION",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "Unlock professional-grade intelligence alignment and custom ElevenLabs voice models.",
                color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp)
            )

            // Features Grid
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureRow(title = "Proactive Coaching AI Engine", desc = "Continuous mental and task tracking sync", isEmerald = isEmerald)
                FeatureRow(title = "Thinking-Optimized Pro Models", desc = "Leverages advanced AI high reasoning core", isEmerald = isEmerald)
                FeatureRow(title = "Premium Voice synthesis", desc = "Fully immersive, authentic verbal dialogue", isEmerald = isEmerald)
                FeatureRow(title = "Spartan accountability persona", desc = "No-holds-barred tactical discipline alignment", isEmerald = isEmerald)
                FeatureRow(title = "Real-time Cloud Syncing", desc = "Instant secured cloud backup of tasks", isEmerald = isEmerald)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Pricing Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Plan 1: Monthly
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selectedPlanIndex == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else (if (isEmerald) GlassGreenOverlay else GlassOverlay))
                        .border(
                            2.dp,
                            if (selectedPlanIndex == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedPlanIndex = 0 }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "MONTHLY",
                            color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("$2.99", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text("per month", color = if (isEmerald) TextMutedGreen else TextMuted, fontSize = 11.sp)
                    }
                }

                // Plan 2: Annual (15% Off)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selectedPlanIndex == 1) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else (if (isEmerald) GlassGreenOverlay else GlassOverlay))
                        .border(
                            2.dp,
                            if (selectedPlanIndex == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedPlanIndex = 1 }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "ANNUAL",
                                color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.tertiary)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "-15%",
                                    color = if (isEmerald) Color.Black else Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("$30.49", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text("billed yearly", color = if (isEmerald) TextMutedGreen else TextMuted, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Subscribe Button
            Button(
                onClick = {
                    viewModel.upgradeToPremium(if (selectedPlanIndex == 0) "Monthly" else "Yearly")
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("subscribe_action_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isEmerald) Color.Black else TextPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isPremium) "MANAGE CURRENT TIERS" else "ACTIVATE PRO SYNC",
                        color = if (isEmerald) Color.Black else TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Terms & Privacy
            Text(
                text = "Restore Purchase  •  Terms of Alignment  •  Privacy Code",
                color = if (isEmerald) TextMutedGreen else TextMuted,
                fontSize = 11.sp,
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .clickable { }
            )
        }
    }
}

@Composable
fun FeatureRow(title: String, desc: String, isEmerald: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                text = desc,
                color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}
