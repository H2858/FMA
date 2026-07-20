package com.example.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.ui.theme.*

@Composable
fun LeaderboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isEmerald by viewModel.isEmeraldTheme.collectAsState()

    fun text(key: String): String = LanguageHelper.translate(key)

    val userPoints = userProfile?.xpPoints ?: 150
    val userName = userProfile?.name ?: "Seeker"

    // Map points to Duolingo-style Leagues
    // 1. Bronze (نحاسي)  0 - 200 XP
    // 2. Silver (فضي)    201 - 500 XP
    // 3. Gold (ذهبي)     501 - 1000 XP
    // 4. Diamond (الماسي) 1001 - 2000 XP
    // 5. Platinum (بلاتيني) 2001+ XP
    val userLeague = when {
        userPoints >= 2001 -> "Platinum"
        userPoints >= 1001 -> "Diamond"
        userPoints >= 501 -> "Gold"
        userPoints >= 201 -> "Silver"
        else -> "Bronze"
    }

    var selectedLeagueTab by remember { mutableStateOf(userLeague) }

    // League Badge Definition
    data class LeagueInfo(
        val key: String,
        val arabicTitle: String,
        val englishTitle: String,
        val color: Color,
        val secondaryColor: Color,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val xpThresholdText: String
    )

    val leagues = remember {
        listOf(
            LeagueInfo("Bronze", "الدوري النحاسي", "Bronze League", Color(0xFFCD7F32), Color(0xFF8B5A2B), Icons.Default.Shield, "0 - 200 XP"),
            LeagueInfo("Silver", "الدوري الفضي", "Silver League", Color(0xFFC0C0C0), Color(0xFF708090), Icons.Default.WorkspacePremium, "201 - 500 XP"),
            LeagueInfo("Gold", "الدوري الذهبي", "Gold League", Color(0xFFFFD700), Color(0xFFB8860B), Icons.Default.EmojiEvents, "501 - 1000 XP"),
            LeagueInfo("Diamond", "الدوري الماسي", "Diamond League", Color(0xFFB9F2FF), Color(0xFF00BFFF), Icons.Default.Diamond, "1001 - 2000 XP"),
            LeagueInfo("Platinum", "الدوري البلاتيني", "Platinum League", Color(0xFFE5E4E2), Color(0xFF5F9EA0), Icons.Default.MilitaryTech, "2001+ XP")
        )
    }

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

    // Selected League Details
    val currentLeagueInfo = leagues.find { it.key == selectedLeagueTab } ?: leagues[0]

    // Local Data Structure for Leaderboard Players
    data class LeaderboardPlayer(
        val name: String,
        val points: Int,
        val tier: String,
        val isVerified: Boolean = false,
        val planType: String = "Monthly",
        val avatarIndex: Int = 0,
        val isMe: Boolean = false
    )

    // Generate simulated competitors for each league
    val competitors = remember(selectedLeagueTab, userPoints, userName) {
        when (selectedLeagueTab) {
            "Bronze" -> listOf(
                LeaderboardPlayer("Aura Titan 🌪️", 190, "Free", false, "Monthly", 1),
                LeaderboardPlayer("Mind Builder 🧱", 120, "Free", false, "Monthly", 2),
                LeaderboardPlayer("Quantum Seeker 🌌", 80, "Free", false, "Monthly", 3),
                LeaderboardPlayer("Novice Monk 🧘‍♂️", 40, "Free", false, "Monthly", 4)
            )
            "Silver" -> listOf(
                LeaderboardPlayer("Time Master ⏱️", 480, "Premium", true, "Monthly", 5),
                LeaderboardPlayer("Calm Seeker 🌊", 410, "Free", false, "Monthly", 6),
                LeaderboardPlayer("Routine King 👑", 300, "Premium", false, "Monthly", 7),
                LeaderboardPlayer("Breathe Slow 🌬️", 230, "Free", false, "Monthly", 8)
            )
            "Gold" -> listOf(
                LeaderboardPlayer("Deep Work Wizard 🧙‍♂️", 950, "Premium", true, "Yearly", 9),
                LeaderboardPlayer("Cyber Spartan ⚔️", 820, "Free", false, "Monthly", 10),
                LeaderboardPlayer("Aura Sage 🌿", 700, "Premium", true, "Monthly", 11),
                LeaderboardPlayer("Focus Monk 🧘‍♀️", 550, "Free", false, "Monthly", 12)
            )
            "Diamond" -> listOf(
                LeaderboardPlayer("Zephyr Sovereign ⚡", 1950, "Premium", true, "Yearly", 13),
                LeaderboardPlayer("Chronos Keeper ⏱️", 1600, "Free", false, "Monthly", 14),
                LeaderboardPlayer("Discipline Knight 🛡️", 1300, "Premium", true, "Monthly", 15),
                LeaderboardPlayer("Focus Titan 🪐", 1100, "Free", false, "Monthly", 16)
            )
            "Platinum" -> listOf(
                LeaderboardPlayer("Supreme Sovereign 👑", 4200, "Premium", true, "Yearly", 17),
                LeaderboardPlayer("Cyber Sage Pro 🌿", 3200, "Premium", true, "Yearly", 18),
                LeaderboardPlayer("Ethereal Monk 💎", 2800, "Premium", true, "Monthly", 19),
                LeaderboardPlayer("Discipline Overlord 🦾", 2100, "Premium", true, "Monthly", 20)
            )
            else -> emptyList()
        }
    }

    // Merge User if they belong to this league
    val leagueRankings = remember(selectedLeagueTab, userPoints, competitors, userProfile) {
        val list = competitors.toMutableList()
        if (userLeague == selectedLeagueTab) {
            list.add(
                LeaderboardPlayer(
                    name = userName,
                    points = userPoints,
                    tier = if (userProfile?.tier == "Premium") "Premium" else "Free",
                    isVerified = userProfile?.isVerified == true,
                    planType = userProfile?.planType ?: "Monthly",
                    avatarIndex = userProfile?.avatarIndex ?: 0,
                    isMe = true
                )
            )
        }
        list.sortByDescending { it.points }
        list
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
                        .testTag("leaderboard_back_btn")
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
                        text = text("leaderboard").uppercase(),
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "DUOLINGO-STYLE LEAGUES",
                        color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Duolingo-style horizontal scrollable League selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leagues.forEach { league ->
                    val isSelected = selectedLeagueTab == league.key
                    val isUserActiveLeague = userLeague == league.key

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) league.color.copy(alpha = 0.15f)
                                else (if (isEmerald) GlassGreenOverlay else GlassOverlay)
                            )
                            .border(
                                1.5.dp,
                                if (isSelected) league.color
                                else if (isUserActiveLeague) league.color.copy(alpha = 0.4f)
                                else (if (isEmerald) GlassGreenBorder else GlassBorder),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                SoundManager.playClick()
                                selectedLeagueTab = league.key
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag("league_tab_${league.key}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = league.icon,
                                contentDescription = league.englishTitle,
                                tint = if (isSelected) league.color else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = league.key.uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) league.color else Color.Gray,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Large visual banner for selected League
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(currentLeagueInfo.secondaryColor.copy(alpha = 0.4f), MaterialTheme.colorScheme.background)
                            )
                        )
                        .border(1.dp, currentLeagueInfo.color.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val currentLanguage by viewModel.currentLanguage.collectAsState()
                            Text(
                                text = if (currentLanguage.code == "ar") currentLeagueInfo.arabicTitle else currentLeagueInfo.englishTitle.uppercase(),
                                color = currentLeagueInfo.color,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Requirement: ${currentLeagueInfo.xpThresholdText}",
                                color = if (isEmerald) TextSecondaryGreen else TextSecondary,
                                fontSize = 11.sp
                            )

                            if (userLeague == selectedLeagueTab) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(currentLeagueInfo.color.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "YOUR ACTIVE LEAGUE",
                                        color = currentLeagueInfo.color,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = currentLeagueInfo.icon,
                            contentDescription = null,
                            tint = currentLeagueInfo.color,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // XP Promotion Target Indicator (if not Platinum yet)
                if (userLeague != "Platinum") {
                    val nextLeagueInfo = leagues[leagues.indexOfFirst { it.key == userLeague } + 1]
                    val requiredXp = when (nextLeagueInfo.key) {
                        "Silver" -> 201
                        "Gold" -> 501
                        "Diamond" -> 1001
                        "Platinum" -> 2001
                        else -> 201
                    }
                    val xpProgress = (userPoints.toFloat() / requiredXp).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                            .border(1.dp, if (isEmerald) GlassGreenBorder else GlassBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Promotion to ${nextLeagueInfo.englishTitle}",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$userPoints / $requiredXp XP",
                                    color = nextLeagueInfo.color,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { xpProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = nextLeagueInfo.color,
                                trackColor = Color.DarkGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Rankings Title
                Text(
                    text = "LEAGUE RANKINGS",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Render players inside the current league
                leagueRankings.forEachIndexed { idx, player ->
                    val rank = idx + 1
                    val name = player.name
                    val points = player.points
                    val tier = player.tier
                    val isMe = player.isMe
                    val isVerified = player.isVerified
                    val planType = player.planType
                    val avatarIndex = player.avatarIndex

                    val rankColor = when (rank) {
                        1 -> currentLeagueInfo.color // League Gold/Color Accent
                        2 -> currentLeagueInfo.color.copy(alpha = 0.8f)
                        3 -> currentLeagueInfo.color.copy(alpha = 0.6f)
                        else -> if (isEmerald) TextSecondaryGreen else TextSecondary
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isMe) currentLeagueInfo.color.copy(alpha = 0.12f)
                                else (if (isEmerald) GlassGreenOverlay else GlassOverlay)
                            )
                            .border(
                                1.dp,
                                if (isMe) currentLeagueInfo.color.copy(alpha = 0.5f)
                                else (if (isEmerald) GlassGreenBorder else GlassBorder),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Rank Circle indicator
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(if (rank <= 3) rankColor.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(1.dp, rankColor.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$rank",
                                        color = rankColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Avatar Image
                                val nodeAvatarRes = avatarDrawables[avatarIndex % avatarDrawables.size]
                                val avatarUrl = avatarUrls.getOrNull(avatarIndex)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, if (isMe) currentLeagueInfo.color else Color.Gray.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(id = nodeAvatarRes),
                                        placeholder = painterResource(id = nodeAvatarRes)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isMe) "$name (You)" else name,
                                            color = if (isMe) currentLeagueInfo.color else TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = if (isMe) FontWeight.Bold else FontWeight.Medium
                                        )
                                        if (isVerified) {
                                            val badgeColor = if (planType.lowercase() == "yearly" || planType.lowercase() == "annual") {
                                                Color(0xFFFFD700) // Gold for Annual/Yearly
                                            } else {
                                                Color(0xFF2196F3) // Blue for Monthly
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = "Verified Player",
                                                tint = badgeColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        } else if (tier == "Premium") {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = "Premium Player",
                                                tint = currentLeagueInfo.color,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = when (rank) {
                                            1 -> "League Champion"
                                            2 -> "Elite Challenger"
                                            3 -> "Top Contender"
                                            else -> "Active Contender"
                                        },
                                        color = if (isEmerald) TextMutedGreen else TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Text(
                                text = "$points XP",
                                color = if (isMe) currentLeagueInfo.color else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}
