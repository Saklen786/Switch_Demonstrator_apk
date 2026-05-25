package org.ssay.switchdemo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ssay.switchdemo.R
import org.ssay.switchdemo.ui.components.DataStreamTerminal
import org.ssay.switchdemo.ui.components.DemoBanner
import org.ssay.switchdemo.ui.components.MotorcycleGraphic
import org.ssay.switchdemo.ui.components.StatusIndicator
import org.ssay.switchdemo.ui.components.VoltageGauge
import org.ssay.switchdemo.viewmodel.DashboardUiState

/**
 * FIXED #3, #4, #11, #15, #19, #20, #26, #44, #45:
 *  - Window-size-class branching (Compact / Medium / Expanded) instead of raw dp checks.
 *  - In landscape / wide layouts, a true two-column grid: motorcycle on the left,
 *    voltage + log on the right. The log uses weight(1f) so it grows to fill space.
 *  - Two-row header with breathing room: title row, then secondary chip row
 *    (firmware / status / demo badge).
 *  - Prominent demo banner at the top + FAB to start/stop without leaving the screen.
 *  - Single `state: DashboardUiState` parameter instead of 14 individual ones.
 *  - All colours route through MaterialTheme.colorScheme.
 */
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    widthSizeClass: WindowWidthSizeClass,
    onToggleDemo: () -> Unit,
    onClearLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config    = LocalConfiguration.current
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact
    val isExpanded = widthSizeClass == WindowWidthSizeClass.Expanded
    val isLandscapeWide = config.screenWidthDp >= config.screenHeightDp && !isCompact

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // FIXED #20: prominent banner pinned to the top
        DemoBanner(
            visible    = state.isDemoMode,
            onStopDemo = onToggleDemo
        )

        Box(modifier = Modifier.weight(1f)) {
            val hPad = when {
                isExpanded -> 28.dp
                isCompact && config.screenWidthDp < 360 -> 12.dp
                else -> 18.dp
            }
            val vPad = if (isExpanded) 22.dp else 14.dp

            if (isLandscapeWide) {
                // FIXED #3: real two-column layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = hPad, vertical = vPad),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left column: header + motorcycle
                    Column(
                        modifier             = Modifier.weight(1.1f),
                        verticalArrangement  = Arrangement.spacedBy(14.dp)
                    ) {
                        DashboardHeader(state = state, isCompact = isCompact, isExpanded = isExpanded)
                        MotorcycleGraphic(
                            switchState         = state.switchState,
                            blinkRateMs         = state.blinkRateMs,
                            showIndicatorLabels = state.showIndicatorLabels,
                            usePlainLabels      = state.usePlainLabels,
                            hasLiveData         = state.hasLiveData
                        )
                    }
                    // Right column: voltage + log (log expands)
                    Column(
                        modifier            = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        VoltageGauge(
                            voltage     = state.voltage,
                            hasLiveData = state.hasLiveData,
                            isWarning   = state.isWarning,
                            compact     = false
                        )
                        // FIXED #19: log fills remaining space
                        DataStreamTerminal(
                            logMessages = state.logMessages,
                            onClear     = onClearLog,
                            compact     = false,
                            showRawHex  = state.showRawHex,
                            modifier    = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Column(
                    modifier              = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = hPad, vertical = vPad),
                    verticalArrangement   = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp)
                ) {
                    DashboardHeader(state = state, isCompact = isCompact, isExpanded = isExpanded)
                    VoltageGauge(
                        voltage     = state.voltage,
                        hasLiveData = state.hasLiveData,
                        isWarning   = state.isWarning,
                        compact     = isCompact
                    )
                    MotorcycleGraphic(
                        switchState         = state.switchState,
                        blinkRateMs         = state.blinkRateMs,
                        showIndicatorLabels = state.showIndicatorLabels,
                        usePlainLabels      = state.usePlainLabels,
                        hasLiveData         = state.hasLiveData
                    )
                    DataStreamTerminal(
                        logMessages = state.logMessages,
                        onClear     = onClearLog,
                        compact     = isCompact,
                        showRawHex  = state.showRawHex
                    )
                }
            }

            // FIXED #26: dashboard FAB to toggle demo mode without going to Settings
            ExtendedFloatingActionButton(
                onClick    = onToggleDemo,
                modifier   = Modifier.align(Alignment.BottomEnd).padding(20.dp).semantics {
                    contentDescription = "Toggle simulated demo mode"
                },
                containerColor = if (state.isDemoMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    Icon(if (state.isDemoMode) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                         contentDescription = null)
                },
                text = { Text(if (state.isDemoMode) "Stop demo" else "Demo", fontWeight = FontWeight.Bold) }
            )
        }
    }
}

/**
 * FIXED #11: the previous header crammed everything into one Row.
 * Now logo + title sit on the first row; firmware / demo badge / status sit
 * in a chip strip on the second row.
 */
@Composable
private fun DashboardHeader(
    state: DashboardUiState,
    isCompact: Boolean,
    isExpanded: Boolean
) {
    val titleSize    = if (isExpanded) 24.sp else if (isCompact) 18.sp else 21.sp
    val subtitleSize = if (isExpanded) 13.sp else 11.sp
    val logoSize     = if (isExpanded) 56.dp else if (isCompact) 38.dp else 46.dp

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(logoSize).graphicsLayer { /* keep alpha = 1 in light theme too */ }
            ) {
                Image(
                    painter            = painterResource(R.drawable.elmos_logo),
                    contentDescription = "Elmos logo",
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Fit
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "E521.39 IC",
                    fontSize   = titleSize,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary
                )
                Text(
                    text          = "Switch Demonstrator",
                    fontSize      = subtitleSize,
                    color         = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    letterSpacing = 1.sp
                )
            }
            // FIXED #7: bigger animated indicator with explicit label
            StatusIndicator(
                state            = state.connectionState,
                reconnectAttempt = state.reconnectAttempt,
                showLabel        = !isCompact,
                dotSize          = if (isCompact) 12.dp else 16.dp
            )
        }

        // Secondary chip row (firmware, demo) — only visible when there is something to show.
        if (state.firmwareVersion != null || state.isDemoMode) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.firmwareVersion != null) {
                    HeaderChip(
                        label   = "FW ${state.firmwareVersion}",
                        accent  = MaterialTheme.colorScheme.tertiary
                    )
                }
                if (state.isDemoMode) {
                    HeaderChip(label = "DEMO", accent = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun HeaderChip(label: String, accent: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text       = label,
            color      = accent,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
