package org.ssay.switchdemo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ssay.switchdemo.R
import org.ssay.switchdemo.data.BleConnectionState
import org.ssay.switchdemo.data.SwitchState
import org.ssay.switchdemo.ui.components.DataStreamTerminal
import org.ssay.switchdemo.ui.components.MotorcycleGraphic
import org.ssay.switchdemo.ui.components.VoltageGauge
import org.ssay.switchdemo.ui.theme.*

@Composable
fun DashboardScreen(
    switchState: SwitchState,
    voltage: Float,
    isWarning: Boolean,
    logMessages: List<String>,
    bleConnectionState: BleConnectionState,
    modifier: Modifier = Modifier
) {
    // -----------------------------------------------------------------------
    // Responsive sizing — derive scale from actual screen width
    // -----------------------------------------------------------------------
    val config        = LocalConfiguration.current
    val screenW       = config.screenWidthDp
    val isCompact     = screenW < 380           // small phone (e.g. 360dp)
    val isExpanded    = screenW >= 600          // tablet / large phone landscape

    val hPad          = if (isExpanded) 32.dp else if (isCompact) 10.dp else 16.dp
    val vPad          = if (isExpanded) 24.dp else 12.dp
    val sectionGap    = if (isCompact) 10.dp else 14.dp

    val titleSize     = if (isExpanded) 24.sp else if (isCompact) 17.sp else 20.sp
    val subtitleSize  = if (isExpanded) 13.sp else 10.sp
    val dotSize       = if (isCompact) 12.dp else 14.dp
    val logoSize      = if (isExpanded) 60.dp else if (isCompact) 36.dp else 44.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = hPad, vertical = vPad),
        verticalArrangement = Arrangement.spacedBy(sectionGap)
    ) {

        // --- Header --------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo: the PNG has a black background.
            // BlendMode.Screen removes pure-black pixels and lets the dark app
            // background show through, while keeping the crimson design vivid.
            Box(
                modifier = Modifier
                    .size(logoSize)
                    .graphicsLayer { blendMode = BlendMode.Screen }
            ) {
                Image(
                    painter          = painterResource(R.drawable.elmos_logo),
                    contentDescription = "Elmos Logo",
                    modifier         = Modifier.fillMaxSize(),
                    contentScale     = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "E521.39 IC",
                    fontSize   = titleSize,
                    fontWeight = FontWeight.Bold,
                    color      = NeonCyan
                )
                Text(
                    text          = "Switch Demonstrator",
                    fontSize      = subtitleSize,
                    color         = GreyText,
                    letterSpacing = 1.sp
                )
            }

            // Connection status dot
            Image(
                painter = painterResource(
                    when (bleConnectionState) {
                        BleConnectionState.CONNECTED    -> R.drawable.status_green
                        BleConnectionState.SCANNING     -> R.drawable.status_yellow
                        BleConnectionState.DISCONNECTED -> R.drawable.status_red
                    }
                ),
                contentDescription = "Connection Status",
                modifier = Modifier.size(dotSize)
            )
        }

        // --- Voltage Gauge -------------------------------------------------
        VoltageGauge(
            voltage   = voltage,
            isWarning = isWarning,
            compact   = isCompact
        )

        // --- Motorcycle Graphic --------------------------------------------
        // fillMaxWidth — BoxWithConstraints inside drives height from aspect ratio
        MotorcycleGraphic(
            switchState = switchState,
            modifier    = Modifier.fillMaxWidth()
        )

        // --- Activity Log --------------------------------------------------
        DataStreamTerminal(
            logMessages = logMessages,
            compact     = isCompact
        )
    }
}