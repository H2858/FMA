package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.*
import com.example.ui.theme.*

class MainActivity : androidx.fragment.app.FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val isDarkMode by mainViewModel.isDarkMode.collectAsState()
            val isEmeraldGlobal by mainViewModel.isEmeraldTheme.collectAsState()
            val currentLanguage by mainViewModel.currentLanguage.collectAsState()
            val layoutDirection = if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MyApplicationTheme(isEmerald = isEmeraldGlobal, darkTheme = isDarkMode) {
                    val navController = rememberNavController()
                    val sessionState by mainViewModel.sessionState.collectAsState()

                // Automatic redirect to Welcome or Dashboard depending on auth state
                LaunchedEffect(sessionState) {
                    if (sessionState != null) {
                        navController.navigate("dashboard") {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        val currentRoute = navController.currentDestination?.route
                        if (currentRoute != null && currentRoute != "welcome" && currentRoute != "auth") {
                            navController.navigate("welcome") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }


                NavHost(
                    navController = navController,
                    startDestination = "welcome",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("welcome") {
                        WelcomeScreen(
                            onGetStarted = {
                                if (sessionState != null) {
                                    navController.navigate("dashboard")
                                } else {
                                    navController.navigate("auth")
                                }
                            }
                        )
                    }

                    composable("auth") {
                        AuthScreen(
                            viewModel = mainViewModel,
                            onAuthSuccess = {
                                navController.navigate("dashboard") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("pricing") {
                        PricingScreen(
                            viewModel = mainViewModel,
                            onDismiss = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = mainViewModel,
                            onNavigateToPricing = {
                                navController.navigate("pricing")
                            },
                            onNavigateToProfile = {
                                navController.navigate("profile")
                            },
                            onNavigateToLeaderboard = {
                                navController.navigate("leaderboard")
                            },
                            onNavigateToAdmin = {
                                navController.navigate("admin")
                            },
                            onLogout = {
                                navController.navigate("auth") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("admin") {
                        val isAdminReal by mainViewModel.isAdminReal.collectAsState()
                        if (isAdminReal) {
                            AdminScreen(
                                viewModel = mainViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            LaunchedEffect(Unit) {
                                navController.popBackStack()
                            }
                        }
                    }

                    composable("profile") {
                        ProfileScreen(
                            viewModel = mainViewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToPricing = { navController.navigate("pricing") }
                        )
                    }

                    composable("leaderboard") {
                        LeaderboardScreen(
                            viewModel = mainViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToPricing: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onLogout: () -> Unit
) {
    val sessionState by viewModel.sessionState.collectAsState()
    var currentTab by remember { mutableStateOf(1) } // Default to central aura screen

    // Safeguard logouts
    LaunchedEffect(sessionState) {
        if (sessionState == null) {
            onLogout()
        }
    }

    val isEmerald by viewModel.isEmeraldTheme.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val bgGradientColors = if (isEmerald) {
        if (isDarkMode) {
            listOf(MaterialTheme.colorScheme.background, Color(0xFF030D08))
        } else {
            listOf(MaterialTheme.colorScheme.background, Color(0xFFE8F5E9)) // light clean green gradient base
        }
    } else {
        if (isDarkMode) {
            listOf(MaterialTheme.colorScheme.background, Color(0xFF0B0507))
        } else {
            listOf(MaterialTheme.colorScheme.background, Color(0xFFFCE4EC)) // light clean plum gradient base
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgGradientColors))
    ) {
        // Embed the actual screens based on the current active tab
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentTab) {
                0 -> ContactsScreen(viewModel = viewModel, onNavigateToPricing = onNavigateToPricing)
                1 -> AICoachScreen(
                    viewModel = viewModel,
                    onNavigateToPricing = onNavigateToPricing,
                    onNavigateToProfile = onNavigateToProfile,
                    onNavigateToLeaderboard = onNavigateToLeaderboard
                )
                2 -> SettingsScreen(
                    viewModel = viewModel,
                    onBack = { currentTab = 1 },
                    onNavigateToAdmin = onNavigateToAdmin
                )
            }
        }

        // Floating Custom Bottom Navigation Bar in thin neon-outline style
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (isEmerald) GlassGreenOverlay else GlassOverlay)
                .border(
                    1.dp,
                    if (isEmerald) GlassGreenBorder else GlassBorder,
                    RoundedCornerShape(24.dp)
                )
                .testTag("dashboard_navigation_bar"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: Contacts / History
                DashboardNavItem(
                    icon = Icons.Outlined.People,
                    activeIcon = Icons.Filled.People,
                    label = "Logs",
                    isActive = currentTab == 0,
                    onClick = { currentTab = 0 },
                    testTag = "nav_item_contacts",
                    isEmerald = isEmerald
                )

                // Tab 1: Central Pulsing Aura (Glowing Core node)
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    if (currentTab == 1) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            1.5.dp,
                            if (currentTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .clickable {
                            SoundManager.playClick()
                            currentTab = 1
                        }
                        .testTag("nav_item_central_aura"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentTab == 1) Icons.Filled.GraphicEq else Icons.Outlined.GraphicEq,
                        contentDescription = "Vortex Aura Core",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Tab 2: Settings
                DashboardNavItem(
                    icon = Icons.Outlined.Settings,
                    activeIcon = Icons.Filled.Settings,
                    label = "System",
                    isActive = currentTab == 2,
                    onClick = { currentTab = 2 },
                    testTag = "nav_item_settings",
                    isEmerald = isEmerald
                )
            }
        }
    }
}

@Composable
fun DashboardNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String,
    isEmerald: Boolean
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                SoundManager.playClick()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isActive) activeIcon else icon,
            contentDescription = label,
            tint = if (isActive) MaterialTheme.colorScheme.primary else (if (isEmerald) TextSecondaryGreen else TextSecondary),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isActive) TextPrimary else (if (isEmerald) TextMutedGreen else TextMuted),
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}
