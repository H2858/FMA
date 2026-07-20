package com.example.ui

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToPricing: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()

    val isPremium by viewModel.isPremium.collectAsState()
    val isEmerald by viewModel.isEmeraldTheme.collectAsState()

    var showBiometricRegisterDialog by remember { mutableStateOf(false) }
    var showLockedAvatarDialog by remember { mutableStateOf(false) }
    var biometricRegistering by remember { mutableStateOf(false) }
    var showAvatarSelector by remember { mutableStateOf(false) }
    var tempSelectedAvatarIndex by remember { mutableStateOf(userProfile?.avatarIndex ?: 0) }

    fun text(key: String): String = LanguageHelper.translate(key)

    val scrollState = rememberScrollState()

    // 30 custom aura representation themes/colors
    val avatarColors = remember {
        List(30) { i ->
            when {
                i < 5 -> listOf(EmeraldPrimary, EmeraldGlow)
                i < 10 -> listOf(EmeraldLight, EmeraldPrimary)
                i < 15 -> listOf(Color(0xFF00E5FF), Color(0xFF00838F)) // Ice Blue
                i < 20 -> listOf(Color(0xFFFFD600), Color(0xFFFF8F00)) // Solar Gold
                i < 25 -> listOf(Color(0xFFFF1744), Color(0xFFB71C1C)) // Crimson Flare
                else -> listOf(Color(0xFFE040FB), Color(0xFF6A1B9A)) // Indigo Portal
            }
        }
    }

    // High-fidelity generated cosmic avatar assets
    val avatarDrawables = remember {
        listOf(
            com.example.R.drawable.avatar_emerald_1784272351852,
            com.example.R.drawable.avatar_sapphire_1784272365835,
            com.example.R.drawable.avatar_gold_1784272380462,
            com.example.R.drawable.avatar_ruby_1784272396822,
            com.example.R.drawable.avatar_amethyst_1784272413835,
            com.example.R.drawable.avatar_oracle_1784272428905
        )
    }

    // Direct high-fidelity remote avatar URLs provided by user
    val avatarUrls = remember {
        listOf(
            "https://i.postimg.cc/7CFtrRZw/1000011720-jpg-2K-202607150141.jpg",
            "https://i.postimg.cc/r05nV9Ds/Avatar-holding-coffee-black-suit-202607142258.jpg",
            "https://i.postimg.cc/9Ry8mBDM/Avatar-in-tennis-clothes-2K-202607142346.jpg",
            "https://i.postimg.cc/dkGHs9hh/Avatar-wearing-Algerian-burnous-2K-202607142311.jpg",
            "https://i.postimg.cc/H8HSV7M8/Avatar-wearing-Algerian-caftan-fez-202607142306.jpg",
            "https://i.postimg.cc/hzVyD0Xt/Avatar-wearing-Algerian-jersey-2K-202607142243.jpg",
            "https://i.postimg.cc/vxMPc6nV/Avatar-wearing-golf-outfit-2K-202607142343.jpg",
            "https://i.postimg.cc/K3F9RgTr/Avatar-wearing-Indian-clothes-2K-202607142250.jpg",
            "https://i.postimg.cc/LgRynZPb/Avatar-wearing-Japanese-clothes-2K-202607142316.jpg",
            "https://i.postimg.cc/2LmHVBWH/Avatar-wearing-Libyan-clothes-2K-202607142308.jpg",
            "https://i.postimg.cc/MfD9RK0F/Avatar-wearing-Libyan-clothes-2K-202607142309.jpg",
            "https://i.postimg.cc/y3PL9Yhw/Avatar-wearing-Mexican-clothing-2K-202607142314.jpg",
            "https://i.postimg.cc/7JVK7YMx/Avatar-wearing-Omani-clothing-2K-202607142337.jpg",
            "https://i.postimg.cc/30BtpJC3/Avatar-wearing-Pakistani-clothes-2K-202607142301.jpg",
            "https://i.postimg.cc/V581jGBb/Avatar-wearing-Russian-clothes-2K-202607142318.jpg",
            "https://i.postimg.cc/xq2YKxyR/Avatar-wearing-Saudi-clothes-2K-202607142244.jpg",
            "https://i.postimg.cc/7bkDgsMJ/Avatar-wearing-Saudi-clothes-2K-202607142244-(1).jpg",
            "https://i.postimg.cc/s14zKYTF/Avatar-wearing-Spanish-clothing-2K-202607142312.jpg",
            "https://i.postimg.cc/LhpRzCBt/Avatar-wearing-Spanish-clothing-2K-202607142312-(1).jpg",
            "https://i.postimg.cc/yWyKnmvb/Avatar-wearing-summer-outfit-hol-202607142330.jpg",
            "https://i.postimg.cc/1fRQd8dz/Avatar-wearing-sun-hat-and-202607142303.jpg",
            "https://i.postimg.cc/Sjf4V6gR/Avatar-wearing-Turkish-shirt-2K-202607142246.jpg",
            "https://i.postimg.cc/gncW7x7X/Avatar-with-Chinese-clothing-2K-202607142238.jpg",
            "https://i.postimg.cc/mtZsnPn7/Avatar-with-curly-hair-sportswear-202607142234.jpg",
            "https://i.postimg.cc/fJWNPVP5/Avatar-with-dark-skin-white-202607142240.jpg",
            "https://i.postimg.cc/gncW7x7y/Create-faceless-avatars-for-app-202607142232.jpg",
            "https://i.postimg.cc/GHMCnc0j/Haaland-hairstyle-black-kit-avatar-202607142340.jpg",
            "https://i.postimg.cc/BXh0fqWV/Male-avatar-wearing-headphones-t-202607142255.jpg",
            "https://i.postimg.cc/HVSg1pGZ/Man-wearing-white-suit-2K-202607142328.jpg",
            "https://i.postimg.cc/kBjmPnr0/Woman-wearing-hijab-abaya-2K-202607142252.jpg"
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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        SoundManager.playClick()
                        onBack()
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                        .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, CircleShape)
                        .testTag("profile_back_btn")
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
                        text = text("profile").uppercase(),
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "YOUR SPIRITUAL IDENTITY NODE",
                        color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Standard elegant top corner logout button
                IconButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                        .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, CircleShape)
                        .testTag("profile_top_logout_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = text("logout"),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Profile Header block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                        .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, RoundedCornerShape(24.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Large majestic styled interactive Avatar
                        val selectedAvatarIndex = userProfile?.avatarIndex ?: 0
                        val selectedColors = avatarColors.getOrElse(selectedAvatarIndex) { listOf(EmeraldPrimary, EmeraldGlow) }
                        val avatarResId = avatarDrawables[selectedAvatarIndex % avatarDrawables.size]

                        Box(
                            modifier = Modifier
                                .size(115.dp)
                                .clip(CircleShape)
                                .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                                .border(2.dp, if (isEmerald) EmeraldPrimary else MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable {
                                    SoundManager.playClick()
                                    tempSelectedAvatarIndex = selectedAvatarIndex
                                    showAvatarSelector = true
                                }
                                .padding(4.dp)
                                .clip(CircleShape)
                        ) {
                            AsyncImage(
                                model = avatarUrls[selectedAvatarIndex % avatarUrls.size],
                                contentDescription = "Active Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = avatarResId),
                                placeholder = painterResource(id = avatarResId)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (viewModel.currentLanguage.value.code == "ar") "انقر لتعديل الصورة" else "Tap to edit picture",
                            color = if (isEmerald) EmeraldPrimary else MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                SoundManager.playClick()
                                tempSelectedAvatarIndex = selectedAvatarIndex
                                showAvatarSelector = true
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = userProfile?.name ?: "Seeker",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            if (userProfile?.isVerified == true) {
                                Spacer(modifier = Modifier.width(6.dp))
                                val plan = userProfile?.planType ?: "Monthly"
                                val badgeColor = if (plan.lowercase() == "yearly" || plan.lowercase() == "annual") {
                                    Color(0xFFFFD700) // Gold
                                } else {
                                    Color(0xFF2196F3) // Blue
                                }
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified Account",
                                    tint = badgeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Text(
                            text = userProfile?.email ?: "local_sandbox@focusmate.ai",
                            color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${userProfile?.xpPoints ?: 0}",
                                    color = EmeraldPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                               )
                                Text(
                                    text = text("xp_points"),
                                    color = if (isEmerald) TextMutedGreen else TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val currentXp = userProfile?.xpPoints ?: 0
                                val computedRank = when {
                                    currentXp >= 2000 -> "#1"
                                    currentXp >= 1000 -> "#2"
                                    currentXp >= 500 -> "#3"
                                    currentXp >= 150 -> "#4"
                                    else -> "#5"
                                }
                                Text(
                                    text = computedRank,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = text("rank"),
                                    color = if (isEmerald) TextMutedGreen else TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isPremium) "Premium" else "Free",
                                    color = if (isPremium) EmeraldPrimary else Color.Gray,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "TIER",
                                    color = if (isEmerald) TextMutedGreen else TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Biometric Login controller
                Text(
                    text = text("biometric_login").uppercase(),
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
                        .clickable { showBiometricRegisterDialog = true }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Fingerprint Setup",
                                tint = EmeraldIconColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (userProfile?.isBiometricEnabled == true) text("disable_biometric") else text("enable_biometric"),
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = text("biometric_desc"),
                                    color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = userProfile?.isBiometricEnabled == true,
                            onCheckedChange = { showBiometricRegisterDialog = true },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldIconColor,
                                checkedTrackColor = EmeraldDark,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.DarkGray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Account Verification controller
                Text(
                    text = text("verification_title").uppercase(),
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
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            val badgeColor = if (userProfile?.isVerified == true) {
                                if (userProfile?.planType?.lowercase() == "yearly" || userProfile?.planType?.lowercase() == "annual") {
                                    Color(0xFFFFD700) // Gold
                                } else {
                                    Color(0xFF2196F3) // Blue
                                }
                            } else {
                                Color.Gray
                            }
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verification Status",
                                tint = badgeColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                val verText = if (userProfile?.isVerified == true) {
                                    if (userProfile?.planType?.lowercase() == "yearly" || userProfile?.planType?.lowercase() == "annual") {
                                        text("verification_yearly")
                                    } else {
                                        text("verification_monthly")
                                    }
                                } else {
                                    text("verification_disabled")
                                }
                                Text(
                                    text = verText,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = text("verification_desc"),
                                    color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        val badgeText = if (userProfile?.isVerified == true) {
                            if (userProfile?.planType?.lowercase() == "yearly" || userProfile?.planType?.lowercase() == "annual") {
                                if (viewModel.currentLanguage.value.code == "ar") "توثيق ذهبي" else "Gold Verified"
                            } else {
                                if (viewModel.currentLanguage.value.code == "ar") "توثيق أزرق" else "Blue Verified"
                            }
                        } else {
                            if (viewModel.currentLanguage.value.code == "ar") "غير موثق" else "Unverified"
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (userProfile?.isVerified == true) {
                                if (userProfile?.planType?.lowercase() == "yearly" || userProfile?.planType?.lowercase() == "annual") {
                                    Color(0xFFFFD700).copy(alpha = 0.15f)
                                } else {
                                    Color(0xFF2196F3).copy(alpha = 0.15f)
                                }
                            } else {
                                Color.Gray.copy(alpha = 0.15f)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (userProfile?.isVerified == true) {
                                    if (userProfile?.planType?.lowercase() == "yearly" || userProfile?.planType?.lowercase() == "annual") {
                                        Color(0xFFFFD700)
                                    } else {
                                        Color(0xFF2196F3)
                                    }
                                } else {
                                    Color.Gray
                                }
                            ),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = if (userProfile?.isVerified == true) {
                                    if (userProfile?.planType?.lowercase() == "yearly" || userProfile?.planType?.lowercase() == "annual") {
                                        Color(0xFFFFD700)
                                    } else {
                                        Color(0xFF2196F3)
                                    }
                                } else {
                                    Color.LightGray
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Avatars Grid gallery replaced by a luxurious promotional customization card
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEmerald) GlassGreenOverlay else GlassOverlay
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            SoundManager.playClick()
                            tempSelectedAvatarIndex = userProfile?.avatarIndex ?: 0
                            showAvatarSelector = true
                        }
                        .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (isEmerald) EmeraldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Palette",
                                tint = if (isEmerald) EmeraldIconColor else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = if (viewModel.currentLanguage.value.code == "ar") "استوديو الرمزيات الفاخر" else "Premium Avatar Studio",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (viewModel.currentLanguage.value.code == "ar") "اختر من بين ٣٠ رمزية ثلاثية الأبعاد وعصرية بالكامل" else "Choose from 30 high-fidelity 3D designer avatars.",
                                color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Chevron Right",
                            tint = if (isEmerald) EmeraldIconColor else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(64.dp))
            }
        }

        // Dialogs
        if (showBiometricRegisterDialog) {
            val currentProfile = userProfile
            val isEnrolled = currentProfile?.isBiometricEnabled == true

            AlertDialog(
                onDismissRequest = { showBiometricRegisterDialog = false },
                title = {
                    Text(
                        text = if (isEnrolled) "DISABLE PASSKEY SECURITY" else "ALIGN BIOMETRIC PASSKEY",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isEnrolled) "Are you sure you want to disable biometric fingerprint authentication from your device node?"
                            else "Secure your daily focus sessions. Calibrate your device's biometric fingerprint scanner to align your login credentials with supreme physical immediacy.",
                            color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (!isEnrolled) {
                            val infinitePulse = rememberInfiniteTransition(label = "pulse")
                            val dynamicScale by infinitePulse.animateFloat(
                                initialValue = 0.9f,
                                targetValue = 1.2f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scanner_scale"
                            )

                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .scale(if (biometricRegistering) dynamicScale else 1.0f)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.15f))
                                    .border(2.dp, EmeraldPrimary, CircleShape)
                                    .clickable {
                                        val activity = context as? androidx.fragment.app.FragmentActivity
                                        if (activity != null && BiometricHelper.isBiometricAvailable(activity)) {
                                            biometricRegistering = true
                                            BiometricHelper.showSystemBiometricPrompt(
                                                activity = activity,
                                                title = "Align Biometric Passkey",
                                                subtitle = "Calibrate your device's biometric sensor",
                                                onSuccess = {
                                                    viewModel.setBiometricEnabled(true)
                                                    showBiometricRegisterDialog = false
                                                    biometricRegistering = false
                                                    Toast.makeText(context, "Biometric alignment successful!", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    biometricRegistering = false
                                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        } else {
                                            biometricRegistering = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Fingerprint Sensor",
                                    tint = EmeraldIconColor,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (biometricRegistering) "CALIBRATING SCANNER..." else "TAP SCANNER TO INITIALIZE",
                                color = if (biometricRegistering) EmeraldPrimary else (if (isEmerald) TextSecondaryGreen else TextSecondary),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    if (isEnrolled) {
                        Button(
                            onClick = {
                                viewModel.setBiometricEnabled(false)
                                showBiometricRegisterDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("DEACTIVATE SECURE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.setBiometricEnabled(true)
                                showBiometricRegisterDialog = false
                                biometricRegistering = false
                            },
                            enabled = biometricRegistering,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldButtonColor)
                        ) {
                            Text("SAVE PASSKEY", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showBiometricRegisterDialog = false
                        biometricRegistering = false
                    }) {
                        Text(text("cancel"), color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, RoundedCornerShape(24.dp))
            )
        }

        if (showLockedAvatarDialog) {
            AlertDialog(
                onDismissRequest = { showLockedAvatarDialog = false },
                title = {
                    Text(
                        text = "AURA REPRESENTATION LOCKED",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                },
                text = {
                    Text(
                        text = "This premium high-fidelity avatar is locked behind the subscription system. Upgrade your tier to unlock all 20 holographic avatars and premium vocal synthetic coaching styles!",
                        color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLockedAvatarDialog = false
                            onNavigateToPricing()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldButtonColor)
                    ) {
                        Text(text("upgrade").uppercase(), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLockedAvatarDialog = false }) {
                        Text(text("cancel"), color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, RoundedCornerShape(24.dp))
            )
        }

        if (showAvatarSelector) {
            AvatarSelectorSubScreen(
                currentLang = viewModel.currentLanguage.value.code,
                avatarUrls = avatarUrls,
                avatarDrawables = avatarDrawables,
                isPremium = isPremium,
                isEmerald = isEmerald,
                tempSelectedAvatarIndex = tempSelectedAvatarIndex,
                onAvatarSelected = { tempSelectedAvatarIndex = it },
                onDismiss = { showAvatarSelector = false },
                onSave = {
                    viewModel.setAvatarIndex(tempSelectedAvatarIndex)
                    showAvatarSelector = false
                },
                onUpgrade = {
                    showAvatarSelector = false
                    showLockedAvatarDialog = true
                }
            )
        }
    }
}

@Composable
fun AvatarSelectorSubScreen(
    currentLang: String,
    avatarUrls: List<String>,
    avatarDrawables: List<Int>,
    isPremium: Boolean,
    isEmerald: Boolean,
    tempSelectedAvatarIndex: Int,
    onAvatarSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onUpgrade: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) }
    val tabs = remember {
        if (currentLang == "ar") {
            listOf("AVATARS")
        } else {
            listOf("AVATARS")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        SoundManager.playClick()
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (MaterialTheme.colorScheme.isLight()) Color(0xFFE5E7EB) else Color(0xFF161616))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = if (currentLang == "ar") "الصورة الشخصية" else "Profile Picture",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(1.2f)) // slight offset to balance back btn
            }

            // Top Selected Large Circle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(if (MaterialTheme.colorScheme.isLight()) Color(0xFFE5E7EB) else Color(0xFF121212))
                        .border(2.5.dp, Color(0xFF8C5E35), CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                ) {
                    val avatarResId = avatarDrawables[tempSelectedAvatarIndex % avatarDrawables.size]
                    AsyncImage(
                        model = avatarUrls[tempSelectedAvatarIndex % avatarUrls.size],
                        contentDescription = "Active Avatar Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = avatarResId),
                        placeholder = painterResource(id = avatarResId)
                    )
                }
            }

            // Category Tab Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tabName ->
                    val isTabSelected = activeTab == index
                    Column(
                        modifier = Modifier
                            .clickable {
                                SoundManager.playClick()
                                activeTab = index
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tabName,
                            color = if (isTabSelected) TextPrimary else (if (MaterialTheme.colorScheme.isLight()) Color(0xFF9CA3AF) else Color(0xFF757575)),
                            fontSize = 15.sp,
                            fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Gold Tab Indicator Line
                        Box(
                            modifier = Modifier
                                .height(2.dp)
                                .width(if (isTabSelected) 32.dp else 0.dp)
                                .background(if (isTabSelected) Color(0xFF8C5E35) else Color.Transparent)
                        )
                    }
                }
            }

            // Custom border using Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )

            // Avatar Grid based on Category
            val filteredIndices = remember(activeTab, avatarUrls) {
                (0 until avatarUrls.size).toList()
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                val chunkSize = 4
                val rows = filteredIndices.chunked(chunkSize)

                rows.forEach { rowIndices ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in 0 until chunkSize) {
                            if (i < rowIndices.size) {
                                val index = rowIndices[i]
                                val isSelected = index == tempSelectedAvatarIndex
                                val isLocked = index >= 10 && !isPremium
                                val avatarResId = avatarDrawables[index % avatarDrawables.size]

                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            SoundManager.playClick()
                                            if (isLocked) {
                                                onUpgrade()
                                            } else {
                                                onAvatarSelected(index)
                                            }
                                        }
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(3.dp, Color(0xFF8C5E35), CircleShape)
                                            } else {
                                                Modifier.border(1.dp, if (MaterialTheme.colorScheme.isLight()) Color(0xFFD1D5DB) else Color(0xFF222222), CircleShape)
                                            }
                                        )
                                        .padding(if (isSelected) 3.dp else 0.dp)
                                        .clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = avatarUrls[index],
                                        contentDescription = "Avatar Item $index",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(id = avatarResId),
                                        placeholder = painterResource(id = avatarResId)
                                    )

                                    if (isLocked) {
                                        // Dark sleek glasslock overlay
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.6f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Locked",
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Empty spacer to balance the Row
                                Spacer(modifier = Modifier.size(72.dp))
                            }
                        }
                    }
                }
            }

            // Bottom Save Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Button(
                    onClick = {
                        SoundManager.playClick()
                        onSave()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8C5E35),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = if (currentLang == "ar") "حفظ" else "Save",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
