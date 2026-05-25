package org.ssay.switchdemo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary        = NeonCyan,
    secondary      = NeonPink,
    tertiary       = NeonGreen,
    background     = DarkBackground,
    surface        = DarkSurface,
    onPrimary      = DarkBackground,
    onSecondary    = DarkBackground,
    onTertiary     = DarkBackground,
    onBackground   = WhiteText,
    onSurface      = WhiteText,
    surfaceVariant = CardBorder,
    outline        = CardBorder
)

private val LightColorScheme = lightColorScheme(
    primary        = LightAccentCyan,
    secondary      = LightAccentPink,
    tertiary       = LightAccentGreen,
    background     = LightBackground,
    surface        = LightSurface,
    onPrimary      = LightBackground,
    onSecondary    = LightBackground,
    onTertiary     = LightBackground,
    onBackground   = LightHeadlineText,
    onSurface      = LightBodyText,
    surfaceVariant = LightCardBorder,
    outline        = LightCardBorder
)

@Composable
fun SwitchDemonstratorTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = Typography,
        content     = content
    )
}