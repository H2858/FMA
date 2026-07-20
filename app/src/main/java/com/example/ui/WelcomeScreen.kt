package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.flow.StateFlow

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val currentLanguage by LanguageHelper.currentLanguage.collectAsState()

    LaunchedEffect(Unit) {
        visible = true
    }

    // High fidelity animated particles in the background
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val translationY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 150f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "translationY"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Professional generated background image representation
        Image(
            painter = painterResource(id = R.drawable.img_welcome_bg_1784137450158),
            contentDescription = "Cosmic alignment background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Semi-transparent overlay to ensure maximum contrast and premium obsidian depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color(0xFF030D08).copy(alpha = 0.90f)
                        )
                    )
                )
        )

        // Live holographic interactive particles animated over the background art
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Render a series of glowing node coordinates with dynamic offsets
            val points = listOf(
                Offset(width * 0.15f, height * 0.25f + translationY * 0.3f),
                Offset(width * 0.85f, height * 0.15f - translationY * 0.2f),
                Offset(width * 0.35f, height * 0.65f + translationY * 0.5f),
                Offset(width * 0.75f, height * 0.55f - translationY * 0.4f),
                Offset(width * 0.50f, height * 0.35f + translationY * 0.1f),
                Offset(width * 0.20f, height * 0.80f - translationY * 0.6f),
                Offset(width * 0.80f, height * 0.85f + translationY * 0.2f)
            )

            points.forEach { offset ->
                drawCircle(
                    color = EmeraldPrimary,
                    radius = 5.dp.toPx() * (pulseAlpha + 0.5f),
                    center = offset,
                    alpha = pulseAlpha * 0.6f
                )
                // Draw delicate high-tech aura lines connecting them
                drawCircle(
                    color = EmeraldPrimary,
                    radius = 12.dp.toPx() * (pulseAlpha + 0.5f),
                    center = offset,
                    alpha = pulseAlpha * 0.2f
                )
            }
        }

        // Floating pulsing neon central aura glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .align(Alignment.Center)
                .blur(110.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            EmeraldPrimary.copy(alpha = 0.18f * (pulseAlpha + 0.5f)),
                            Color.Transparent
                        )
                    )
                )
        )

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(1400)) + slideInVertically(
                initialOffsetY = { 100 },
                animationSpec = tween(1400)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Majestic Icon Aura Core
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(GlassGreenOverlay)
                        .border(
                            1.5.dp,
                            Brush.verticalGradient(listOf(EmeraldPrimary, Color.Transparent)),
                            RoundedCornerShape(36.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI Coach Logo",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = LanguageHelper.translate("app_title"),
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = LanguageHelper.translate("welcome_desc"),
                    color = TextSecondaryGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(54.dp))

                Button(
                    onClick = onGetStarted,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("get_started_button"),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = LanguageHelper.translate("get_started"),
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Get Started",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
