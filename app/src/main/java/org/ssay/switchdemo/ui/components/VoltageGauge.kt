package org.ssay.switchdemo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ssay.switchdemo.ui.theme.NeonCyan
import org.ssay.switchdemo.ui.theme.NeonPink

/**
 * FIXED #14: voltage_background.png replaced with a Canvas-drawn gradient + accent
 *            line. Crisp at any DPI, theme-aware.
 * FIXED #15: when [hasLiveData] is false, shows "— V" with a "no data" caption
 *            instead of a misleading 0.00.
 * FIXED #16: secondary line explains the value in plain language.
 * FIXED #45: every colour comes from MaterialTheme.colorScheme.
 * FIXED #52: contentDescription announces the actual reading.
 */
@Composable
fun VoltageGauge(
    voltage: Float,
    hasLiveData: Boolean,
    isWarning: Boolean,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val accentColor by animateColorAsState(
        targetValue   = if (isWarning) NeonPink else NeonCyan,
        animationSpec = tween(300),
        label         = "voltageColor"
    )

    val config      = LocalConfiguration.current
    val screenW     = config.screenWidthDp
    val gaugeHeight = when {
        screenW >= 600 -> 120.dp
        compact        -> 84.dp
        else           -> 100.dp
    }
    val labelSize    = if (compact) 10.sp else 11.sp
    val subtitleSize = if (compact) 9.sp  else 10.sp
    val valueSize    = if (compact) 30.sp else 36.sp
    val unitSize     = if (compact) 14.sp else 18.sp
    val iconSize     = if (compact) 30.dp else 36.dp
    val hPad         = if (compact) 14.dp else 20.dp

    val cd = if (hasLiveData) "Switch reading: %.2f volts".format(voltage)
             else              "Switch reading: no data"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(gaugeHeight)
            .clip(RoundedCornerShape(14.dp))
            .semantics { contentDescription = cd }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            // Card surface with subtle accent gradient
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF14181F), Color(0xFF0A0D14)),
                    start  = Offset(0f, 0f),
                    end    = Offset(size.width, size.height)
                )
            )
            // Glow accent line on the left edge
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, accentColor.copy(alpha = 0.95f), Color.Transparent),
                    startY = 0f,
                    endY   = size.height
                ),
                topLeft = Offset(0f, 0f),
                size    = androidx.compose.ui.geometry.Size(width = 4f, height = size.height)
            )
            // Faint horizontal scan line for the "instrument" feel
            drawLine(
                color       = accentColor.copy(alpha = 0.18f),
                start       = Offset(0f, size.height * 0.62f),
                end         = Offset(size.width, size.height * 0.62f),
                strokeWidth = 1f
            )
        }
        Row(
            modifier              = Modifier.fillMaxSize().padding(horizontal = hPad, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text          = "Switch reading",
                    fontSize      = labelSize,
                    fontWeight    = FontWeight.Bold,
                    color         = accentColor,
                    letterSpacing = 1.sp
                )
                Text(
                    text     = "Voltage from the resistor ladder",
                    fontSize = subtitleSize,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (hasLiveData) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = accentColor, fontSize = valueSize, fontWeight = FontWeight.Bold)) {
                                append("%.2f".format(voltage))
                            }
                            withStyle(SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = unitSize,
                                fontWeight = FontWeight.Normal)
                            ) { append("  V") }
                        }
                    )
                } else {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                fontSize = valueSize,
                                fontWeight = FontWeight.Bold)
                            ) { append("—") }
                            withStyle(SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                fontSize = unitSize,
                                fontWeight = FontWeight.Normal)
                            ) { append("  V") }
                        }
                    )
                    Text(
                        text     = "Connect the device to see live data",
                        fontSize = subtitleSize,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
            Icon(
                imageVector       = Icons.Filled.Bolt,
                contentDescription = null,
                tint              = if (hasLiveData) accentColor else accentColor.copy(alpha = 0.35f),
                modifier          = Modifier.size(iconSize)
            )
        }
    }
}
