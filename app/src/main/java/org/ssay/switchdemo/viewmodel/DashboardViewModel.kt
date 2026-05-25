package org.ssay.switchdemo.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.ssay.switchdemo.data.*

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("switch_demo_prefs", Context.MODE_PRIVATE)

    private val bleManager = BleManager(application)
    private val audioManager = AudioManager(application)

    // --- Exposed UI State ---
    val connectionState: StateFlow<BleConnectionState> = bleManager.connectionState
    val switchState: StateFlow<SwitchState> = bleManager.switchState
    val logMessages: StateFlow<List<String>> = bleManager.logMessages

    private val _bleDeviceName = MutableStateFlow(
        prefs.getString("ble_device_name", "RE_Switch_Dash") ?: "RE_Switch_Dash"
    )
    val bleDeviceName: StateFlow<String> = _bleDeviceName.asStateFlow()

    // Derived states for UI convenience
    val voltage: StateFlow<Float> = switchState.map { it.adcVolts }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val isWarning: StateFlow<Boolean> = switchState.map { it.warn || it.horn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        // Observe switch state changes for audio
        viewModelScope.launch {
            var wasHornActive = false
            var wasConnected = false

            switchState.collect { state ->
                // Horn audio
                if (state.horn && !wasHornActive) {
                    audioManager.playHorn()
                } else if (!state.horn && wasHornActive) {
                    audioManager.stopHorn()
                }
                wasHornActive = state.horn
            }
        }

        // Observe connection state for engine sound
        viewModelScope.launch {
            connectionState.collect { state ->
                when (state) {
                    BleConnectionState.SCANNING -> audioManager.playEngineLoop()
                    BleConnectionState.CONNECTED -> audioManager.stopEngineLoop()
                    BleConnectionState.DISCONNECTED -> audioManager.stopEngineLoop()
                }
            }
        }
    }

    fun toggleBleConnection() {
        when (connectionState.value) {
            BleConnectionState.DISCONNECTED -> {
                bleManager.scanAndConnect(_bleDeviceName.value)
            }
            BleConnectionState.SCANNING -> {
                // Cancel scanning
                bleManager.disconnect()
            }
            BleConnectionState.CONNECTED -> {
                bleManager.disconnect()
            }
        }
    }

    fun updateDeviceName(name: String) {
        _bleDeviceName.value = name
        prefs.edit().putString("ble_device_name", name).apply()
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.disconnect()
        audioManager.release()
    }
}
