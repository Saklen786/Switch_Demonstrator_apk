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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ssay.switchdemo.data.BleConnectionState
import org.ssay.switchdemo.data.SwitchState
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
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) permissionLauncher.launch(missingPermissions.toTypedArray())

        enableEdgeToEdge()
        setContent {
            val vm: DashboardViewModel = viewModel()
            val isDarkTheme by vm.isDarkTheme.collectAsStateWithLifecycle()
            SwitchDemonstratorTheme(darkTheme = isDarkTheme) {
                SwitchDemoApp(viewModel = vm)
            }
        }
    }
}

@Composable
fun SwitchDemoApp(viewModel: DashboardViewModel = viewModel()) {
    val connectionState      by viewModel.connectionState.collectAsStateWithLifecycle()
    val effectiveSwitchState by viewModel.effectiveSwitchState.collectAsStateWithLifecycle()
    val effectiveVoltage     by viewModel.effectiveVoltage.collectAsStateWithLifecycle()
    val effectiveIsWarning   by viewModel.effectiveIsWarning.collectAsStateWithLifecycle()
    val effectiveLogMessages by viewModel.effectiveLogMessages.collectAsStateWithLifecycle()
    val deviceName           by viewModel.bleDeviceName.collectAsStateWithLifecycle()
    val isDarkTheme          by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val isDemoMode           by viewModel.isDemoMode.collectAsStateWithLifecycle()
    val showRawHex           by viewModel.showRawHex.collectAsStateWithLifecycle()
    val showIndicatorLabels  by viewModel.showIndicatorLabels.collectAsStateWithLifecycle()
    val blinkRateMs          by viewModel.blinkRateMs.collectAsStateWithLifecycle()
    val firmwareVersion      by viewModel.firmwareVersion.collectAsStateWithLifecycle()
    val scanTimedOut         by viewModel.scanTimedOut.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf("dashboard") }
    val haptic = LocalHapticFeedback.current

    // Haptic on connection state change
    var prevConnection by remember { mutableStateOf(connectionState) }
    LaunchedEffect(connectionState) {
        if (connectionState != prevConnection) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            prevConnection = connectionState
        }
    }
    // Haptic on horn
    LaunchedEffect(effectiveSwitchState.horn) {
        if (effectiveSwitchState.horn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val config      = LocalConfiguration.current
    val isLandscape = config.screenWidthDp > config.screenHeightDp
    val bgColor     = Color(0xFF050710)

    if (isLandscape) {
        // Landscape: sidebar left, content right
        Row(modifier = Modifier.fillMaxSize().background(bgColor).systemBarsPadding()) {
            Column(modifier = Modifier.width(160.dp).fillMaxHeight()) {
                BleConnectBar(
                    connectionState = connectionState,
                    onToggle        = { viewModel.toggleBleConnection() },
                    scanTimedOut    = scanTimedOut,
                    modifier        = Modifier.fillMaxWidth().weight(1f)
                )
                BottomNavBar(
                    currentScreen = currentScreen,
                    onNavigate    = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentScreen = it }
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(bgColor)) {
                ScreenContent(currentScreen, viewModel, connectionState, effectiveSwitchState, effectiveVoltage, effectiveIsWarning, effectiveLogMessages, deviceName, isDarkTheme, isDemoMode, showRawHex, showIndicatorLabels, blinkRateMs, firmwareVersion)
            }
        }
    } else {
        // Portrait: original layout
        Scaffold(
            containerColor = bgColor,
            bottomBar = {
                Column {
                    BleConnectBar(connectionState = connectionState, onToggle = { viewModel.toggleBleConnection() }, scanTimedOut = scanTimedOut)
                    BottomNavBar(currentScreen = currentScreen, onNavigate = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentScreen = it })
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(bgColor)) {
                ScreenContent(currentScreen, viewModel, connectionState, effectiveSwitchState, effectiveVoltage, effectiveIsWarning, effectiveLogMessages, deviceName, isDarkTheme, isDemoMode, showRawHex, showIndicatorLabels, blinkRateMs, firmwareVersion)
            }
        }
    }
}

@Composable
private fun ScreenContent(
    currentScreen: String,
    viewModel: DashboardViewModel,
    connectionState: BleConnectionState,
    effectiveSwitchState: SwitchState,
    effectiveVoltage: Float,
    effectiveIsWarning: Boolean,
    effectiveLogMessages: List<String>,
    deviceName: String,
    isDarkTheme: Boolean,
    isDemoMode: Boolean,
    showRawHex: Boolean,
    showIndicatorLabels: Boolean,
    blinkRateMs: Long,
    firmwareVersion: String?
) {
    AnimatedContent(targetState = currentScreen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screenTransition") { screen ->
        when (screen) {
            "dashboard" -> DashboardScreen(
                switchState         = effectiveSwitchState,
                voltage             = effectiveVoltage,
                isWarning           = effectiveIsWarning,
                logMessages         = effectiveLogMessages,
                bleConnectionState  = connectionState,
                blinkRateMs         = blinkRateMs,
                showIndicatorLabels = showIndicatorLabels,
                showRawHex          = showRawHex,
                isDemoMode          = isDemoMode,
                firmwareVersion     = firmwareVersion
            )
            "settings" -> SettingsScreen(
                currentDeviceName            = deviceName,
                onDeviceNameChanged          = { viewModel.updateDeviceName(it) },
                isDarkTheme                  = isDarkTheme,
                onDarkThemeChanged           = { viewModel.setDarkTheme(it) },
                showRawHex                   = showRawHex,
                onShowRawHexChanged          = { viewModel.setShowRawHex(it) },
                showIndicatorLabels          = showIndicatorLabels,
                onShowIndicatorLabelsChanged = { viewModel.setShowIndicatorLabels(it) },
                blinkRateMs                  = blinkRateMs,
                onBlinkRateMsChanged         = { viewModel.setBlinkRateMs(it) },
                isDemoMode                   = isDemoMode,
                onStartDemo                  = { viewModel.startDemoMode() },
                onStopDemo                   = { viewModel.stopDemoMode() }
            )
            "about" -> AboutScreen(firmwareVersion = firmwareVersion)
        }
    }
}