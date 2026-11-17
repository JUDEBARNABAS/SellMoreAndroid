package com.example.hardware

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.model.KioskConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

data class PairedBluetoothSummary(
    val name: String,
    val address: String,
    val isSellMoreTarget: Boolean
)

class BluetoothSppManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSysLog: (String, Boolean) -> Unit
) {
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    private val _pairedDevices = MutableStateFlow<List<PairedBluetoothSummary>>(emptyList())
    val pairedDevices: StateFlow<List<PairedBluetoothSummary>> = _pairedDevices.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _rxLines = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val rxLines: SharedFlow<String> = _rxLines.asSharedFlow()

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var readJob: Job? = null
    private var connectJob: Job? = null

    fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        val btAdapter = adapter
        if (btAdapter == null || !hasRequiredPermissions()) {
            _pairedDevices.value = emptyList()
            return
        }
        val bonded = runCatching { btAdapter.bondedDevices.orEmpty() }.getOrDefault(emptySet())
        _pairedDevices.value = bonded.map { device ->
            val name = device.name ?: "Unnamed Device"
            PairedBluetoothSummary(
                name = name,
                address = device.address,
                isSellMoreTarget = name.equals(KioskConstants.BLUETOOTH_DEVICE_NAME, ignoreCase = true) ||
                    name.contains("HC-05", ignoreCase = true) ||
                    name.contains("HC-06", ignoreCase = true) ||
                    name.contains("SellMore", ignoreCase = true) ||
                    name.contains("ESP32", ignoreCase = true)
            )
        }.sortedByDescending { it.isSellMoreTarget }
    }

    @SuppressLint("MissingPermission")
    fun connectToTargetOrAddress(targetAddress: String? = null) {
        if (!hasRequiredPermissions()) {
            onSysLog("Bluetooth permission required before connecting to ${KioskConstants.BLUETOOTH_DEVICE_NAME} / HC-05.", true)
            return
        }
        val btAdapter = adapter
        if (btAdapter == null || !btAdapter.isEnabled) {
            onSysLog("Bluetooth adapter is disabled or unavailable on this device.", true)
            return
        }
        refreshPairedDevices()
        val bonded = runCatching { btAdapter.bondedDevices.orEmpty() }.getOrDefault(emptySet())
        val device: BluetoothDevice? = when {
            targetAddress != null -> bonded.firstOrNull { it.address == targetAddress }
            else -> bonded.firstOrNull {
                it.name?.equals(KioskConstants.BLUETOOTH_DEVICE_NAME, ignoreCase = true) == true
            } ?: bonded.firstOrNull {
                val n = it.name.orEmpty()
                n.contains("HC-05", ignoreCase = true) ||
                    n.contains("HC-06", ignoreCase = true) ||
                    n.contains("SellMore", ignoreCase = true) ||
                    n.contains("ESP32", ignoreCase = true)
            }
        }
        if (device == null) {
            onSysLog("Paired device ('HC-05', 'HC-06', or '${KioskConstants.BLUETOOTH_DEVICE_NAME}') not found. Pair in Android Bluetooth settings first.", true)
            return
        }

        connectJob?.cancel()
        connectJob = scope.launch(Dispatchers.IO) {
            try {
                disconnectInternal()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                ) {
                    runCatching { btAdapter.cancelDiscovery() }
                }
                val sppUuid = UUID.fromString(KioskConstants.SPP_UUID_STRING)
                onSysLog("Opening RFCOMM SPP socket to ${device.name ?: device.address}...", false)
                val rfcommSocket = device.createRfcommSocketToServiceRecord(sppUuid)
                rfcommSocket.connect()
                socket = rfcommSocket
                outputStream = rfcommSocket.outputStream
                _isConnected.value = true
                _connectedDeviceName.value = "${device.name ?: "ESP32"} (SPP)"
                onSysLog("Bluetooth SPP connected: ${device.name ?: device.address}", false)
                startReadLoop(rfcommSocket)
                sendCommand("PING")
            } catch (e: Exception) {
                onSysLog("Bluetooth SPP connection failed: ${e.message}", true)
                disconnectInternal()
            }
        }
    }

    private fun startReadLoop(rfcommSocket: BluetoothSocket) {
        readJob?.cancel()
        readJob = scope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(rfcommSocket.inputStream, Charsets.UTF_8))
                while (isActive && _isConnected.value) {
                    val line = reader.readLine() ?: break
                    val cleaned = line.trim()
                    if (cleaned.isNotEmpty()) {
                        _rxLines.emit(cleaned)
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    onSysLog("Bluetooth SPP link dropped: ${e.message}", true)
                }
            } finally {
                disconnectInternal()
            }
        }
    }

    fun sendCommand(command: String): Boolean {
        val stream = outputStream ?: return false
        return try {
            val bytes = (command.trimEnd() + "\n").toByteArray(Charsets.UTF_8)
            stream.write(bytes)
            stream.flush()
            true
        } catch (e: Exception) {
            onSysLog("Bluetooth SPP TX failed: ${e.message}", true)
            false
        }
    }

    fun disconnect() {
        connectJob?.cancel()
        disconnectInternal()
    }

    private fun disconnectInternal() {
        readJob?.cancel()
        readJob = null
        runCatching { outputStream?.close() }
        runCatching { socket?.close() }
        outputStream = null
        socket = null
        _isConnected.value = false
        _connectedDeviceName.value = null
    }
}
