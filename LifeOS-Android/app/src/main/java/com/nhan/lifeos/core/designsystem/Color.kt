package com.nhan.lifeos.core.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Midnight Aurora Theme Tokens
val LifeOSPrimary = Color(0xFF8B5CF6)         // Electric Purple (#8B5CF6)
val LifeOSPrimaryVariant = Color(0xFF7C3AED)  // Deep Purple
val LifeOSCyan = Color(0xFF22D3EE)            // Electric Cyan (#22D3EE)
val LifeOSGreen = Color(0xFF10B981)           // Success Emerald (#10B981)
val LifeOSRed = Color(0xFFEF4444)             // Coral Red (#EF4444)
val LifeOSAmber = Color(0xFFF59E0B)           // Amber / Warning (#F59E0B)

val LifeOSBackgroundDark = Color(0xFF0B1024)  // Deep Navy (#0B1024)
val LifeOSSurfaceDark = Color(0xFF171F3B)     // Dark Surface (#171F3B)
val LifeOSSurfaceCard = Color(0xFF1E294B)     // Card Surface (#1E294B)
val LifeOSSurfaceHover = Color(0xFF25335E)    // Surface 3 (#25335E)
val LifeOSGlassBorder = Color(0x1AFFFFFF)     // Subtle Glass Border (rgba(255,255,255,0.08-0.1))
val LifeOSGlassBorderAccent = Color(0x338B5CF6) // Purple subtle glow border

val LifeOSTextHigh = Color(0xFFF8FAFC)        // Slate 50
val LifeOSTextMid = Color(0xFF94A3B8)         // Slate 400
val LifeOSTextLow = Color(0xFF64748B)         // Slate 500

val LifeOSAuroraGradient = Brush.horizontalGradient(
    colors = listOf(LifeOSPrimary, LifeOSCyan)
)
val LifeOSCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF1E294B), Color(0xFF151C38))
)

