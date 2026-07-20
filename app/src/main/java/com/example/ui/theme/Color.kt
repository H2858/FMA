package com.example.ui.theme

import androidx.compose.ui.graphics.Color

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme

// Premium Burgundy Theme Colors
val BurgundyPrimary = Color(0xFF800020)       // Main primary Burgundy
val BurgundyGlow = Color(0xFFC3073F)          // Radiant glow accent
val BurgundyDark = Color(0xFF4A0E17)          // Dark rich burgundy
val BurgundyLight = Color(0xFFD93B58)         // Lighter highlight

// Cosmic Dark Palette (Burgundy Mode)
val CosmicBlack = Color(0xFF0C0709)           // Off-black with warm burgundy/plum undertone
val SlateCard = Color(0xFF160F12)             // Darker background card
val SlateCardBorder = Color(0xFF2C1920)       // Thin premium borders

// --- COSMIC EMERALD PALETTE (Soft, premium, and comfortable for the eyes) ---
val EmeraldPrimary = Color(0xFF10B981)        // Soft classy emerald green (comfortable for eyes)
val EmeraldGlow = Color(0xFF059669)           // Deeper rich green glow
val EmeraldDark = Color(0xFF064E3B)           // Dark rich forest emerald
val EmeraldLight = Color(0xFF34D399)          // Soft pastel mint green

// Obsidian Dark Palette (Emerald Mode)
val ObsidianBlack = Color(0xFF060D0A)         // Ultra-dark background with deep teal/green undertone (matches screenshot)
val ObsidianCard = Color(0xFF0B1411)          // Dark obsidian container card
val ObsidianCardBorder = Color(0xFF14241E)    // Thin high-end dark green border

// Helper to determine if the color scheme is light or dark
fun ColorScheme.isLight(): Boolean = this.background == Color(0xFFFAFAFA)

// Dynamic responsive icon color for emerald mode
val EmeraldIconColor: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.isLight()) Color(0xFF032215) else Color(0xFF10B981)

// Dynamic button container color for emerald mode
val EmeraldButtonColor: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.isLight()) Color(0xFF032215) else Color(0xFF10B981)

// Text & Accent Colors - Dynamically responsive to Light/Dark Mode
val TextPrimary: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.isLight()) Color(0xFF060D0A) else Color(0xFFFBF5F7)

val TextSecondary: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.isLight()) Color(0xFF374151) else Color(0xFFBCA6AF)

val TextSecondaryGreen: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.isLight()) Color(0xFF032B1B) else Color(0xFF8FA89B)

val TextMuted: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.isLight()) Color(0xFF4B5563) else Color(0xFF7A646C)

val TextMutedGreen: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.isLight()) Color(0xFF053A25) else Color(0xFF536B5E)

// Glassmorphism effects - Dynamically responsive to Light/Dark Mode
val GlassOverlay: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.isLight()) Color(0xEBF8FAFC) else Color(0x66160F12)

val GlassGreenOverlay: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.isLight()) Color(0x0C032215) else Color(0x660B1411)

val GlassBorder: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.isLight()) Color(0xFFE2E8F0) else Color(0x33F5E6EB)

val GlassGreenBorder: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.isLight()) Color(0x28032215) else Color(0x33E6FFEE)


// Accent Neon
val NeonGlow = Color(0xFFFF2A5F)              // Ultra-neon hot burgundy-pink for highlights
val AccentAura = Color(0x1B800020)            // Low-alpha aura gradient color
val EmeraldAccentAura = Color(0x1B10B981)     // Low-alpha soft green aura gradient
