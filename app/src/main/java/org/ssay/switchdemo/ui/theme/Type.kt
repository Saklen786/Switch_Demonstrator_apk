package org.ssay.switchdemo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * FIXED #50: a deliberate Typography. Display/Headline/Title use the system
 * default sans (which renders as Roboto / Roboto Flex on most devices), while
 * the data-heavy bodies/labels use FontFamily.Monospace so packets and ADC
 * values line up cleanly. Colours are intentionally NOT baked in here — they
 * come from MaterialTheme.colorScheme via the consuming composables (FIXED #45).
 */
private val Brand     = FontFamily.SansSerif
private val Telemetry = FontFamily.Monospace

val Typography = Typography(
    displayLarge   = TextStyle(fontFamily = Brand,     fontWeight = FontWeight.Bold,     fontSize = 44.sp, lineHeight = 48.sp),
    headlineLarge  = TextStyle(fontFamily = Brand,     fontWeight = FontWeight.Bold,     fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = Brand,     fontWeight = FontWeight.Bold,     fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge     = TextStyle(fontFamily = Brand,     fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleMedium    = TextStyle(fontFamily = Brand,     fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge      = TextStyle(fontFamily = Brand,     fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium     = TextStyle(fontFamily = Brand,     fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 18.sp),
    bodySmall      = TextStyle(fontFamily = Telemetry, fontWeight = FontWeight.Normal,   fontSize = 11.sp, lineHeight = 16.sp),
    labelLarge     = TextStyle(fontFamily = Brand,     fontWeight = FontWeight.Bold,     fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium    = TextStyle(fontFamily = Telemetry, fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 14.sp),
    labelSmall     = TextStyle(fontFamily = Brand,     fontWeight = FontWeight.Medium,   fontSize = 10.sp, lineHeight = 14.sp)
)
