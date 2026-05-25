package org.ssay.switchdemo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * FIXED #5: explicit dialog when Bluetooth is off, with a button that opens
 * Settings -> Bluetooth via ACTION_BLUETOOTH_SETTINGS.
 */
@Composable
fun BluetoothOffDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Filled.BluetoothDisabled, null) },
        title = { Text("Bluetooth is off") },
        text  = { Text("Turn Bluetooth on to connect to your demonstrator board.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Open Bluetooth settings") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * FIXED #6: dialog shown when the user denied (or permanently denied) Bluetooth
 * permissions, with a deep-link to the app's settings page.
 */
@Composable
fun PermissionDeniedDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Filled.Lock, null) },
        title = { Text("Bluetooth permission needed") },
        text  = {
            Text(
                "This app needs Bluetooth scan and connect permission to find the " +
                "demonstrator board. Open settings to grant it."
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Open app settings") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * FIXED #10: confirms a disconnect so a presenter doesn't break the demo by
 * mis-tapping the bar.
 */
@Composable
fun DisconnectConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Filled.Warning, null) },
        title = { Text("Disconnect from device?") },
        text  = { Text("You can reconnect at any time by tapping the connection bar.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Disconnect") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
