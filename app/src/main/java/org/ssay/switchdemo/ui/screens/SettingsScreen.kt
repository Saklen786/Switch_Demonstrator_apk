package org.ssay.switchdemo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ssay.switchdemo.ui.theme.*

@Composable
fun SettingsScreen(
    currentDeviceName: String,
    onDeviceNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingName by remember { mutableStateOf(currentDeviceName) }
    var hasChanged by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Text(
            text = "Settings",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = NeonPink
        )

        // BLE Device Name Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Bluetooth Device Name",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = WhiteText
            )
            Text(
                text = "Enter the name of your BLE device to connect automatically",
                fontSize = 12.sp,
                color = GreyText
            )

            OutlinedTextField(
                value = editingName,
                onValueChange = {
                    editingName = it
                    hasChanged = it != currentDeviceName
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Device Name", color = GreyText) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WhiteText,
                    unfocusedTextColor = LightGreyText,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CardBorder,
                    cursorColor = NeonCyan,
                    focusedLabelColor = NeonCyan,
                    unfocusedLabelColor = GreyText
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Current value display
            Text(
                text = "Current: $currentDeviceName",
                fontSize = 11.sp,
                color = GreyText
            )

            // Save button
            Button(
                onClick = {
                    onDeviceNameChanged(editingName)
                    hasChanged = false
                },
                enabled = hasChanged && editingName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = DarkBackground,
                    disabledContainerColor = CardBorder,
                    disabledContentColor = GreyText
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (hasChanged) "Save Changes" else "Saved",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
