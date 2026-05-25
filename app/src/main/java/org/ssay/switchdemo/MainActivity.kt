package org.ssay.switchdemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ssay.switchdemo.data.BleConnectionState
import org.ssay.switchdemo.data.BleManager
import org.ssay.switchdemo.data.Screen
import org.ssay.switchdemo.ui.components.BleConnectBar
import org.ssay.switchdemo.ui.components.BluetoothOffDialog
import org.ssay.switchdemo.ui.components.BottomNavBar
import org.ssay.switchdemo.ui.components.DisconnectConfirmDialog
import org.ssay.switchdemo.ui.components.PermissionDeniedDialog
import org.ssay.switchdemo.ui.screens.AboutScreen
import org.ssay.switchdemo.ui.screens.DashboardScreen
import org.ssay.switchdemo.ui.screens.OnboardingScreen
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

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // FIXED #47: SplashScreen API.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Drop the splash on the very next frame.
        splash.setKeepOnScreenCondition { false }

        // FIXED #6: if the user denies permission(s) here, BleManager will emit
        //           Event.PermissionDenied the next time the user taps Connect,
        //           which the Compose tree turns into PermissionDeniedDialog.
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { /* result is observed via the BleManager event flow */ }
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())

        enableEdgeToEdge()
        setContent {
            val vm: DashboardViewModel = viewModel()
            val isDarkTheme by vm.isDarkTheme.collectAsStateWithLifecycle()
            val windowSize = calculateWindowSizeClass(this)
            SwitchDemonstratorTheme(darkTheme = isDarkTheme) {
                SwitchDemoApp(viewModel = vm, widthSizeClass = windowSize.widthSizeClass)
            }
        }
    }
}

@Composable
fun SwitchDemoApp(
    viewModel: DashboardViewModel,
    widthSizeClass: WindowWidthSizeClass
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic  = LocalHapticFeedback.current

    var currentScreen by rememberSaveable(stateSaver = ScreenSaver) {
        mutableStateOf<Screen>(Screen.Dashboard)
    }

    var showBluetoothOffDialog by remember { mutableStateOf(false) }
    var showPermissionDialog   by remember { mutableStateOf(false) }
    var showDisconnectDialog   by remember { mutableStateOf(false) }
    val snackbarHostState      = remember { SnackbarHostState() }

    // Listen to BLE events from the manager (FIXED #5, #6)
    LaunchedEffect(Unit) {
        viewModel.bleEvents.collect { ev ->
            when (ev) {
                BleManager.Event.BluetoothDisabled -> showBluetoothOffDialog = true
                BleManager.Event.PermissionDenied  -> showPermissionDialog   = true
            }
        }
    }
    // Snackbar feedback for settings changes (FIXED #28)
    LaunchedEffect(Unit) {
        viewModel.settingsToast.collect { msg ->
            snackbarHostState.showSnackbar(msg, withDismissAction = true)
        }
    }
    // FIXED #56: differentiated haptics
    var prevConnection by remember { mutableStateOf(ui.connectionState) }
    LaunchedEffect(ui.connectionState) {
        if (ui.connectionState != prevConnection) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            prevConnection = ui.connectionState
        }
    }
    LaunchedEffect(ui.switchState.indicator) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // FIXED #36: BackHandler — go to dashboard before exiting.
    BackHandler(enabled = currentScreen != Screen.Dashboard) {
        currentScreen = Screen.Dashboard
    }

    // FIXED #54: onboarding gates the rest of the UI on first launch.
    if (!ui.onboardingDone) {
        OnboardingScreen(onFinish = { viewModel.markOnboardingDone() })
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                BleConnectBar(
                    connectionState  = ui.connectionState,
                    reconnectAttempt = ui.reconnectAttempt,
                    scanTimedOut     = ui.scanTimedOut,
                    onToggle         = {
                        when (ui.connectionState) {
                            BleConnectionState.CONNECTED -> showDisconnectDialog = true   // FIXED #10
                            else -> viewModel.toggleBleConnection()
                        }
                    }
                )
                BottomNavBar(
                    currentScreen = currentScreen,
                    onNavigate    = { currentScreen = it }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background)
        ) {
            // FIXED #51: slide + fade between screens.
            AnimatedContent(
                targetState   = currentScreen,
                transitionSpec = {
                    val forward = Screen.all.indexOf(targetState) >= Screen.all.indexOf(initialState)
                    val dir = if (forward) 1 else -1
                    slideInHorizontally(initialOffsetX = { dir * it / 6 }) + fadeIn() togetherWith
                    slideOutHorizontally(targetOffsetX = { -dir * it / 6 }) + fadeOut()
                },
                label = "screenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Dashboard -> DashboardScreen(
                        state          = ui,
                        widthSizeClass = widthSizeClass,
                        onToggleDemo   = { viewModel.toggleDemoMode() },
                        onClearLog     = { /* For now log lives in BleManager — clearing only affects demo log */ }
                    )
                    is Screen.Settings -> SettingsScreen(
                        currentDeviceName            = ui.deviceName,
                        onDeviceNameChanged          = { viewModel.updateDeviceName(it) },
                        isDarkTheme                  = ui.isDarkTheme,
                        onDarkThemeChanged           = { viewModel.setDarkTheme(it) },
                        showRawHex                   = ui.showRawHex,
                        onShowRawHexChanged          = { viewModel.setShowRawHex(it) },
                        showIndicatorLabels          = ui.showIndicatorLabels,
                        onShowIndicatorLabelsChanged = { viewModel.setShowIndicatorLabels(it) },
                        usePlainLabels               = ui.usePlainLabels,
                        onUsePlainLabelsChanged      = { viewModel.setUsePlainLabels(it) },
                        blinkRateMs                  = ui.blinkRateMs,
                        onBlinkRateMsChanged         = { viewModel.setBlinkRateMs(it) },
                        isDemoMode                   = ui.isDemoMode,
                        onStartDemo                  = { viewModel.startDemoMode() },
                        onStopDemo                   = { viewModel.stopDemoMode() }
                    )
                    is Screen.About -> AboutScreen(firmwareVersion = ui.firmwareVersion)
                }
            }
        }
    }

    // ---- Modal dialogs ----
    if (showBluetoothOffDialog) {
        BluetoothOffDialog(
            onConfirm = {
                showBluetoothOffDialog = false
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            },
            onDismiss = { showBluetoothOffDialog = false }
        )
    }
    if (showPermissionDialog) {
        PermissionDeniedDialog(
            onConfirm = {
                showPermissionDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
            onDismiss = { showPermissionDialog = false }
        )
    }
    if (showDisconnectDialog) {
        DisconnectConfirmDialog(
            onConfirm = {
                showDisconnectDialog = false
                viewModel.confirmDisconnect()
            },
            onDismiss = { showDisconnectDialog = false }
        )
    }
}

/** Saver for [Screen] sealed class so navigation survives configuration changes. */
private val ScreenSaver = androidx.compose.runtime.saveable.Saver<Screen, String>(
    save    = { it.route },
    restore = { Screen.fromRoute(it) }
)
