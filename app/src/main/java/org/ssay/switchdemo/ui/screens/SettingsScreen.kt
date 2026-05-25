package org.ssay.switchdemo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * FIXED #23, #24, #25, #27, #28, #29, #45:
 *  - Validation: device name is trimmed of whitespace and unsupported characters
 *    are stripped before saving. Helper text shows the default name.
 *  - Blink rate slider now reads "Slow / Normal / Fast" with the ms value as a
 *    secondary detail.
 *  - "How it works" expandable card explains the IC and the resistor ladder in
 *    plain language.
 *  - Advanced section (collapsed by default) hides developer-only toggles like
 *    Raw Hex Mode.
 *  - Save confirmation handled by the Snackbar driven from the ViewModel.
 *  - Padding adapts to compact (<380dp) screens.
 *  - Every colour comes from MaterialTheme.colorScheme.
 */
@Composable
fun SettingsScreen(
    currentDeviceName: String,
    onDeviceNameChanged: (String) -> Unit,
    isDarkTheme: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
    showRawHex: Boolean,
    onShowRawHexChanged: (Boolean) -> Unit,
    showIndicatorLabels: Boolean,
    onShowIndicatorLabelsChanged: (Boolean) -> Unit,
    usePlainLabels: Boolean,
    onUsePlainLabelsChanged: (Boolean) -> Unit,
    blinkRateMs: Long,
    onBlinkRateMsChanged: (Long) -> Unit,
    isDemoMode: Boolean,
    onStartDemo: () -> Unit,
    onStopDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingName by remember(currentDeviceName) { mutableStateOf(currentDeviceName) }
    val sanitised = remember(editingName) { sanitiseDeviceName(editingName) }
    val hasChanged = sanitised != currentDeviceName && sanitised.isNotBlank()
    var showAdvanced by remember { mutableStateOf(false) }
    var showHowItWorks by remember { mutableStateOf(false) }

    val config = LocalConfiguration.current
    val hPad = if (config.screenWidthDp < 380) 14.dp else 20.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = hPad, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text       = "Settings",
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.secondary
        )

        // --- Bluetooth device name ---
        SettingsCard(title = "Bluetooth device name") {
            Text(
                text     = "Connect automatically to a board with this advertised name.",
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            OutlinedTextField(
                value         = editingName,
                onValueChange = { editingName = it },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                label         = { Text("Device Name") },
                supportingText = {
                    Text(
                        text     = if (sanitised != editingName)
                            "Will be saved as \"$sanitised\""
                        else "Default: RE_Switch_Dash",
                        fontSize = 11.sp
                    )
                },
                isError       = sanitised.isBlank() && editingName.isNotBlank(),
                shape         = RoundedCornerShape(8.dp)
            )
            Button(
                onClick  = { onDeviceNameChanged(sanitised) },
                enabled  = hasChanged,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape    = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text       = if (hasChanged) "Save changes" else "Saved",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- How it works (FIXED #25) ---
        ExpandableCard(
            title    = "How it works",
            expanded = showHowItWorks,
            onToggle = { showHowItWorks = !showHowItWorks }
        ) {
            Text(
                text       = "A 17-position rotary switch on the handlebar feeds a resistor ladder. " +
                             "The Elmos E521.39 IC measures the divider voltage, the PICO microcontroller " +
                             "polls it via ADC and broadcasts the JSON state over BLE — what you see on " +
                             "this dashboard is the live feed.",
                fontSize   = 13.sp,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = 18.sp
            )
        }

        // --- Display ---
        SettingsCard(title = "Display") {
            ToggleRow("Dark theme", "Switch between dark and light UI", isDarkTheme, onDarkThemeChanged)
            ToggleRow("Indicator labels", "Show LEFT / RIGHT / BEAM labels on the motorcycle",
                      showIndicatorLabels, onShowIndicatorLabelsChanged)
            ToggleRow("Plain language labels", "Use \"Low beam\" instead of \"DIPPER\", etc.",
                      usePlainLabels, onUsePlainLabelsChanged)
        }

        // --- Indicator blink speed (FIXED #24) ---
        SettingsCard(title = "Indicator blink speed") {
            val sliderValue = blinkRateMs.toFloat()
            val speedLabel = when {
                blinkRateMs >= 800 -> "Slow"
                blinkRateMs <= 250 -> "Fast"
                else                -> "Normal"
            }
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(text = speedLabel, fontWeight = FontWeight.Bold,
                     color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Text(text = "${blinkRateMs} ms",
                     color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            Slider(
                value         = sliderValue,
                onValueChange = { onBlinkRateMsChanged(it.toLong()) },
                valueRange    = 100f..1000f,
                steps         = 17
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Fast",   fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text("Normal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text("Slow",   fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }

        // --- Demo mode ---
        SettingsCard(title = "Simulated demo mode") {
            Text(
                text     = "Cycle through every switch state without any hardware. The same toggle is also " +
                           "available from the dashboard FAB.",
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick  = if (isDemoMode) onStopDemo else onStartDemo,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (isDemoMode) MaterialTheme.colorScheme.error
                                     else            MaterialTheme.colorScheme.tertiary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary
                ),
                shape    = RoundedCornerShape(8.dp)
            ) {
                Text(text = if (isDemoMode) "Stop demo" else "Start demo", fontWeight = FontWeight.Bold)
            }
        }

        // --- Advanced (FIXED #27) ---
        ExpandableCard(
            title    = "Advanced",
            expanded = showAdvanced,
            onToggle = { showAdvanced = !showAdvanced }
        ) {
            ToggleRow(
                "Raw hex log",
                "Display each log line as raw hexadecimal bytes (developer use)",
                showRawHex,
                onShowRawHexChanged
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * FIXED #23: trims whitespace and removes characters that wouldn't appear in a
 * standard BLE adv name (control chars, leading/trailing spaces).
 */
private fun sanitiseDeviceName(input: String): String =
    input.trim().filter { it.isLetterOrDigit() || it == '_' || it == '-' || it == ' ' }

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
private fun ExpandableCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClick = onToggle)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text       = title,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector       = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint              = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier            = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content             = content
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, subtext: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            Text(text = subtext, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
