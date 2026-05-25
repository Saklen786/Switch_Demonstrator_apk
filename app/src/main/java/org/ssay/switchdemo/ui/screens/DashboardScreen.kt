package org.ssay.switchdemo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.elmos_logo),
                contentDescription = "Elmos Logo",
                modifier = Modifier.height(45.dp).width(60.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "E521.39 IC",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Text(
                    text = "Switch Demonstrator",
                    fontSize = 10.sp,
                    color = GreyText,
                    letterSpacing = 1.sp
                )
            }

            // Status indicator dot
            val dotColor = when (bleConnectionState) {
                BleConnectionState.CONNECTED -> NeonGreen
                BleConnectionState.SCANNING -> StatusYellow
                BleConnectionState.DISCONNECTED -> NeonPink
            }
            Image(
                painter = painterResource(
                    when (bleConnectionState) {
                        BleConnectionState.CONNECTED -> R.drawable.status_green
                        BleConnectionState.SCANNING -> R.drawable.status_yellow
                        BleConnectionState.DISCONNECTED -> R.drawable.status_red
                    }
                ),
                contentDescription = "Connection Status",
                modifier = Modifier.size(14.dp)
            )
        }

        // --- Voltage Gauge ---
        VoltageGauge(voltage = voltage, isWarning = isWarning)

        // --- Motorcycle Graphic ---
        MotorcycleGraphic(
            switchState = switchState,
            modifier = Modifier.weight(1f, fill = false)
        )

        // --- Data Stream Terminal ---
        DataStreamTerminal(logMessages = logMessages)
    }
}
