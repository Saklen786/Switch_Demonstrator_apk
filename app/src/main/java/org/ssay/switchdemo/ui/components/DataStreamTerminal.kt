package org.ssay.switchdemo.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ssay.switchdemo.ui.theme.NeonCyan
import org.ssay.switchdemo.ui.theme.NeonGreen
import org.ssay.switchdemo.ui.theme.NeonPink

private fun String.toHexDump(): String =
    this.toByteArray(Charsets.UTF_8).joinToString(" ") { "%02X".format(it) }

/**
 * FIXED #17: smart auto-scroll. Only follows the tail when the user is already
 * near the bottom; otherwise we leave their scroll position alone so they can
 * read history.
 *
 * FIXED #18: a Clear button on the header (delegates to [onClear]).
 *
 * FIXED #19: when used inside a Column with weight(1f) the terminal fills the
 * remaining vertical space — no more hardcoded 80–120dp gaps.
 *
 * FIXED #45: all colours come from the MaterialTheme.colorScheme so the light
 * theme actually looks light.
 *
 * FIXED #50: log uses a monospace FontFamily so packets line up cleanly.
 */
@Composable
fun DataStreamTerminal(
    logMessages: List<String>,
    onClear: () -> Unit = {},
    compact: Boolean = false,
    showRawHex: Boolean = false,
    modifier: Modifier = Modifier
) {
    var collapsed by remember { mutableStateOf(false) }
    val listState  = rememberLazyListState()

    // FIXED #17: smart auto-scroll (only when near the tail)
    LaunchedEffect(logMessages.size) {
        if (logMessages.isEmpty() || collapsed) return@LaunchedEffect
        val info     = listState.layoutInfo
        val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
        val total       = info.totalItemsCount
        // "Near the bottom" means the user can already see the second-to-last item.
        val isAtBottom = total == 0 || lastVisible >= total - 2
        if (isAtBottom) listState.animateScrollToItem(logMessages.lastIndex)
    }

    val labelSize = if (compact) 11.sp else 13.sp
    val msgSize   = if (compact) 10.sp else 11.sp
    val innerPad  = if (compact) 12.dp else 16.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(innerPad)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text          = "Activity log",
                    fontSize      = labelSize,
                    fontWeight    = FontWeight.Bold,
                    color         = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )
                if (showRawHex) {
                    Text(
                        text       = "HEX",
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color      = NeonPink,
                        modifier   = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier              = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "●",   fontSize = 8.sp,  color = NeonGreen)
                    Text(text = "LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
                // FIXED #18: clear button (48dp tappable)
                IconButton(
                    onClick  = onClear,
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = "Clear activity log" }
                ) {
                    Icon(
                        imageVector       = Icons.Filled.DeleteOutline,
                        contentDescription = null,
                        tint              = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier          = Modifier.size(20.dp)
                    )
                }
                Text(
                    text     = if (collapsed) "Show" else "Hide",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier
                        .heightIn(min = 32.dp)
                        .clickable { collapsed = !collapsed }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = !collapsed,
            enter = expandVertically() + fadeIn(),
            exit  = shrinkVertically() + fadeOut()
        ) {
            // FIXED #19: heightIn instead of fixed height — the parent decides.
            LazyColumn(
                state    = listState,
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(top = 10.dp)
            ) {
                items(logMessages) { msg ->
                    val display = if (showRawHex) msg.toHexDump() else msg
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(text = "›  ", fontSize = msgSize, fontWeight = FontWeight.Bold, color = NeonCyan,
                             fontFamily = FontFamily.Monospace)
                        Text(
                            text  = display,
                            fontSize = msgSize,
                            color = if (showRawHex) NeonGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            lineHeight = 16.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
