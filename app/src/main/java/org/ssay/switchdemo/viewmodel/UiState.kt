package org.ssay.switchdemo.viewmodel

import org.ssay.switchdemo.data.BleConnectionState
import org.ssay.switchdemo.data.SwitchState

/**
 * FIXED #44: replaces the previous 14-parameter ScreenContent() composable.
 * Bundles every display-related field that flows top-down through the UI.
 */
data class DashboardUiState(
    val switchState: SwitchState         = SwitchState(),
    val voltage: Float                   = 0f,
    val isWarning: Boolean               = false,
    val logMessages: List<String>        = listOf("System ready."),
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val reconnectAttempt: Int            = 0,
    val deviceName: String               = "RE_Switch_Dash",
    val isDarkTheme: Boolean             = true,
    val isDemoMode: Boolean              = false,
    val showRawHex: Boolean              = false,
    val showIndicatorLabels: Boolean     = true,
    val usePlainLabels: Boolean          = true,
    val blinkRateMs: Long                = 500L,
    val firmwareVersion: String?         = null,
    val scanTimedOut: Boolean            = false,
    val onboardingDone: Boolean          = false
) {
    /** True when the gauge / motorcycle should treat data as "live". */
    val hasLiveData: Boolean
        get() = isDemoMode || connectionState == BleConnectionState.CONNECTED
}
