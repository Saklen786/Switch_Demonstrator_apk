package org.ssay.switchdemo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ssay.switchdemo.R
import org.ssay.switchdemo.data.BleConnectionState
import org.ssay.switchdemo.ui.theme.*

@Composable
fun BleConnectBar(
    connectionState: BleConnectionState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconTint: Color
    val mainText: String
    val subText: String?

    when (connectionState) {
        BleConnectionState.DISCONNECTED -> {
            iconTint = NeonCyan
            mainText = "Connect Device"
            subText = null
        }
        BleConnectionState.SCANNING -> {
            iconTint = NeonGreen
            mainText = "Scanning..."
            subText = "Looking for device"
        }
        BleConnectionState.CONNECTED -> {
            iconTint = NeonGreen
            mainText = "Connected"
            subText = "Tap to disconnect"
        }
    }

    // Pulse animation for scanning
    val pulseAlpha: Float
    if (connectionState == BleConnectionState.SCANNING) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        pulseAlpha = alpha
    } else {
        pulseAlpha = 1f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(65.dp)
            .clickable(onClick = onToggle)
    ) {
        Image(
            painter = painterResource(R.drawable.ble_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .alpha(pulseAlpha),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.icon_bluetooth),
                contentDescription = "Bluetooth",
                modifier = Modifier.size(30.dp),
                colorFilter = ColorFilter.tint(iconTint)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = mainText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhiteText
                )
                if (subText != null) {
                    Text(
                        text = subText,
                        fontSize = 10.sp,
                        color = GreyText
                    )
                }
            }
        }
    }
}
