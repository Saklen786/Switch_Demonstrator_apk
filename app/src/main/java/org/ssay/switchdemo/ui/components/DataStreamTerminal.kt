package org.ssay.switchdemo.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ssay.switchdemo.ui.theme.*

@Composable
fun DataStreamTerminal(
    logMessages: List<String>,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    var collapsed by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Auto-scroll to newest message
    LaunchedEffect(logMessages.size) {
        if (logMessages.isNotEmpty() && !collapsed) {
            listState.animateScrollToItem(logMessages.lastIndex)
        }
    }

    // Responsive log pane height
    val config      = LocalConfiguration.current
    val screenH     = config.screenHeightDp
    val logHeight: Dp = when {
        screenH >= 800 -> 120.dp
        compact        -> 80.dp
        else           -> 100.dp
    }

    val labelSize  = if (compact) 10.sp else 12.sp
    val msgSize    = if (compact) 10.sp else 11.sp
    val innerPad   = if (compact) 12.dp else 16.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(innerPad)
    ) {
        // Header row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text          = "Activity Log",
                fontSize      = labelSize,
                fontWeight    = FontWeight.Bold,
                color         = LightGreyText,
                letterSpacing = 0.5.sp
            )

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // LIVE badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkBackground)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "●", fontSize = 8.sp, color = NeonGreen)
                    Text(
                        text       = "LIVE",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color      = WhiteText
                    )
                }

                Text(
                    text       = if (collapsed) "SHOW" else "HIDE",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = GreyText,
                    modifier   = Modifier.clickable { collapsed = !collapsed }
                )
            }
        }

        // Collapsible log area
        AnimatedVisibility(
            visible = !collapsed,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            LazyColumn(
                state    = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(logHeight)
                    .padding(top = 10.dp)
            ) {
                items(logMessages) { msg ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text       = "›  ",
                            fontSize   = msgSize,
                            fontWeight = FontWeight.Bold,
                            color      = NeonCyan
                        )
                        Text(
                            text       = msg,
                            fontSize   = msgSize,
                            color      = GreyText,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}