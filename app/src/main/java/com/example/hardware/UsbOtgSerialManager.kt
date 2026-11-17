package com.example.hardware

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.model.KioskConstants
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.Ch34xSerialDriver
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver
import com.hoho.android.usbserial.driver.FtdiSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
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

data class UsbDeviceSummary(
    val deviceId: Int,
    val deviceName: String,
    val productName: String,
    val vendorIdHex: String,
    val productIdHex: String,
    val driverLabel: String,
    val hasPermission: Boolean
)

class UsbOtgSerialManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSysLog: (String, Boolean) -> Unit
) {
    companion object {
        private const val ACTION_USB_PERMISSION = "com.aistudio.sellmore.kiosk.USB_PERMISSION"
    }

    private val usbManager: UsbManager? =
        context.getSystemService(Context.USB_SERVICE) as? UsbManager

    private val customProber: UsbSerialProber by lazy {
        val table = UsbSerialProber.getDefaultProbeTable()
        // Arduino Uno R3 / R4 / Mega (0x2341)
        table.addProduct(0x2341, 0x0043, CdcAcmSerialDriver::class.java)
        table.addProduct(0x2341, 0x0001, CdcAcmSerialDriver::class.java)
        table.addProduct(0x2341, 0x1002, CdcAcmSerialDriver::class.java)
        // Espressif native USB-JTAG/Serial & CDC-ACM (0x303A)
        table.addProduct(0x303A, 0x1001, CdcAcmSerialDriver::class.java)
        table.addProduct(0x303A, 0x0002, CdcAcmSerialDriver::class.java)
        // Silicon Labs CP210x (0x10C4)
        table.addProduct(0x10C4, 0xEA60, Cp21xxSerialDriver::class.java)
        table.addProduct(0x10C4, 0xEA70, Cp21xxSerialDriver::class.java)
        // WCH CH340/CH341 (0x1A86)
        table.addProduct(0x1A86, 0x7523, Ch34xSerialDriver::class.java)
        table.addProduct(0x1A86, 0x55D4, CdcAcmSerialDriver::class.java)
        // FTDI (0x0403)
        table.addProduct(0x0403, 0x6001, FtdiSerialDriver::class.java)
        table.addProduct(0x0403, 0x6015, FtdiSerialDriver::class.java)
        UsbSerialProber(table)
    }

    private val _availableDevices = MutableStateFlow<List<UsbDeviceSummary>>(emptyList())
    val availableDevices: StateFlow<List<UsbDeviceSummary>> = _availableDevices.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _rxLines = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val rxLines: SharedFlow<String> = _rxLines.asSharedFlow()

    private var activePort: UsbSerialPort? = null
    private var readJob: Job? = null
    private var pendingDeviceIdForConnect: Int? = null
    private var receiverRegistered = false

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_USB_PERMISSION -> {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    refreshDevices()
                    if (granted && device != null) {
                        onSysLog("USB-OTG permission granted for ${device.productName ?: device.deviceName}", false)
                        openDevice(device)
                    } else {
                        onSysLog("USB-OTG permission denied by user.", true)
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    refreshDevices()
                    onSysLog("USB-OTG device attached. Ready to connect at 115200 baud.", false)
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    if (device != null && activePort?.driver?.device?.deviceId == device.deviceId) {
                        disconnect()
                        onSysLog("USB-OTG ESP32 detached.", true)
                    }
                    refreshDevices()
                }
            }
        }
    }

    fun register() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        ContextCompat.registerReceiver(
            context,
            usbReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        receiverRegistered = true
        refreshDevices()
    }

    fun unregister() {
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(usbReceiver) }
            receiverRegistered = false
        }
        disconnect()
    }

    fun refreshDevices() {
        val manager = usbManager ?: return
        val drivers = findAllDrivers(manager)
        val summaries = drivers.map { driver ->
            val d = driver.device
            UsbDeviceSummary(
                deviceId = d.deviceId,
                deviceName = d.deviceName,
                productName = d.productName ?: "ESP32 USB-UART",
                vendorIdHex = "0x%04X".format(d.vendorId),
                productIdHex = "0x%04X".format(d.productId),
                driverLabel = driver.javaClass.simpleName.removeSuffix("SerialDriver"),
                hasPermission = manager.hasPermission(d)
            )
        }
        _availableDevices.value = summaries
    }

    private fun findAllDrivers(manager: UsbManager): List<UsbSerialDriver> {
        val defaultDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        val customDrivers = customProber.findAllDrivers(manager)
        val byId = LinkedHashMap<Int, UsbSerialDriver>()
        (defaultDrivers + customDrivers).forEach { drv ->
            byId[drv.device.deviceId] = drv
        }
        return byId.values.toList()
    }

    fun connectFirstOrDevice(deviceId: Int? = null) {
        val manager = usbManager
        if (manager == null) {
            onSysLog("UsbManager unavailable on this Android device.", true)
            return
        }
        refreshDevices()
        val drivers = findAllDrivers(manager)
        if (drivers.isEmpty()) {
            onSysLog("No ESP32 USB-OTG UART device detected (CP210x / CH340 / FTDI / ESP32-S3).", true)
            return
        }
        val targetDriver = if (deviceId != null) {
            drivers.firstOrNull { it.device.deviceId == deviceId } ?: drivers.first()
        } else {
            drivers.first()
        }
        val device = targetDriver.device
        if (manager.hasPermission(device)) {
            openDevice(device)
        } else {
            pendingDeviceIdForConnect = device.deviceId
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val permIntent = Intent(ACTION_USB_PERMISSION).setPackage(context.packageName)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, permIntent, flags)
            onSysLog("Requesting Android USB-OTG permission for ${device.productName ?: device.deviceName}...", false)
            manager.requestPermission(device, pendingIntent)
        }
    }

    private fun openDevice(device: UsbDevice) {
        val manager = usbManager ?: return
        val driver = findAllDrivers(manager).firstOrNull { it.device.deviceId == device.deviceId }
        if (driver == null) {
            onSysLog("No compatible serial driver found for ${device.deviceName}.", true)
            return
        }
        val connection = manager.openDevice(device)
        if (connection == null) {
            onSysLog("Failed to open USB device connection.", true)
            return
        }
        try {
            disconnect()
            val port = driver.ports.firstOrNull() ?: throw IllegalStateException("No USB serial ports")
            port.open(connection)
            port.setParameters(
                KioskConstants.USB_BAUD_RATE,
                8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )
            runCatching {
                port.dtr = true
                port.rts = true
            }
            activePort = port
            _isConnected.value = true
            val label = "${device.productName ?: "ESP32"} (115200 8N1)"
            _connectedDeviceName.value = label
            onSysLog("USB-OTG Serial connected: $label", false)
            startReaderLoop(port)
            sendCommand("PING")
        } catch (e: Exception) {
            onSysLog("USB-OTG open error: ${e.message}", true)
            disconnect()
        }
    }

    private fun startReaderLoop(port: UsbSerialPort) {
        readJob?.cancel()
        readJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            val lineBuffer = StringBuilder()
            while (isActive && _isConnected.value) {
                try {
                    val len = port.read(buffer, 200)
                    if (len > 0) {
                        val chunk = String(buffer, 0, len, Charsets.UTF_8)
                        for (ch in chunk) {
                            if (ch == '\n') {
                                val line = lineBuffer.toString().trim('\r', ' ')
                                lineBuffer.clear()
                                if (line.isNotEmpty()) {
                                    _rxLines.emit(line)
                                }
                            } else if (ch != '\r') {
                                lineBuffer.append(ch)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        onSysLog("USB-OTG read stream closed: ${e.message}", true)
                    }
                    break
                }
            }
        }
    }

    fun sendCommand(command: String): Boolean {
        val port = activePort ?: return false
        return try {
            val payload = (command.trimEnd() + "\n").toByteArray(Charsets.UTF_8)
            port.write(payload, 500)
            true
        } catch (e: Exception) {
            onSysLog("USB-OTG TX failed: ${e.message}", true)
            false
        }
    }

    fun disconnect() {
        readJob?.cancel()
        readJob = null
        runCatching { activePort?.close() }
        activePort = null
        _isConnected.value = false
        _connectedDeviceName.value = null
    }
}
