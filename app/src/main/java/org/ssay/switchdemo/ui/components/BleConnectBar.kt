package org.ssay.switchdemo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ssay.switchdemo.data.BleConnectionState
import org.ssay.switchdemo.ui.theme.NeonCyan
import org.ssay.switchdemo.ui.theme.NeonGreen
import org.ssay.switchdemo.ui.theme.NeonPink
import org.ssay.switchdemo.ui.theme.StatusYellow

/**
 * FIXED #1, #2, #9, #56:
 *  - Replaced the stretched-PNG background with a Canvas-drawn gradient that adapts
 *    to any size and orientation.
 *  - Wrapped the bar in a Material3 Card with `clickable(role = Button)` so it gets
 *    a proper ripple and announces "Button" to TalkBack.
 *  - The whole row is laid out horizontally regardless of orientation.
 *
 * FIXED #8: shows attempt counter when the manager is in RECONNECTING state.
 */
@Composable
fun BleConnectBar(
    connectionState: BleConnectionState,
    reconnectAttempt: Int = 0,
    scanTimedOut: Boolean = false,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (mainText, subText, accent) = when (connectionState) {
        BleConnectionState.DISCONNECTED -> Triple(
            if (scanTimedOut) "Device Not Found" else "Connect Device",
            if (scanTimedOut) "Tap to retry scan" else "Tap to find your demonstrator board",
            if (scanTimedOut) NeonPink else NeonCyan
        )
        BleConnectionState.SCANNING     -> Triple("Scanning…",   "Looking for device",  StatusYellow)
        BleConnectionState.RECONNECTING -> Triple(
            "Reconnecting…",
            if (reconnectAttempt > 0) "Attempt $reconnectAttempt of 5 — tap to cancel" else "Tap to cancel",
            StatusYellow
        )
        BleConnectionState.CONNECTED    -> Triple("Connected", "Tap to disconnect", NeonGreen)
    }

    val icon = when (connectionState) {
        BleConnectionState.CONNECTED    -> Icons.Filled.Bluetooth
        BleConnectionState.SCANNING,
        BleConnectionState.RECONNECTING -> Icons.Filled.BluetoothSearching
        BleConnectionState.DISCONNECTED -> if (scanTimedOut) Icons.Filled.BluetoothDisabled else Icons.Filled.Bluetooth
    }

    val config    = LocalConfiguration.current
    val isCompact = config.screenWidthDp < 380
    val barHeight = if (isCompact) 60.dp else 68.dp

    Surface(
        modifier  = modifier.fillMaxWidth().heightIn(min = barHeight),
        color     = MaterialTheme.colorScheme.surface,
        shape     = RoundedCornerShape(0.dp),
        tonalElevation = 2.dp
    ) {
        Card(
            modifier  = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            onClick   = onToggle,
            shape     = RoundedCornerShape(14.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = barHeight - 16.dp)) {
                // FIXED #2: Canvas gradient instead of stretched PNG.
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors    = listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.04f)),
                            start     = Offset(0f, 0f),
                            end       = Offset(size.width, size.height)
                        )
                    )
                }
                Row(
                    modifier              = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    StatusIndicator(
                        state            = connectionState,
                        reconnectAttempt = reconnectAttempt,
                        showLabel        = false,
                        dotSize          = if (isCompact) 14.dp else 16.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = mainText,
                            fontSize   = if (isCompact) 15.sp else 17.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text       = subText,
                            fontSize   = if (isCompact) 11.sp else 12.sp,
                            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Icon(
                        imageVector       = icon,
                        contentDescription = null,
                        tint              = accent,
                        modifier          = Modifier.size(if (isCompact) 24.dp else 28.dp)
                    )
                }
            }
        }
    }
}
