package org.ssay.switchdemo.data

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.util.UUID

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    companion object {
        val NUS_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val NUS_TX_CHAR_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private val NOISY_TRANSITIONS = listOf(
            setOf(10, 11), setOf(8, 9), setOf(12, 3),
            setOf(13, 3), setOf(14, 3), setOf(15, 3)
        )
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner

    private var gatt: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Debounce state
    private var pendingStateInt = 6
    private var currentStateInt = 6
    private var debounceJob: Job? = null

    // --- Exposed flows ---
    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _switchState = MutableStateFlow(SwitchState())
    val switchState: StateFlow<SwitchState> = _switchState.asStateFlow()

    private val _logMessages = MutableStateFlow(listOf("System ready.", "Awaiting connection..."))
    val logMessages: StateFlow<List<String>> = _logMessages.asStateFlow()

    private fun log(msg: String) {
        _logMessages.update { list ->
            (list + msg).takeLast(50)
        }
    }

    // --- GATT Callback ---
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    log("Device connected.")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    log("Device disconnected.")
                    _connectionState.value = BleConnectionState.DISCONNECTED
                    _switchState.value = mapStateIntToSwitchState(6)
                    currentStateInt = 6
                    pendingStateInt = 6
                    gatt.close()
                    this@BleManager.gatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(NUS_SERVICE_UUID)
                if (service != null) {
                    val txChar = service.getCharacteristic(NUS_TX_CHAR_UUID)
                    if (txChar != null) {
                        gatt.setCharacteristicNotification(txChar, true)
                        val descriptor = txChar.getDescriptor(CCCD_UUID)
                        if (descriptor != null) {
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                        }
                        _connectionState.value = BleConnectionState.CONNECTED
                        log("Notifications enabled. Receiving data...")
                    } else {
                        log("TX characteristic not found.")
                    }
                } else {
                    log("UART service not found on device.")
                }
            }
        }

        // API 33+ signature
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == NUS_TX_CHAR_UUID) {
                handleReceivedData(value)
            }
        }

        // Legacy signature for API < 33
        @Deprecated("Deprecated in API 33")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                if (characteristic.uuid == NUS_TX_CHAR_UUID) {
                    handleReceivedData(characteristic.value)
                }
            }
        }
    }

    // --- Data handling with debounce ---
    private fun handleReceivedData(data: ByteArray) {
        try {
            val decoded = String(data, Charsets.UTF_8).trim()
            if (decoded.isEmpty()) return

            val json = JSONObject(decoded)
            val stateInt = json.optInt("state", 16)
            val volts = json.optDouble("adc_volts", 0.0).toFloat()

            processDebounced(stateInt, volts, decoded)
        } catch (_: Exception) {
            // Silently ignore malformed data
        }
    }

    private fun processDebounced(stateInt: Int, volts: Float, raw: String) {
        // Always update voltage immediately
        _switchState.update { it.copy(adcVolts = volts) }

        if (stateInt != pendingStateInt) {
            pendingStateInt = stateInt
            debounceJob?.cancel()

            val delayMs = if (setOf(currentStateInt, stateInt) in NOISY_TRANSITIONS) 250L else 30L

            debounceJob = scope.launch {
                delay(delayMs)
                commitState(stateInt, volts, raw)
            }
        } else if (debounceJob == null || debounceJob?.isCompleted == true) {
            commitState(stateInt, volts, raw)
        }
    }

    private fun commitState(stateInt: Int, volts: Float, raw: String) {
        debounceJob = null
        if (currentStateInt != stateInt) {
            log("RX: $raw")
        }
        currentStateInt = stateInt
        _switchState.value = mapStateIntToSwitchState(stateInt, volts)
    }

    // --- Scan & Connect ---
    fun scanAndConnect(deviceName: String) {
        if (_connectionState.value != BleConnectionState.DISCONNECTED) return

        val adapter = bluetoothAdapter ?: run {
            log("Bluetooth not available.")
            return
        }
        if (!adapter.isEnabled) {
            log("Bluetooth is disabled.")
            return
        }

        _connectionState.value = BleConnectionState.SCANNING
        log("Scanning for \"$deviceName\"...")

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.device.name ?: return
                if (name == deviceName) {
                    stopScan()
                    log("Found: ${result.device.address}")
                    gatt = result.device.connectGatt(
                        context, false, gattCallback, BluetoothDevice.TRANSPORT_LE
                    )
                }
            }

            override fun onScanFailed(errorCode: Int) {
                log("Scan failed (error $errorCode).")
                _connectionState.value = BleConnectionState.DISCONNECTED
            }
        }
        scanCallback = callback

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner?.startScan(emptyList(), settings, callback)

        // Auto-stop scan after 10 seconds
        scope.launch {
            delay(10_000)
            if (_connectionState.value == BleConnectionState.SCANNING) {
                stopScan()
                log("Device not found. Scan timed out.")
                _connectionState.value = BleConnectionState.DISCONNECTED
            }
        }
    }

    private fun stopScan() {
        scanCallback?.let { scanner?.stopScan(it) }
        scanCallback = null
    }

    fun disconnect() {
        stopScan()
        gatt?.disconnect()
        if (_connectionState.value == BleConnectionState.SCANNING) {
            _connectionState.value = BleConnectionState.DISCONNECTED
            log("Scan cancelled.")
        }
    }
}
