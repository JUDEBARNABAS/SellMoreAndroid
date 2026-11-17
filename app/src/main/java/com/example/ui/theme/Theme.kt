package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SellMoreDarkColorScheme = darkColorScheme(
    primary = StrawberryPink,
    onPrimary = TextPrimary,
    primaryContainer = StrawberryContainer,
    onPrimaryContainer = StrawberryPinkSoft,
    secondary = TelemetryCyan,
    onSecondary = EspressoBg,
    secondaryContainer = TelemetryCyanContainer,
    onSecondaryContainer = TelemetryCyanSoft,
    tertiary = DispenseEmerald,
    onTertiary = EspressoBg,
    tertiaryContainer = DispenseEmeraldContainer,
    onTertiaryContainer = DispenseEmeraldSoft,
    background = EspressoBg,
    onBackground = TextPrimary,
    surface = EspressoSurface,
    onSurface = TextPrimary,
    surfaceVariant = EspressoSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = EspressoBorder,
    outlineVariant = EspressoBorderSubtle,
    error = ErrorRose,
    onError = EspressoBg,
    errorContainer = ErrorRoseContainer,
    onErrorContainer = ErrorRose
)

val SellMoreShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SellMoreTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SellMoreDarkColorScheme,
        typography = SellMoreTypography,
        shapes = SellMoreShapes,
        content = content
    )
}
