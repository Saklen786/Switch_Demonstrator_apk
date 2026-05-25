package org.ssay.switchdemo.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

/**
 * FIXED #38: scope is now injected from the ViewModel (viewModelScope) instead of
 * being created internally. The scope's lifetime now matches the ViewModel's.
 *
 * FIXED #40: removed the class-wide @SuppressLint("MissingPermission"). Each
 * function that calls a permission-gated BLE API is annotated individually,
 * and a runtime permission check is performed up front.
 *
 * FIXED #41: state mutations are kept simple — the synchronized buffer block
 * does only buffer manipulation; StateFlow updates happen outside the lock.
 *
 * FIXED #5: emits a [Event.BluetoothDisabled] when the adapter is off, so the
 * UI can surface a dialog with a "Turn on Bluetooth" button.
 *
 * FIXED #6: emits [Event.PermissionDenied] when the runtime permissions are
 * missing, so the UI can prompt the user.
 *
 * FIXED #8: emits a RECONNECTING connection state with attempt counters.
 */
class BleManager(
    private val context: Context,
    private val scope: CoroutineScope
) {

    companion object {
        val NUS_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val NUS_TX_CHAR_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        val CCCD_UUID: UUID        = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private val NOISY_TRANSITIONS = listOf(
            setOf(10, 11), setOf(8, 9), setOf(12, 3),
            setOf(13, 3), setOf(14, 3), setOf(15, 3)
        )

        const val SCAN_TIMEOUT_MS        = 10_000L
        const val AUTO_RECONNECT_DELAY_MS = 3_000L
        const val MAX_RECONNECT_ATTEMPTS  = 5
    }

    /** One-shot UI events the ViewModel/Composables react to. */
    sealed class Event {
        data object BluetoothDisabled : Event()
        data object PermissionDenied  : Event()
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner

    private var gatt: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null

    private var pendingStateInt = 6
    private var currentStateInt = 6
    private var debounceJob: Job? = null

    @Volatile private var reconnectAttempts = 0
    @Volatile private var lastDeviceName: String = ""
    @Volatile private var userRequestedDisconnect = false
    private var reconnectJob: Job? = null

    private val receiveBuffer = StringBuilder()

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _switchState = MutableStateFlow(SwitchState())
    val switchState: StateFlow<SwitchState> = _switchState.asStateFlow()

    private val _logMessages = MutableStateFlow(listOf("System ready.", "Awaiting connection..."))
    val logMessages: StateFlow<List<String>> = _logMessages.asStateFlow()

    private val _firmwareVersion = MutableStateFlow<String?>(null)
    val firmwareVersion: StateFlow<String?> = _firmwareVersion.asStateFlow()

    private val _scanTimedOut = MutableStateFlow(false)
    val scanTimedOut: StateFlow<Boolean> = _scanTimedOut.asStateFlow()

    private val _reconnectAttempt = MutableStateFlow(0)
    /** 0 when not reconnecting; otherwise 1..MAX_RECONNECT_ATTEMPTS. */
    val reconnectAttempt: StateFlow<Int> = _reconnectAttempt.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private fun log(msg: String) {
        _logMessages.update { list -> (list + msg).takeLast(50) }
    }

    private fun hasBlePermissions(): Boolean {
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return needed.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")  // FIXED #40: scoped to the callback that actually needs it
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    log("Device connected. Negotiating MTU...")
                    reconnectAttempts = 0
                    _reconnectAttempt.value = 0
                    _scanTimedOut.value = false
                    gatt.requestMtu(512)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    log("Device disconnected.")
                    _switchState.value = mapStateIntToSwitchState(6)
                    currentStateInt = 6
                    pendingStateInt = 6
                    synchronized(receiveBuffer) { receiveBuffer.clear() }
                    gatt.close()
                    this@BleManager.gatt = null

                    val canRetry = !userRequestedDisconnect &&
                            reconnectAttempts < MAX_RECONNECT_ATTEMPTS &&
                            lastDeviceName.isNotBlank()
                    if (canRetry) {
                        scheduleReconnect()
                    } else {
                        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                            log("Auto-reconnect: max attempts ($MAX_RECONNECT_ATTEMPTS) reached.")
                        }
                        _connectionState.value = BleConnectionState.DISCONNECTED
                        _reconnectAttempt.value = 0
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val payload = mtu - 3
            if (status == BluetoothGatt.GATT_SUCCESS) log("MTU negotiated: $mtu bytes ($payload byte payload).")
            else log("MTU negotiation failed — using default 20-byte payload.")
            gatt.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) { log("Service discovery failed (status $status)."); return }
            val service = gatt.getService(NUS_SERVICE_UUID) ?: run { log("UART service not found on device."); return }
            val txChar  = service.getCharacteristic(NUS_TX_CHAR_UUID) ?: run { log("TX characteristic not found."); return }
            if (!gatt.setCharacteristicNotification(txChar, true)) { log("setCharacteristicNotification failed."); return }
            val descriptor = txChar.getDescriptor(CCCD_UUID) ?: run { log("CCCD descriptor not found."); return }
            val ok: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
            log(if (ok) "Enabling notifications..." else "Descriptor write failed — check peripheral.")
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (descriptor.uuid != CCCD_UUID) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BleConnectionState.CONNECTED
                _reconnectAttempt.value = 0
                log("Notifications enabled. Receiving data...")
            } else {
                log("Failed to enable notifications (GATT status $status).")
                _connectionState.value = BleConnectionState.DISCONNECTED
                gatt.disconnect()
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            if (characteristic.uuid == NUS_TX_CHAR_UUID) handleReceivedData(value)
        }

        @Deprecated("Deprecated in API 33")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                characteristic.uuid == NUS_TX_CHAR_UUID) handleReceivedData(characteristic.value)
        }
    }

    private fun handleReceivedData(data: ByteArray) {
        // FIXED #41: extract the JSON inside the lock; emit StateFlow updates outside it.
        val jsonStr: String? = synchronized(receiveBuffer) {
            receiveBuffer.append(String(data, Charsets.UTF_8))
            val buf = receiveBuffer.toString()
            val start = buf.indexOf('{')
            val end   = buf.lastIndexOf('}')
            if (start < 0 || end <= start) return@synchronized null
            val str = buf.substring(start, end + 1)
            receiveBuffer.clear()
            if (end + 1 < buf.length) receiveBuffer.append(buf.substring(end + 1))
            str
        } ?: return

        try {
            val json     = JSONObject(jsonStr)
            val stateInt = json.optInt("state", 16)
            val volts    = json.optDouble("adc_volts", 0.0).toFloat()
            if (json.has("fw") && _firmwareVersion.value == null) {
                // Kotlin 2.x's K2 compiler treats JSONObject.optString(name) as
                // returning a nullable platform type (String?). Pin it explicitly
                // and use a safe call so the code is correct under either inference.
                val fw: String? = json.optString("fw")
                _firmwareVersion.value = fw?.takeIf { it.isNotBlank() }
            }
            processDebounced(stateInt, volts, jsonStr.trim())
        } catch (_: Exception) { /* malformed — wait for next chunk */ }
    }

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

    private fun scheduleReconnect() {
        reconnectAttempts++
        _reconnectAttempt.value = reconnectAttempts
        _connectionState.value = BleConnectionState.RECONNECTING   // FIXED #8
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            log("Auto-reconnect attempt $reconnectAttempts of $MAX_RECONNECT_ATTEMPTS in ${AUTO_RECONNECT_DELAY_MS / 1000}s...")
            delay(AUTO_RECONNECT_DELAY_MS)
            if (!userRequestedDisconnect && _connectionState.value == BleConnectionState.RECONNECTING) {
                scanAndConnect(lastDeviceName)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun scanAndConnect(deviceName: String) {
        if (_connectionState.value == BleConnectionState.SCANNING ||
            _connectionState.value == BleConnectionState.CONNECTED) return
        val adapter = bluetoothAdapter ?: run { log("Bluetooth not available."); return }
        if (!adapter.isEnabled) {
            log("Bluetooth is disabled.")
            _connectionState.value = BleConnectionState.DISCONNECTED
            scope.launch { _events.emit(Event.BluetoothDisabled) }   // FIXED #5
            return
        }
        if (!hasBlePermissions()) {
            log("Bluetooth permissions not granted.")
            _connectionState.value = BleConnectionState.DISCONNECTED
            scope.launch { _events.emit(Event.PermissionDenied) }    // FIXED #6
            return
        }
        lastDeviceName = deviceName
        userRequestedDisconnect = false
        _connectionState.value = BleConnectionState.SCANNING
        _scanTimedOut.value = false
        log("Scanning for \"$deviceName\"...")

        val callback = object : ScanCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.device.name ?: return
                if (name.equals(deviceName, ignoreCase = true)) {
                    stopScan()
                    log("Found: ${result.device.address}")
                    gatt = result.device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                }
            }
            override fun onScanFailed(errorCode: Int) {
                log("Scan failed (error $errorCode).")
                _connectionState.value = BleConnectionState.DISCONNECTED
                _scanTimedOut.value = false
                _reconnectAttempt.value = 0
            }
        }
        scanCallback = callback
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner?.startScan(emptyList(), settings, callback)

        scope.launch {
            delay(SCAN_TIMEOUT_MS)
            if (_connectionState.value == BleConnectionState.SCANNING) {
                stopScan()
                _scanTimedOut.value = true
                log("Device not found. Scan timed out.")
                if (!userRequestedDisconnect && reconnectAttempts < MAX_RECONNECT_ATTEMPTS &&
                    lastDeviceName.isNotBlank()) {
                    scheduleReconnect()
                } else {
                    _connectionState.value = BleConnectionState.DISCONNECTED
                    _reconnectAttempt.value = 0
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        scanCallback?.let { scanner?.stopScan(it) }
        scanCallback = null
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        userRequestedDisconnect = true
        reconnectJob?.cancel()
        reconnectAttempts = 0
        _reconnectAttempt.value = 0
        stopScan()
        gatt?.disconnect()
        if (_connectionState.value == BleConnectionState.SCANNING ||
            _connectionState.value == BleConnectionState.RECONNECTING) {
            _connectionState.value = BleConnectionState.DISCONNECTED
            log("Cancelled.")
        }
    }
}
