package org.ssay.switchdemo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ssay.switchdemo.R
import org.ssay.switchdemo.ui.theme.*

@Composable
fun VoltageGauge(
    voltage: Float,
    isWarning: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor by animateColorAsState(
        targetValue = if (isWarning) NeonPink else NeonCyan,
        animationSpec = tween(300),
        label = "voltageColor"
    )

    Box(modifier = modifier.fillMaxWidth().height(100.dp)) {
        // Background image
        Image(
            painter = painterResource(R.drawable.voltage_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Battery Voltage",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = accentColor, fontSize = 38.sp, fontWeight = FontWeight.Bold)) {
                            append(String.format("%.2f", voltage))
                        }
                        withStyle(SpanStyle(color = GreyText, fontSize = 18.sp, fontWeight = FontWeight.Normal)) {
                            append("  V")
                        }
                    }
                )
            }

            Image(
                painter = painterResource(R.drawable.icon_voltage),
                contentDescription = "Voltage",
                modifier = Modifier.size(40.dp),
                colorFilter = ColorFilter.tint(accentColor)
            )
        }
    }
}
