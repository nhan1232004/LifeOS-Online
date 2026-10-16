package com.nhan.lifeos.core.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================================================
// Apple System Colors (Dynamic iOS 18 & macOS HIG)
// ==========================================================================
val AppleBlue = Color(0xFF0A84FF)
val AppleIndigo = Color(0xFF5E5CE6)
val ApplePurple = Color(0xFFBF5AF2)
val ApplePink = Color(0xFFFF375F)
val AppleRed = Color(0xFFFF453A)
val AppleOrange = Color(0xFFFF9F0A)
val AppleYellow = Color(0xFFFFD60A)
val AppleGreen = Color(0xFF30D158)
val AppleMint = Color(0xFF63E6E2)
val AppleTeal = Color(0xFF40C8E0)
val AppleCyan = Color(0xFF64D2FF)

// Apple iOS Surfaces (OLED True Black & Inset Grouped Materials)
val AppleSystemBackground = Color(0xFF000000)
val AppleSecondarySystemBackground = Color(0xFF1C1C1E)
val AppleTertiarySystemBackground = Color(0xFF2C2C2E)
val AppleQuaternarySystemBackground = Color(0xFF3A3A3C)
val AppleSeparator = Color(0x2EFFFFFF)

// Primary LifeOS Tokens mapped to Apple HIG
val LifeOSPrimary = AppleBlue
val LifeOSPrimaryVariant = Color(0xFF007AFF)
val LifeOSCyan = AppleCyan
val LifeOSLavender = ApplePurple
val LifeOSGreen = AppleGreen
val LifeOSRed = AppleRed
val LifeOSAmber = AppleOrange

val LifeOSBackgroundDark = AppleSystemBackground
val LifeOSSurfaceDark = AppleSecondarySystemBackground
val LifeOSSurfaceCard = AppleSecondarySystemBackground
val LifeOSSurfaceHover = AppleTertiarySystemBackground
val LifeOSGlassBorder = AppleSeparator
val LifeOSGlassBorderAccent = Color(0x400A84FF)

val LifeOSTextHigh = Color(0xFFFFFFFF)
val LifeOSTextMid = Color(0xA6EBEBF5)       // ~65% alpha
val LifeOSTextLow = Color(0x61EBEBF5)       // ~38% alpha

val LifeOSAuroraGradient = Brush.horizontalGradient(
    colors = listOf(AppleBlue, AppleCyan)
)
val LifeOSLavenderGradient = Brush.horizontalGradient(
    colors = listOf(ApplePurple, AppleBlue)
)
val LifeOSCardGradient = Brush.verticalGradient(
    colors = listOf(AppleSecondarySystemBackground, AppleSecondarySystemBackground)
)

// Dynamic Accent Presets matching Web Token System
enum class AccentPreset(val displayName: String, val primary: Color, val borderAccent: Color) {
    APPLE_BLUE("Apple Blue", AppleBlue, Color(0x400A84FF)),
    LINEAR_VIOLET("Linear Violet", Color(0xFF8B7CFF), Color(0x408B7CFF)),
    EMERALD_MINT("Emerald Forest", Color(0xFF10B981), Color(0x4010B981)),
    SUNSET_AMBER("Sunset Amber", Color(0xFFFF9F0A), Color(0x40FF9F0A)),
    NEON_ROSE("Neon Rose", Color(0xFFFF375F), Color(0x40FF375F)),
    TITANIUM_STEALTH("Titanium Stealth", Color(0xFF98989D), Color(0x4098989D))
}
