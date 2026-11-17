package com.example.hardware

import android.content.Context
import com.example.model.DispenseStep
import com.example.model.DispenseUiState
import com.example.model.Esp32WifiStatus
import com.example.model.HardwareTransport
import com.example.model.LogDirection
import com.example.model.SerialLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class HardwareController(
    context: Context,
    private val scope: CoroutineScope,
    private val onHardwareAudit: (String, String, String) -> Unit
) {
    companion object {
        const val STOP_SPEED = 90
        const val DISPENSE_SPEED = 180
        const val DISPENSE_DURATION_MS = 2000L
    }

    private val logIdSeq = AtomicLong(1L)
    private val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _serialLogs = MutableStateFlow<List<SerialLogEntry>>(emptyList())
    val serialLogs: StateFlow<List<SerialLogEntry>> = _serialLogs.asStateFlow()

    // Represents Continuous Rotation Servo speed/value (0 = Full Rev, 90 = STOP, 180 = Full Fwd)
    private val _servoAngle = MutableStateFlow(STOP_SPEED)
    val servoAngle: StateFlow<Int> = _servoAngle.asStateFlow()

    private val _dispenseCycles = MutableStateFlow(0)
    val dispenseCycles: StateFlow<Int> = _dispenseCycles.asStateFlow()

    private val _wifiStatus = MutableStateFlow(Esp32WifiStatus(servoAngle = STOP_SPEED, servoHomeAngle = STOP_SPEED, servoActionAngle = DISPENSE_SPEED))
    val wifiStatus: StateFlow<Esp32WifiStatus> = _wifiStatus.asStateFlow()

    private val _wifiPollingActive = MutableStateFlow(false)
    val wifiPollingActive: StateFlow<Boolean> = _wifiPollingActive.asStateFlow()

    private val _dispenseState = MutableStateFlow(DispenseUiState())
    val dispenseState: StateFlow<DispenseUiState> = _dispenseState.asStateFlow()

    val usbManager = UsbOtgSerialManager(
        context = context,
        scope = scope,
        onSysLog = { msg, isErr ->
            appendLog(if (isErr) LogDirection.ERR else LogDirection.SYS, "USB-OTG", msg)
        }
    )

    val bluetoothManager = BluetoothSppManager(
        context = context,
        scope = scope,
        onSysLog = { msg, isErr ->
            appendLog(if (isErr) LogDirection.ERR else LogDirection.SYS, "BT-SPP", msg)
        }
    )

    private val wifiClient = Esp32WifiClient()
    private var wifiPollJob: Job? = null
    private var currentWifiIp: String = "192.168.4.1"
    private var simulationMode: Boolean = false

    init {
        usbManager.register()
        appendLog(
            LogDirection.SYS,
            "BRIDGE",
            "SellMore Hardware Controller ready (360° Continuous Servo: 90=STOP, 180=FWD 2000ms, 0=REV)"
        )
        scope.launch {
            usbManager.rxLines.collect { line ->
                handleIncomingSerialLine("USB-OTG", line)
            }
        }
        scope.launch {
            bluetoothManager.rxLines.collect { line ->
                handleIncomingSerialLine("BT-SPP", line)
            }
        }
    }

    fun updateSettings(wifiIp: String, simMode: Boolean) {
        currentWifiIp = wifiIp
        simulationMode = simMode
    }

    fun resolveActiveTransport(): HardwareTransport {
        return when {
            usbManager.isConnected.value -> HardwareTransport.USB_OTG
            bluetoothManager.isConnected.value -> HardwareTransport.BLUETOOTH_SPP
            _wifiStatus.value.isReachable && !simulationMode -> HardwareTransport.WIFI_HTTP
            else -> HardwareTransport.ANDROID_BRIDGE
        }
    }

    private fun handleIncomingSerialLine(transport: String, line: String) {
        appendLog(LogDirection.RX, transport, line)
        val speedMatch = Regex("""(?:SPEED|ANGLE)=(\d+)""").find(line)
        if (speedMatch != null) {
            speedMatch.groupValues[1].toIntOrNull()?.let { _servoAngle.value = it.coerceIn(0, 180) }
        }
        if (line.contains("DISPENSE_DONE", ignoreCase = true)) {
            _servoAngle.value = STOP_SPEED
        }
        val cyclesMatch = Regex("""CYCLES=(\d+)""").find(line)
        if (cyclesMatch != null) {
            cyclesMatch.groupValues[1].toIntOrNull()?.let { _dispenseCycles.value = it }
        }
    }

    fun appendLog(direction: LogDirection, transport: String, message: String) {
        val entry = SerialLogEntry(
            id = logIdSeq.getAndIncrement(),
            timestamp = timeFormatter.format(Date()),
            direction = direction,
            transport = transport,
            message = message
        )
        _serialLogs.update { current ->
            (current + entry).takeLast(300)
        }
    }

    fun clearLogs() {
        _serialLogs.value = emptyList()
        appendLog(LogDirection.SYS, "MONITOR", "Serial monitor buffer cleared.")
    }

    fun sendSerialCommand(command: String) {
        val cmd = command.trim()
        if (cmd.isEmpty()) return
        val transport = resolveActiveTransport()
        when (transport) {
            HardwareTransport.USB_OTG -> {
                appendLog(LogDirection.TX, "USB-OTG", cmd)
                usbManager.sendCommand(cmd)
                applyLocalSpeedEcho(cmd)
            }
            HardwareTransport.BLUETOOTH_SPP -> {
                appendLog(LogDirection.TX, "BT-SPP", cmd)
                bluetoothManager.sendCommand(cmd)
                applyLocalSpeedEcho(cmd)
            }
            HardwareTransport.WIFI_HTTP -> {
                appendLog(LogDirection.TX, "WI-FI", cmd)
                scope.launch {
                    when {
                        cmd.equals("DISPENSE", ignoreCase = true) ||
                            cmd.equals("TEST", ignoreCase = true) ||
                            cmd.equals("SERVO:0_TO_180", ignoreCase = true) -> testWifiStart()
                        cmd.equals("SERVO:90", ignoreCase = true) || cmd == "90" -> testWifiReset()
                        else -> testWifiConnection(currentWifiIp)
                    }
                }
            }
            HardwareTransport.ANDROID_BRIDGE -> {
                appendLog(LogDirection.TX, "BRIDGE", cmd)
                simulateBridgeCommand(cmd)
            }
        }
    }

    private fun applyLocalSpeedEcho(cmd: String) {
        val upper = cmd.uppercase(Locale.US)
        if (upper.startsWith("SERVO:") && !upper.contains("_TO_")) {
            upper.removePrefix("SERVO:").toIntOrNull()?.let {
                _servoAngle.value = it.coerceIn(0, 180)
            }
        }
    }

    private fun simulateBridgeCommand(cmd: String) {
        scope.launch {
            delay(120)
            val upper = cmd.uppercase(Locale.US)
            when {
                upper == "PING" -> {
                    appendLog(LogDirection.RX, "BRIDGE", "PONG:SELLMORE_UNO_READY:PIN=9:BAUD=9600")
                }
                upper == "STATUS" -> {
                    appendLog(
                        LogDirection.RX,
                        "BRIDGE",
                        "STATUS:SPEED=${_servoAngle.value}:STOP=90:CYCLES=${_dispenseCycles.value}"
                    )
                }
                upper == "DISPENSE" || upper == "TEST" || upper == "SERVO:0_TO_180" -> {
                    runBridgeDispenseCycle()
                }
                upper.startsWith("SERVO:") || upper.toIntOrNull() != null -> {
                    val speed = upper.removePrefix("SERVO:").toIntOrNull()?.coerceIn(0, 180) ?: STOP_SPEED
                    _servoAngle.value = speed
                    appendLog(LogDirection.RX, "BRIDGE", "ACK:SERVO:SPEED=$speed")
                }
                else -> {
                    appendLog(LogDirection.RX, "BRIDGE", "OK:CMD_ECHO=$cmd")
                }
            }
        }
    }

    private suspend fun runBridgeDispenseCycle() {
        _servoAngle.value = DISPENSE_SPEED
        appendLog(LogDirection.RX, "BRIDGE", "[SERVO][DISPENSE_TIMED] Spinning for ${DISPENSE_DURATION_MS}ms")
        delay(DISPENSE_DURATION_MS)
        _servoAngle.value = STOP_SPEED
        val cycles = _dispenseCycles.value + 1
        _dispenseCycles.value = cycles
        appendLog(LogDirection.RX, "BRIDGE", "ACK:SERVO:DISPENSE_DONE:CYCLES=$cycles")
    }

    fun setServoAngle(speed: Int) {
        val clamped = speed.coerceIn(0, 180)
        sendSerialCommand("SERVO:$clamped")
    }

    fun resetServoHome() {
        val transport = resolveActiveTransport()
        if (transport == HardwareTransport.WIFI_HTTP) {
            scope.launch { testWifiReset() }
        } else {
            // For 360-degree continuous rotation servo, 90 = STOP
            sendSerialCommand("SERVO:$STOP_SPEED")
        }
    }

    fun testDispenseCycle() {
        val transport = resolveActiveTransport()
        when (transport) {
            HardwareTransport.USB_OTG -> {
                appendLog(LogDirection.TX, "USB-OTG", "DISPENSE")
                usbManager.sendCommand("DISPENSE")
                scope.launch {
                    _servoAngle.value = DISPENSE_SPEED
                    delay(DISPENSE_DURATION_MS + 100L)
                    _servoAngle.value = STOP_SPEED
                }
            }
            HardwareTransport.BLUETOOTH_SPP -> {
                appendLog(LogDirection.TX, "BT-SPP", "DISPENSE")
                bluetoothManager.sendCommand("DISPENSE")
                scope.launch {
                    _servoAngle.value = DISPENSE_SPEED
                    delay(DISPENSE_DURATION_MS + 100L)
                    _servoAngle.value = STOP_SPEED
                }
            }
            HardwareTransport.WIFI_HTTP -> {
                scope.launch { testWifiStart() }
            }
            HardwareTransport.ANDROID_BRIDGE -> {
                appendLog(LogDirection.TX, "BRIDGE", "DISPENSE")
                scope.launch { runBridgeDispenseCycle() }
            }
        }
    }

    fun startOrderDispenseFlow(orderRef: String, autoDispense: Boolean, onCompleted: () -> Unit) {
        val transport = resolveActiveTransport()
        scope.launch {
            _dispenseState.value = DispenseUiState(
                isActive = true,
                step = DispenseStep.PAYMENT_CONFIRMED,
                currentServoAngle = STOP_SPEED,
                transport = transport,
                orderRef = orderRef,
                statusNote = "Payment confirmed. Preparing ${transport.displayName}..."
            )
            delay(450)

            _dispenseState.update {
                it.copy(
                    step = DispenseStep.DISPENSING_SERVO,
                    currentServoAngle = if (autoDispense) DISPENSE_SPEED else STOP_SPEED,
                    statusNote = if (autoDispense) {
                        "Spinning 360° continuous servo (Speed 180 for 2000ms)..."
                    } else {
                        "Auto-dispense disabled — marking order ready..."
                    }
                )
            }

            if (autoDispense) {
                when (transport) {
                    HardwareTransport.USB_OTG -> {
                        appendLog(LogDirection.TX, "USB-OTG", "DISPENSE")
                        usbManager.sendCommand("DISPENSE")
                        _servoAngle.value = DISPENSE_SPEED
                    }
                    HardwareTransport.BLUETOOTH_SPP -> {
                        appendLog(LogDirection.TX, "BT-SPP", "DISPENSE")
                        bluetoothManager.sendCommand("DISPENSE")
                        _servoAngle.value = DISPENSE_SPEED
                    }
                    HardwareTransport.WIFI_HTTP -> {
                        appendLog(LogDirection.TX, "WI-FI", "POST /api/start ($currentWifiIp)")
                        _servoAngle.value = DISPENSE_SPEED
                        val res = wifiClient.triggerStart(currentWifiIp)
                        res.onSuccess {
                            appendLog(LogDirection.RX, "WI-FI", it)
                        }.onFailure {
                            appendLog(LogDirection.ERR, "WI-FI", "POST /api/start error: ${it.message}")
                        }
                    }
                    HardwareTransport.ANDROID_BRIDGE -> {
                        appendLog(LogDirection.TX, "BRIDGE", "DISPENSE")
                        _servoAngle.value = DISPENSE_SPEED
                        appendLog(LogDirection.RX, "BRIDGE", "[SERVO][DISPENSE_TIMED] Spinning for ${DISPENSE_DURATION_MS}ms")
                    }
                }
                delay(DISPENSE_DURATION_MS + 100L)
                _servoAngle.value = STOP_SPEED
                val nextCycles = _dispenseCycles.value + 1
                _dispenseCycles.value = nextCycles
                if (transport == HardwareTransport.ANDROID_BRIDGE) {
                    appendLog(
                        LogDirection.RX,
                        "BRIDGE",
                        "ACK:SERVO:DISPENSE_DONE:CYCLES=$nextCycles"
                    )
                }
            } else {
                delay(800)
            }

            _dispenseState.update {
                it.copy(
                    step = DispenseStep.READY_TO_ENJOY,
                    currentServoAngle = STOP_SPEED,
                    statusNote = "Sealed 330ml bottle dispensed! Servo stopped (Speed 90)."
                )
            }
            onHardwareAudit(
                "Dispense Complete ($orderRef)",
                "Transport: ${transport.displayName} • 360° Servo Speed 180 (2000ms) -> 90 STOP",
                "SUCCESS"
            )
            delay(1100)
            _dispenseState.value = DispenseUiState(isActive = false)
            onCompleted()
        }
    }

    fun dismissDispenseModal() {
        _dispenseState.value = DispenseUiState(isActive = false)
    }

    suspend fun testWifiConnection(ip: String): Result<Esp32WifiStatus> {
        currentWifiIp = ip
        if (simulationMode) {
            val simStatus = Esp32WifiStatus(
                state = "SIM_READY",
                servoAngle = _servoAngle.value,
                servoHomeAngle = STOP_SPEED,
                servoActionAngle = DISPENSE_SPEED,
                networkMode = "SIMULATION",
                lastEvent = "Simulated Wi-Fi ping OK",
                cycles = _dispenseCycles.value,
                latencyMs = 14L,
                isReachable = true
            )
            _wifiStatus.value = simStatus
            appendLog(LogDirection.TX, "WI-FI-SIM", "GET http://$ip/api/status")
            appendLog(LogDirection.RX, "WI-FI-SIM", "200 OK (14ms) state=SIM_READY speed=${_servoAngle.value}")
            return Result.success(simStatus)
        }

        appendLog(LogDirection.TX, "WI-FI", "GET http://$ip/api/status")
        val result = wifiClient.fetchStatus(ip)
        result.onSuccess { status ->
            _wifiStatus.value = status
            _servoAngle.value = status.servoAngle
            _dispenseCycles.value = status.cycles
            appendLog(
                LogDirection.RX,
                "WI-FI",
                "200 OK (${status.latencyMs}ms) state=${status.state} speed=${status.servoAngle} mode=${status.networkMode}"
            )
        }.onFailure { err ->
            _wifiStatus.update { it.copy(isReachable = false, lastEvent = err.message ?: "Unreachable") }
            appendLog(LogDirection.ERR, "WI-FI", "GET /api/status failed: ${err.message}")
        }
        return result
    }

    suspend fun testWifiStart() {
        if (simulationMode) {
            appendLog(LogDirection.TX, "WI-FI-SIM", "POST http://$currentWifiIp/api/start")
            runBridgeDispenseCycle()
            return
        }
        appendLog(LogDirection.TX, "WI-FI", "POST http://$currentWifiIp/api/start")
        _servoAngle.value = DISPENSE_SPEED
        wifiClient.triggerStart(currentWifiIp)
            .onSuccess { body ->
                appendLog(LogDirection.RX, "WI-FI", "POST /api/start -> $body")
                delay(DISPENSE_DURATION_MS + 100L)
                _servoAngle.value = STOP_SPEED
            }
            .onFailure { err ->
                _servoAngle.value = STOP_SPEED
                appendLog(LogDirection.ERR, "WI-FI", "POST /api/start failed: ${err.message}")
            }
    }

    suspend fun testWifiReset() {
        if (simulationMode) {
            appendLog(LogDirection.TX, "WI-FI-SIM", "POST http://$currentWifiIp/api/reset")
            _servoAngle.value = STOP_SPEED
            appendLog(LogDirection.RX, "WI-FI-SIM", """{"status":"stopped","speed":90}""")
            return
        }
        appendLog(LogDirection.TX, "WI-FI", "POST http://$currentWifiIp/api/reset")
        wifiClient.triggerReset(currentWifiIp)
            .onSuccess { body ->
                _servoAngle.value = STOP_SPEED
                appendLog(LogDirection.RX, "WI-FI", "POST /api/reset -> $body")
            }
            .onFailure { err ->
                appendLog(LogDirection.ERR, "WI-FI", "POST /api/reset failed: ${err.message}")
            }
    }

    fun setWifiPolling(enabled: Boolean) {
        _wifiPollingActive.value = enabled
        wifiPollJob?.cancel()
        if (enabled) {
            wifiPollJob = scope.launch(Dispatchers.IO) {
                while (isActive && _wifiPollingActive.value) {
                    testWifiConnection(currentWifiIp)
                    delay(1500)
                }
            }
        }
    }

    fun release() {
        wifiPollJob?.cancel()
        usbManager.unregister()
        bluetoothManager.disconnect()
    }
}
