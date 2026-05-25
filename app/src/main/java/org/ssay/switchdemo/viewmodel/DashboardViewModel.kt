package org.ssay.switchdemo.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.ssay.switchdemo.data.*

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("switch_demo_prefs", Context.MODE_PRIVATE)
    private val bleManager = BleManager(application)
    private val audioManager = AudioManager(application)

    val connectionState: StateFlow<BleConnectionState> = bleManager.connectionState
    val switchState: StateFlow<SwitchState> = bleManager.switchState
    val logMessages: StateFlow<List<String>> = bleManager.logMessages
    val firmwareVersion: StateFlow<String?> = bleManager.firmwareVersion
    val scanTimedOut: StateFlow<Boolean> = bleManager.scanTimedOut

    private val _bleDeviceName = MutableStateFlow(
        prefs.getString("ble_device_name", "RE_Switch_Dash") ?: "RE_Switch_Dash"
    )
    val bleDeviceName: StateFlow<String> = _bleDeviceName.asStateFlow()

    val voltage: StateFlow<Float> = switchState.map { it.adcVolts }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val isWarning: StateFlow<Boolean> = switchState.map { it.warn || it.horn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _blinkRateMs = MutableStateFlow(prefs.getInt("blink_rate_ms", 500).toLong())
    val blinkRateMs: StateFlow<Long> = _blinkRateMs.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _showRawHex = MutableStateFlow(prefs.getBoolean("show_raw_hex", false))
    val showRawHex: StateFlow<Boolean> = _showRawHex.asStateFlow()

    private val _showIndicatorLabels = MutableStateFlow(prefs.getBoolean("show_indicator_labels", true))
    val showIndicatorLabels: StateFlow<Boolean> = _showIndicatorLabels.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _demoSwitchState = MutableStateFlow(SwitchState())
    private val _demoVoltage = MutableStateFlow(0f)
    private val _demoLogMessages = MutableStateFlow(listOf("Demo mode active.", "Cycling through switch states..."))

    val effectiveSwitchState: StateFlow<SwitchState> = combine(
        _isDemoMode, switchState, _demoSwitchState
    ) { demo, real, demoState -> if (demo) demoState else real }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SwitchState())

    val effectiveVoltage: StateFlow<Float> = combine(
        _isDemoMode, voltage, _demoVoltage
    ) { demo, realV, demoV -> if (demo) demoV else realV }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val effectiveLogMessages: StateFlow<List<String>> = combine(
        _isDemoMode, logMessages, _demoLogMessages
    ) { demo, realLog, demoLog -> if (demo) demoLog else realLog }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("System ready."))

    val effectiveIsWarning: StateFlow<Boolean> = effectiveSwitchState.map { it.warn || it.horn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            var wasHornActive = false
            effectiveSwitchState.collect { state ->
                if (state.horn && !wasHornActive) audioManager.playHorn()
                else if (!state.horn && wasHornActive) audioManager.stopHorn()
                wasHornActive = state.horn
            }
        }
        viewModelScope.launch {
            connectionState.collect { state ->
                when (state) {
                    BleConnectionState.SCANNING     -> audioManager.playEngineLoop()
                    BleConnectionState.CONNECTED    -> audioManager.stopEngineLoop()
                    BleConnectionState.DISCONNECTED -> audioManager.stopEngineLoop()
                }
            }
        }
    }

    fun toggleBleConnection() {
        if (_isDemoMode.value) return
        when (connectionState.value) {
            BleConnectionState.DISCONNECTED -> bleManager.scanAndConnect(_bleDeviceName.value)
            BleConnectionState.SCANNING     -> bleManager.disconnect()
            BleConnectionState.CONNECTED    -> bleManager.disconnect()
        }
    }

    fun updateDeviceName(name: String) {
        _bleDeviceName.value = name
        prefs.edit().putString("ble_device_name", name).apply()
    }

    fun setDarkTheme(dark: Boolean) {
        _isDarkTheme.value = dark
        prefs.edit().putBoolean("is_dark_theme", dark).apply()
    }

    fun setShowRawHex(show: Boolean) {
        _showRawHex.value = show
        prefs.edit().putBoolean("show_raw_hex", show).apply()
    }

    fun setShowIndicatorLabels(show: Boolean) {
        _showIndicatorLabels.value = show
        prefs.edit().putBoolean("show_indicator_labels", show).apply()
    }

    fun setBlinkRateMs(ms: Long) {
        _blinkRateMs.value = ms
        prefs.edit().putInt("blink_rate_ms", ms.toInt()).apply()
    }

    fun startDemoMode() {
        if (_isDemoMode.value) return
        _isDemoMode.value = true
        viewModelScope.launch {
            var idx = 0
            _demoLogMessages.value = listOf("Demo mode active.", "Cycling through all switch states...")
            while (_isDemoMode.value) {
                val (state, volts) = DEMO_STATES[idx % DEMO_STATES.size]
                _demoSwitchState.value = mapStateIntToSwitchState(state, volts)
                _demoVoltage.value = volts
                _demoLogMessages.update { list ->
                    (list + "DEMO: state=$state  adc=${String.format("%.2f", volts)}V").takeLast(50)
                }
                idx++
                delay(1500)
            }
        }
    }

    fun stopDemoMode() {
        _isDemoMode.value = false
        _demoSwitchState.value = SwitchState()
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.disconnect()
        audioManager.release()
    }
}