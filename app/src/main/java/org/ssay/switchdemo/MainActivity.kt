package org.ssay.switchdemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ssay.switchdemo.data.BleConnectionState
import org.ssay.switchdemo.ui.components.BleConnectBar
import org.ssay.switchdemo.ui.components.BottomNavBar
import org.ssay.switchdemo.ui.screens.AboutScreen
import org.ssay.switchdemo.ui.screens.DashboardScreen
import org.ssay.switchdemo.ui.screens.SettingsScreen
import org.ssay.switchdemo.ui.theme.SwitchDemonstratorTheme
import org.ssay.switchdemo.viewmodel.DashboardViewModel

class MainActivity : ComponentActivity() {

    private val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request BLE permissions on launch
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { /* Results handled — BLE operations check individually */ }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }

        enableEdgeToEdge()
        setContent {
            SwitchDemonstratorTheme {
                SwitchDemoApp()
            }
        }
    }
}

@Composable
fun SwitchDemoApp(
    viewModel: DashboardViewModel = viewModel()
) {
    val switchState by viewModel.switchState.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val logMessages by viewModel.logMessages.collectAsStateWithLifecycle()
    val voltage by viewModel.voltage.collectAsStateWithLifecycle()
    val isWarning by viewModel.isWarning.collectAsStateWithLifecycle()
    val deviceName by viewModel.bleDeviceName.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf("dashboard") }

    val backgroundColor = Color(0xFF050710)

    Scaffold(
        containerColor = backgroundColor,
        bottomBar = {
            Column {
                BleConnectBar(
                    connectionState = connectionState,
                    onToggle = { viewModel.toggleBleConnection() }
                )
                BottomNavBar(
                    currentScreen = currentScreen,
                    onNavigate = { currentScreen = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(backgroundColor)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "screenTransition"
            ) { screen ->
                when (screen) {
                    "dashboard" -> DashboardScreen(
                        switchState = switchState,
                        voltage = voltage,
                        isWarning = isWarning,
                        logMessages = logMessages,
                        bleConnectionState = connectionState
                    )
                    "settings" -> SettingsScreen(
                        currentDeviceName = deviceName,
                        onDeviceNameChanged = { viewModel.updateDeviceName(it) }
                    )
                    "about" -> AboutScreen()
                }
            }
        }
    }
}
