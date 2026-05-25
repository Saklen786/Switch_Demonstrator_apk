package org.ssay.switchdemo.data

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
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

    // -----------------------------------------------------------------------
    // Reassembly buffer for fragmented BLE packets.
    // Default MTU = 23 bytes → 20 bytes payload.  A typical JSON payload such
    // as {"state":0,"adc_volts":12.34}  is ~33 chars and gets split across two
    // packets.  We buffer until we have a balanced { … } before parsing.
    // -----------------------------------------------------------------------
    private val receiveBuffer = StringBuilder()

    // --- Exposed flows ---
    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _switchState = MutableStateFlow(SwitchState())
    val switchState: StateFlow<SwitchState> = _switchState.asStateFlow()

    private val _logMessages = MutableStateFlow(listOf("System ready.", "Awaiting connection..."))
    val logMessages: StateFlow<List<String>> = _logMessages.asStateFlow()

    private fun log(msg: String) {
        _logMessages.update { list -> (list + msg).takeLast(50) }
    }

    // --- GATT Callback ---
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    log("Device connected. Negotiating MTU...")
                    // Request a larger MTU so long JSON payloads fit in one packet.
                    // discoverServices() is called inside onMtuChanged after the
                    // negotiation completes (or immediately on failure).
                    gatt.requestMtu(512)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    log("Device disconnected.")
                    _connectionState.value = BleConnectionState.DISCONNECTED
                    _switchState.value = mapStateIntToSwitchState(6)
                    currentStateInt = 6
                    pendingStateInt = 6
                    synchronized(receiveBuffer) { receiveBuffer.clear() }
                    gatt.close()
                    this@BleManager.gatt = null
                }
            }
        }

        // Called after requestMtu(). Proceed to service discovery once we know
        // the final MTU (whether the full 512 was granted or a smaller value).
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val payload = mtu - 3  // ATT overhead = 3 bytes
            if (status == BluetoothGatt.GATT_SUCCESS) {
                log("MTU negotiated: $mtu bytes ($payload byte payload).")
            } else {
                log("MTU negotiation failed — using default 20-byte payload.")
            }
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                log("Service discovery failed (status $status).")
                return
            }

            val service = gatt.getService(NUS_SERVICE_UUID)
            if (service == null) {
                log("UART service not found on device.")
                return
            }

            val txChar = service.getCharacteristic(NUS_TX_CHAR_UUID)
            if (txChar == null) {
                log("TX characteristic not found.")
                return
            }

            // Step 1: register local (Android-side) notification interest
            if (!gatt.setCharacteristicNotification(txChar, true)) {
                log("setCharacteristicNotification failed.")
                return
            }

            // Step 2: write CCCD so the peripheral actually sends notifications
            val descriptor = txChar.getDescriptor(CCCD_UUID)
            if (descriptor == null) {
                log("CCCD descriptor not found.")
                return
            }

            val writeResult: Boolean =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // API 33+ — non-deprecated overload
                    gatt.writeDescriptor(
                        descriptor,
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    ) == BluetoothStatusCodes.SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }

            if (writeResult) {
                log("Enabling notifications...")
            } else {
                log("Descriptor write failed — check peripheral.")
            }
            // CONNECTED state is confirmed inside onDescriptorWrite below
        }

        // Fires after CCCD write completes — this is the earliest safe point to
        // set CONNECTED and start receiving characteristic notifications.
        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (descriptor.uuid == CCCD_UUID) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    _connectionState.value = BleConnectionState.CONNECTED
                    log("Notifications enabled. Receiving data...")
                } else {
                    log("Failed to enable notifications (GATT status $status).")
                    _connectionState.value = BleConnectionState.DISCONNECTED
                    gatt.disconnect()
                }
            }
        }

        // API 33+ notification callback
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == NUS_TX_CHAR_UUID) handleReceivedData(value)
        }

        // Legacy notification callback for API < 33
        @Deprecated("Deprecated in API 33")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                if (characteristic.uuid == NUS_TX_CHAR_UUID) handleReceivedData(characteristic.value)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Data handling — reassemble fragmented packets into complete JSON objects
    // -----------------------------------------------------------------------
    private fun handleReceivedData(data: ByteArray) {
        synchronized(receiveBuffer) {
            receiveBuffer.append(String(data, Charsets.UTF_8))

            val buf = receiveBuffer.toString()

            // Find first '{' and last '}' — only parse when we have a balanced object
            val start = buf.indexOf('{')
            val end   = buf.lastIndexOf('}')

            if (start < 0 || end <= start) return // incomplete — wait for next packet

            val jsonStr = buf.substring(start, end + 1)

            // Keep any trailing bytes (next packet started) in the buffer
            receiveBuffer.clear()
            if (end + 1 < buf.length) receiveBuffer.append(buf.substring(end + 1))

            try {
                val json     = JSONObject(jsonStr)
                val stateInt = json.optInt("state", 16)
                val volts    = json.optDouble("adc_volts", 0.0).toFloat()
                processDebounced(stateInt, volts, jsonStr.trim())
            } catch (_: Exception) {
                // Malformed JSON — discard silently
            }
        }
    }

    // --- Debounce ---
    private fun processDebounced(stateInt: Int, volts: Float, raw: String) {
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
        if (currentStateInt != stateInt) log("RX: $raw")
        currentStateInt = stateInt
        _switchState.value = mapStateIntToSwitchState(stateInt, volts)
    }

    // --- Scan & Connect ---
    fun scanAndConnect(deviceName: String) {
        if (_connectionState.value != BleConnectionState.DISCONNECTED) return

        val adapter = bluetoothAdapter ?: run { log("Bluetooth not available."); return }
        if (!adapter.isEnabled) { log("Bluetooth is disabled."); return }

        _connectionState.value = BleConnectionState.SCANNING
        log("Scanning for \"$deviceName\"...")

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.device.name ?: return
                // Case-insensitive match — device may advertise "RE_SWITCH_DASH"
                // while the user typed "RE_Switch_Dash"
                if (name.equals(deviceName, ignoreCase = true)) {
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