package org.ssay.switchdemo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ssay.switchdemo.ui.theme.*

@Composable
fun SettingsScreen(
    currentDeviceName: String,
    onDeviceNameChanged: (String) -> Unit,
    isDarkTheme: Boolean = true,
    onDarkThemeChanged: (Boolean) -> Unit = {},
    showRawHex: Boolean = false,
    onShowRawHexChanged: (Boolean) -> Unit = {},
    showIndicatorLabels: Boolean = true,
    onShowIndicatorLabelsChanged: (Boolean) -> Unit = {},
    blinkRateMs: Long = 500L,
    onBlinkRateMsChanged: (Long) -> Unit = {},
    isDemoMode: Boolean = false,
    onStartDemo: () -> Unit = {},
    onStopDemo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var editingName by remember { mutableStateOf(currentDeviceName) }
    var hasChanged  by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(text = "Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NeonPink)

        // BLE Device Name
        SettingsCard("Bluetooth Device Name") {
            Text(text = "Enter the name of your BLE device to connect automatically", fontSize = 12.sp, color = GreyText)
            OutlinedTextField(
                value = editingName,
                onValueChange = { editingName = it; hasChanged = it != currentDeviceName },
                modifier   = Modifier.fillMaxWidth(),
                singleLine = true,
                label      = { Text("Device Name", color = GreyText) },
                colors     = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WhiteText, unfocusedTextColor = LightGreyText,
                    focusedBorderColor = NeonCyan, unfocusedBorderColor = CardBorder,
                    cursorColor = NeonCyan, focusedLabelColor = NeonCyan, unfocusedLabelColor = GreyText
                ),
                shape = RoundedCornerShape(8.dp)
            )
            Text(text = "Current: $currentDeviceName", fontSize = 11.sp, color = GreyText)
            Button(
                onClick  = { onDeviceNameChanged(editingName); hasChanged = false },
                enabled  = hasChanged && editingName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground, disabledContainerColor = CardBorder, disabledContentColor = GreyText),
                shape    = RoundedCornerShape(8.dp)
            ) {
                Text(text = if (hasChanged) "Save Changes" else "Saved", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // Display
        SettingsCard("Display") {
            SettingsToggleRow("Dark Theme",        "Switch between dark and light UI",              isDarkTheme,         onDarkThemeChanged)
            SettingsToggleRow("Indicator Labels",  "Show text overlays on motorcycle graphic",      showIndicatorLabels, onShowIndicatorLabelsChanged)
        }

        // Log
        SettingsCard("Data Log") {
            SettingsToggleRow("Raw Hex Mode", "Display log messages as hex bytes", showRawHex, onShowRawHexChanged)
        }

        // Blink Rate
        SettingsCard("Indicator Blink Rate") {
            Text(text = "Half-period: ${blinkRateMs} ms  (full cycle: ${blinkRateMs * 2} ms)", fontSize = 12.sp, color = NeonCyan)
            Slider(
                value         = blinkRateMs.toFloat(),
                onValueChange = { onBlinkRateMsChanged(it.toLong()) },
                valueRange    = 100f..1000f,
                steps         = 17,
                colors        = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = CardBorder)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Fast (100ms)", fontSize = 10.sp, color = GreyText)
                Text(text = "Slow (1000ms)", fontSize = 10.sp, color = GreyText)
            }
        }

        // Demo Mode
        SettingsCard("Simulated Demo Mode") {
            Text(text = "Cycles through all switch states without needing a real device. Useful for testing and presentations.", fontSize = 12.sp, color = GreyText)
            Spacer(Modifier.height(4.dp))
            Button(
                onClick  = if (isDemoMode) onStopDemo else onStartDemo,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = if (isDemoMode) NeonPink else NeonGreen, contentColor = DarkBackground),
                shape    = RoundedCornerShape(8.dp)
            ) {
                Text(text = if (isDemoMode) "Stop Demo" else "Start Demo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WhiteText)
        content()
    }
}

@Composable
private fun SettingsToggleRow(label: String, subtext: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label,   fontSize = 14.sp, color = LightGreyText, fontWeight = FontWeight.Medium)
            Text(text = subtext, fontSize = 11.sp, color = GreyText)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = NeonCyan, uncheckedThumbColor = GreyText, uncheckedTrackColor = CardBorder)
        )
    }
}