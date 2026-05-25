package org.ssay.switchdemo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ssay.switchdemo.data.BleConnectionState
import org.ssay.switchdemo.ui.theme.NeonCyan
import org.ssay.switchdemo.ui.theme.NeonGreen
import org.ssay.switchdemo.ui.theme.NeonPink
import org.ssay.switchdemo.ui.theme.StatusYellow

/**
 * FIXED #7: replaces the tiny 12-14dp PNG status dot with a properly visible,
 * animated, vector-drawn indicator with an explicit text label.
 *
 * FIXED #52: exposes a contentDescription so accessibility services announce
 * the connection state instead of just "image".
 */
@Composable
fun StatusIndicator(
    state: BleConnectionState,
    reconnectAttempt: Int = 0,
    showLabel: Boolean = true,
    dotSize: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    val (label, color, animated) = when (state) {
        BleConnectionState.CONNECTED    -> Triple("Connected",    NeonGreen,    false)
        BleConnectionState.SCANNING     -> Triple("Scanning",     StatusYellow, true)
        BleConnectionState.RECONNECTING -> Triple(
            if (reconnectAttempt > 0) "Reconnecting $reconnectAttempt/5" else "Reconnecting",
            StatusYellow, true
        )
        BleConnectionState.DISCONNECTED -> Triple("Disconnected", NeonPink,     false)
    }

    val infinite = rememberInfiniteTransition(label = "statusPulse")
    val pulse by infinite.animateFloat(
        initialValue  = 0.45f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val ringScale by infinite.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.6f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1100, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )

    val cd = "Connection status: ${label.lowercase()}"
    Row(
        modifier              = modifier.semantics { contentDescription = cd },
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier        = Modifier.size(dotSize * 2),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(dotSize * 2)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val coreRadius = dotSize.toPx() / 2f

                if (animated) {
                    // Expanding outer ring
                    drawCircle(
                        color  = color.copy(alpha = (1.4f - ringScale).coerceIn(0f, 1f) * 0.6f),
                        center = Offset(cx, cy),
                        radius = coreRadius * ringScale
                    )
                }
                // Soft halo behind the core
                drawCircle(
                    brush  = Brush.radialGradient(
                        listOf(color.copy(alpha = 0.55f * (if (animated) pulse else 1f)), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = coreRadius * 1.9f
                    ),
                    center = Offset(cx, cy),
                    radius = coreRadius * 1.9f
                )
                // Solid core
                drawCircle(
                    color  = color,
                    center = Offset(cx, cy),
                    radius = coreRadius
                )
            }
        }
        if (showLabel) {
            Text(
                text       = label,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
