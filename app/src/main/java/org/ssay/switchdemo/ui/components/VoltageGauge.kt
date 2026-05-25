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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val accentColor by animateColorAsState(
        targetValue    = if (isWarning) NeonPink else NeonCyan,
        animationSpec  = tween(300),
        label          = "voltageColor"
    )

    // Responsive sizing
    val config       = LocalConfiguration.current
    val screenW      = config.screenWidthDp
    val gaugeHeight  = when {
        screenW >= 600 -> 120.dp
        compact        -> 80.dp
        else           -> 96.dp
    }
    val labelSize   = if (compact) 10.sp else 11.sp
    val valueSize   = if (compact) 30.sp else 36.sp
    val unitSize    = if (compact) 14.sp else 18.sp
    val iconSize    = if (compact) 32.dp else 40.dp
    val hPad        = if (compact) 14.dp else 20.dp

    Box(modifier = modifier.fillMaxWidth().height(gaugeHeight)) {
        Image(
            painter      = painterResource(R.drawable.voltage_background),
            contentDescription = null,
            modifier     = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = hPad),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text          = "Battery Voltage",
                    fontSize      = labelSize,
                    fontWeight    = FontWeight.Bold,
                    color         = accentColor,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = accentColor, fontSize = valueSize, fontWeight = FontWeight.Bold)) {
                            append(String.format("%.2f", voltage))
                        }
                        withStyle(SpanStyle(color = GreyText, fontSize = unitSize, fontWeight = FontWeight.Normal)) {
                            append("  V")
                        }
                    }
                )
            }

            Image(
                painter            = painterResource(R.drawable.icon_voltage),
                contentDescription = "Voltage",
                modifier           = Modifier.size(iconSize),
                colorFilter        = ColorFilter.tint(accentColor)
            )
        }
    }
}