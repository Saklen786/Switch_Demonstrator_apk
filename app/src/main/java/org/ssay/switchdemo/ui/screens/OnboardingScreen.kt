package org.ssay.switchdemo.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ssay.switchdemo.R

/**
 * FIXED #54: 3-step welcome flow shown only on the very first launch
 * (gated by a DataStore flag). Skip and Done both dismiss permanently.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        OnbPage("Welcome to the Elmos E521.39 demo",
                "A live view of how a single ADC line decodes 17 motorcycle handlebar switch positions."),
        OnbPage("Connect over Bluetooth",
                "Power the demonstrator board, tap the connection bar at the bottom, and grant Bluetooth access."),
        OnbPage("Read the dashboard",
                "Voltage shows the live ADC reading. The motorcycle graphic mirrors the active switches. The log streams every event.")
    )
    var index by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) { Text("Skip") }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        Image(
            painter            = painterResource(R.drawable.elmos_logo),
            contentDescription = "Elmos logo",
            modifier           = Modifier.height(60.dp).width(120.dp),
            contentScale       = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(40.dp))

        AnimatedContent(
            targetState   = index,
            transitionSpec = {
                slideInHorizontally(initialOffsetX = { it / 4 }) + fadeIn() togetherWith
                slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut()
            },
            label = "onbStep"
        ) { i ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text       = pages[i].title,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground,
                    textAlign  = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text       = pages[i].body,
                    fontSize   = 14.sp,
                    color      = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    textAlign  = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pages.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == index) 12.dp else 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (i == index) MaterialTheme.colorScheme.primary
                            else            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (index < pages.lastIndex) index++ else onFinish()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape    = RoundedCornerShape(8.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(if (index < pages.lastIndex) "Next" else "Got it",
                 fontWeight = FontWeight.Bold)
        }
    }
}

private data class OnbPage(val title: String, val body: String)
