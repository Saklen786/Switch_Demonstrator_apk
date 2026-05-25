package org.ssay.switchdemo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.ssay.switchdemo.data.*

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    // FIXED #38: BleManager now uses the ViewModel's scope, so its background
    //            coroutines are cancelled automatically when the VM is cleared.
    // FIXED #39: SharedPreferences replaced with DataStore via SettingsRepository.
    // FIXED #42: AudioManager renamed to SoundManager (no clash with android.media.AudioManager).
    private val bleManager     = BleManager(application, viewModelScope)
    private val soundManager   = SoundManager(application)
    private val settings       = SettingsRepository(application)

    val connectionState   = bleManager.connectionState
    val switchState       = bleManager.switchState
    val logMessages       = bleManager.logMessages
    val firmwareVersion   = bleManager.firmwareVersion
    val scanTimedOut      = bleManager.scanTimedOut
    val reconnectAttempt  = bleManager.reconnectAttempt
    val bleEvents         = bleManager.events

    val bleDeviceName: StateFlow<String>      = settings.deviceName.stateInVm("RE_Switch_Dash")
    val blinkRateMs: StateFlow<Long>          = settings.blinkRateMs.map { it.toLong() }.stateInVm(500L)
    val isDarkTheme: StateFlow<Boolean>       = settings.isDarkTheme.stateInVm(true)
    val showRawHex: StateFlow<Boolean>        = settings.showRawHex.stateInVm(false)
    val showIndicatorLabels: StateFlow<Boolean> = settings.showIndicatorLabels.stateInVm(true)
    val usePlainLabels: StateFlow<Boolean>    = settings.usePlainLabels.stateInVm(true)
    val onboardingDone: StateFlow<Boolean>    = settings.onboardingDone.stateInVm(false)

    val voltage: StateFlow<Float>   = switchState.map { it.adcVolts }.stateInVm(0f)
    val isWarning: StateFlow<Boolean> = switchState.map { it.warn || it.horn }.stateInVm(false)

    private val _isDemoMode      = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _demoSwitchState = MutableStateFlow(SwitchState())
    private val _demoVoltage     = MutableStateFlow(0f)
    private val _demoLogMessages = MutableStateFlow(
        listOf("Demo mode active.", "Cycling through switch states...")
    )

    private val _settingsToast   = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** FIXED #28: the UI listens to this flow and shows a Snackbar. */
    val settingsToast: SharedFlow<String> = _settingsToast.asSharedFlow()

    val effectiveSwitchState: StateFlow<SwitchState> = combine(
        _isDemoMode, switchState, _demoSwitchState
    ) { demo, real, demoState -> if (demo) demoState else real }
        .stateInVm(SwitchState())

    val effectiveVoltage: StateFlow<Float> = combine(
        _isDemoMode, voltage, _demoVoltage
    ) { demo, realV, demoV -> if (demo) demoV else realV }
        .stateInVm(0f)

    val effectiveLogMessages: StateFlow<List<String>> = combine(
        _isDemoMode, logMessages, _demoLogMessages
    ) { demo, realLog, demoLog -> if (demo) demoLog else realLog }
        .stateInVm(listOf("System ready."))

    val effectiveIsWarning: StateFlow<Boolean> = effectiveSwitchState
        .map { it.warn || it.horn }
        .stateInVm(false)

    /** FIXED #44: the single object the screen-level composables read from. */
    val uiState: StateFlow<DashboardUiState> = combine(
        listOf(
            effectiveSwitchState, effectiveVoltage, effectiveIsWarning,
            effectiveLogMessages, connectionState, reconnectAttempt,
            bleDeviceName, isDarkTheme, _isDemoMode,
            showRawHex, showIndicatorLabels, usePlainLabels,
            blinkRateMs, firmwareVersion, scanTimedOut, onboardingDone
        )
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        DashboardUiState(
            switchState         = values[0] as SwitchState,
            voltage             = values[1] as Float,
            isWarning           = values[2] as Boolean,
            logMessages         = values[3] as List<String>,
            connectionState     = values[4] as BleConnectionState,
            reconnectAttempt    = values[5] as Int,
            deviceName          = values[6] as String,
            isDarkTheme         = values[7] as Boolean,
            isDemoMode          = values[8] as Boolean,
            showRawHex          = values[9] as Boolean,
            showIndicatorLabels = values[10] as Boolean,
            usePlainLabels      = values[11] as Boolean,
            blinkRateMs         = values[12] as Long,
            firmwareVersion     = values[13] as String?,
            scanTimedOut        = values[14] as Boolean,
            onboardingDone      = values[15] as Boolean
        )
    }.stateInVm(DashboardUiState())

    init {
        viewModelScope.launch {
            var wasHornActive = false
            effectiveSwitchState.collect { state ->
                if (state.horn && !wasHornActive) soundManager.playHorn()
                else if (!state.horn && wasHornActive) soundManager.stopHorn()
                wasHornActive = state.horn
            }
        }
        viewModelScope.launch {
            connectionState.collect { state ->
                when (state) {
                    BleConnectionState.SCANNING     -> soundManager.playEngineLoop()
                    BleConnectionState.RECONNECTING -> soundManager.playEngineLoop()
                    BleConnectionState.CONNECTED    -> soundManager.stopEngineLoop()
                    BleConnectionState.DISCONNECTED -> soundManager.stopEngineLoop()
                }
            }
        }
    }

    // ---- Connection ----

    fun toggleBleConnection() {
        if (_isDemoMode.value) return
        when (connectionState.value) {
            BleConnectionState.DISCONNECTED  -> bleManager.scanAndConnect(bleDeviceName.value)
            BleConnectionState.SCANNING      -> bleManager.disconnect()
            BleConnectionState.RECONNECTING  -> bleManager.disconnect()
            BleConnectionState.CONNECTED     -> bleManager.disconnect()
        }
    }

    /** Called after the user explicitly confirms a disconnect (FIXED #10). */
    fun confirmDisconnect() = bleManager.disconnect()

    fun retryConnection() {
        if (_isDemoMode.value) return
        bleManager.scanAndConnect(bleDeviceName.value)
    }

    // ---- Settings (with Snackbar feedback — FIXED #28) ----

    fun updateDeviceName(name: String) {
        // FIXED #23: trim whitespace before persisting so we never store " RE_Switch_Dash ".
        val sanitised = name.trim()
        if (sanitised.isBlank()) return
        viewModelScope.launch {
            settings.setDeviceName(sanitised)
            _settingsToast.emit("Device name saved")
        }
    }

    fun setDarkTheme(dark: Boolean) {
        viewModelScope.launch {
            settings.setDarkTheme(dark)
            _settingsToast.emit(if (dark) "Dark theme enabled" else "Light theme enabled")
        }
    }

    fun setShowRawHex(show: Boolean) {
        viewModelScope.launch {
            settings.setShowRawHex(show)
            _settingsToast.emit(if (show) "Raw hex log enabled" else "Plain log enabled")
        }
    }

    fun setShowIndicatorLabels(show: Boolean) {
        viewModelScope.launch {
            settings.setShowIndicatorLabels(show)
            _settingsToast.emit(if (show) "Indicator labels on" else "Indicator labels off")
        }
    }

    fun setUsePlainLabels(plain: Boolean) {
        viewModelScope.launch {
            settings.setUsePlainLabels(plain)
            _settingsToast.emit(if (plain) "Plain language labels" else "Engineering labels")
        }
    }

    fun setBlinkRateMs(ms: Long) {
        viewModelScope.launch { settings.setBlinkRateMs(ms.toInt()) }
    }

    fun markOnboardingDone() {
        viewModelScope.launch { settings.setOnboardingDone(true) }
    }

    // ---- Demo mode (FIXED #26: also reachable from the dashboard FAB) ----

    fun toggleDemoMode() {
        if (_isDemoMode.value) stopDemoMode() else startDemoMode()
    }

    fun startDemoMode() {
        if (_isDemoMode.value) return
        // Demo mode supersedes a real connection — disconnect the radio first.
        if (connectionState.value != BleConnectionState.DISCONNECTED) bleManager.disconnect()
        _isDemoMode.value = true
        viewModelScope.launch {
            var idx = 0
            _demoLogMessages.value = listOf("Demo mode active.", "Cycling through all switch states...")
            while (_isDemoMode.value) {
                val (state, volts) = DEMO_STATES[idx % DEMO_STATES.size]
                _demoSwitchState.value = mapStateIntToSwitchState(state, volts)
                _demoVoltage.value = volts
                _demoLogMessages.update { list ->
                    (list + "DEMO: state=$state  adc=${"%.2f".format(volts)}V").takeLast(50)
                }
                idx++
                delay(1500)
            }
        }
        viewModelScope.launch { _settingsToast.emit("Demo mode started") }
    }

    fun stopDemoMode() {
        if (!_isDemoMode.value) return
        _isDemoMode.value = false
        _demoSwitchState.value = SwitchState()
        viewModelScope.launch { _settingsToast.emit("Demo mode stopped") }
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.disconnect()
        soundManager.release()
    }

    // ---- helper ----
    private fun <T> Flow<T>.stateInVm(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)
}
