package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SkillSwapDarkColorScheme = darkColorScheme(
    primary = VioletPrimary,
    onPrimary = Color.White,
    primaryContainer = VioletDeep,
    onPrimaryContainer = SoftLavender,
    secondary = CobaltBlue,
    onSecondary = Color.White,
    secondaryContainer = ElevatedNavy,
    onSecondaryContainer = TextPrimary,
    tertiary = CyanHighlight,
    onTertiary = DeepNavyBg,
    background = DeepNavyBg,
    onBackground = TextPrimary,
    surface = MidnightSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedNavy,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = SubtleBorder,
    error = RoseError,
    onError = Color.White
)

private val SkillSwapShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SkillSwapTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SkillSwapDarkColorScheme,
        typography = SkillSwapTypography,
        shapes = SkillSwapShapes,
        content = content
    )
}
