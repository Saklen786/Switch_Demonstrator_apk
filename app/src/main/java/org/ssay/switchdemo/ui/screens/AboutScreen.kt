package org.ssay.switchdemo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ssay.switchdemo.BuildConfig
import org.ssay.switchdemo.R

/**
 * FIXED #30, #31, #32, #45:
 *  - Wrapped in `verticalScroll` so the content is reachable on small or
 *    landscape screens.
 *  - Sections explaining the IC, key features, how-to-use, and contact info.
 *  - Version reads from BuildConfig.VERSION_NAME instead of a hardcoded string.
 *  - Colours come from MaterialTheme.colorScheme.
 */
@Composable
fun AboutScreen(
    firmwareVersion: String? = null,
    modifier: Modifier = Modifier
) {
    val config = LocalConfiguration.current
    val hPad   = if (config.screenWidthDp < 380) 14.dp else 20.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = hPad, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text       = "About",
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.secondary
        )

        // Header card with logo + version
        Card {
            Image(
                painter            = painterResource(R.drawable.elmos_logo),
                contentDescription = "Elmos logo",
                modifier           = Modifier.height(58.dp).width(110.dp),
                contentScale       = ContentScale.Fit
            )
            Text(
                text       = "E521.39 Switch Demonstrator",
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
                textAlign  = TextAlign.Center
            )
            Text(
                // FIXED #32: read from BuildConfig.
                text       = "App version ${BuildConfig.VERSION_NAME}",
                fontSize   = 13.sp,
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            if (firmwareVersion != null) {
                Text(
                    text       = "Firmware: $firmwareVersion",
                    fontSize   = 13.sp,
                    color      = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        SectionCard(title = "What is the E521.39?") {
            Text(
                text       = "The Elmos E521.39 is a high-precision automotive ADC designed for resistor-ladder " +
                             "switch decoding. A single analog input reliably distinguishes 17+ switch positions " +
                             "on a two-wheeler handlebar — replacing a thick wiring harness with a single signal line.",
                fontSize   = 13.sp,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = 19.sp
            )
        }

        SectionCard(title = "Key features") {
            BulletRow("17 distinguishable switch states")
            BulletRow("Resistor-ladder ADC decoding")
            BulletRow("BLE telemetry to this dashboard")
            BulletRow("Automotive-grade noise rejection")
        }

        SectionCard(title = "How to use this demo") {
            NumberedRow(1, "Power the demonstrator board.")
            NumberedRow(2, "Tap the connection bar at the bottom and grant Bluetooth permissions.")
            NumberedRow(3, "Toggle the handlebar switch and watch the dashboard update in real time.")
            NumberedRow(4, "No hardware? Tap the demo button on the dashboard to see a simulated cycle.")
        }

        SectionCard(title = "Contact & support") {
            Text(
                text       = "Built by SSAY for Elmos product demonstrations.\nFor questions, contact your Elmos representative.",
                fontSize   = 13.sp,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = 19.sp
            )
        }

        Text(
            text       = "Developed by SSAY",
            fontSize   = 12.sp,
            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content             = content
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text       = title,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface
        )
        content()
    }
}

@Composable
private fun BulletRow(text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            imageVector       = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint              = MaterialTheme.colorScheme.primary,
            modifier          = Modifier.size(18.dp)
        )
        Text(
            text       = text,
            fontSize   = 13.sp,
            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            lineHeight = 19.sp,
            modifier   = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NumberedRow(n: Int, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier        = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = n.toString(),
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize   = 12.sp
            )
        }
        Text(
            text       = text,
            fontSize   = 13.sp,
            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            lineHeight = 19.sp,
            modifier   = Modifier.weight(1f)
        )
    }
}
