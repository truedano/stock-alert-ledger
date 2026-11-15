package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FinancialDarkColorScheme = darkColorScheme(
    primary = BrandSky,
    onPrimary = CanvasObsidian,
    primaryContainer = BrandSkyContainer,
    onPrimaryContainer = TextPrimary,
    secondary = AlertAmber,
    onSecondary = CanvasObsidian,
    secondaryContainer = AlertAmberContainer,
    onSecondaryContainer = AlertAmber,
    tertiary = BrandIndigo,
    onTertiary = CanvasObsidian,
    background = CanvasObsidian,
    onBackground = TextPrimary,
    surface = SurfaceElevated,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = SurfaceElevated,
    surfaceContainer = SurfaceCard,
    surfaceContainerHigh = SurfaceCardHigh,
    outline = HairlineBorder,
    outlineVariant = HairlineBorder,
    error = TaiwanRed,
    onError = TextPrimary,
    errorContainer = TaiwanRedContainer,
    onErrorContainer = TaiwanRed,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun StockAssistantTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FinancialDarkColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
