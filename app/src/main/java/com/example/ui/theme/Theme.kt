package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = DeepTeal,
    onPrimary = Color.White,
    primaryContainer = TealMist,
    onPrimaryContainer = DeepTealDark,
    secondary = AmberGold,
    onSecondary = Color.White,
    secondaryContainer = AmberCream,
    onSecondaryContainer = AmberDeep,
    tertiary = BotanicalSage,
    onTertiary = Color.White,
    tertiaryContainer = SageTint,
    onTertiaryContainer = Color(0xFF064E3B),
    background = IvoryPaper,
    onBackground = InkCharcoal,
    surface = IvoryPaper,
    onSurface = InkCharcoal,
    surfaceVariant = SoftParchment,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLowest = WarmCardWhite,
    surfaceContainerLow = Color(0xFFF6F3EC),
    surfaceContainer = WarmCardWhite,
    surfaceContainerHigh = SoftParchment,
    outline = WarmOutline,
    outlineVariant = Color(0xFFE6E0D6),
    error = CrimsonError,
    errorContainer = CrimsonTint,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrightTeal,
    onPrimary = DeepTealDark,
    primaryContainer = TealMistDark,
    onPrimaryContainer = TealMist,
    secondary = AmberGoldLight,
    onSecondary = AmberDeep,
    secondaryContainer = Color(0xFF452806),
    onSecondaryContainer = AmberCream,
    tertiary = SageDark,
    onTertiary = Color(0xFF064E3B),
    tertiaryContainer = Color(0xFF134E4A),
    onTertiaryContainer = SageTint,
    background = DeepInkBg,
    onBackground = PaleParchmentText,
    surface = DeepInkBg,
    onSurface = PaleParchmentText,
    surfaceVariant = DeepSlateVariant,
    onSurfaceVariant = MutedSlateLight,
    surfaceContainerLowest = Color(0xFF0B1114),
    surfaceContainerLow = DeepSlateSurface,
    surfaceContainer = DeepSlateSurface,
    surfaceContainerHigh = DeepSlateVariant,
    outline = Color(0xFF334148),
    outlineVariant = Color(0xFF233036),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF7F1D1D),
)

val LessonVaultShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun LessonVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = LessonVaultShapes,
        content = content,
    )
}
