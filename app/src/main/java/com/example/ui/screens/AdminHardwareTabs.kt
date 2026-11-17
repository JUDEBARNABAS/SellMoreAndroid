package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.preferences.KioskPreferences
import com.example.hardware.Esp32Sketches
import com.example.hardware.PairedBluetoothSummary
import com.example.hardware.UsbDeviceSummary
import com.example.model.Esp32WifiStatus
import com.example.model.SerialLogEntry
import com.example.ui.components.LiveSerialMonitorCard
import com.example.ui.components.ServoHornDial
import com.example.ui.theme.DispenseEmerald
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.EspressoBg
import com.example.ui.theme.EspressoBorder
import com.example.ui.theme.EspressoSurface
import com.example.ui.theme.EspressoSurfaceElevated
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StrawberryPink
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VanillaAmber

@Composable
fun Esp32WifiTab(
    preferences: KioskPreferences,
    wifiStatus: Esp32WifiStatus,
    wifiPollingActive: Boolean,
    serialLogs: List<SerialLogEntry>,
    onSaveAndTest: (String) -> Unit,
    onToggleSimulation: (Boolean) -> Unit,
    onTogglePolling: (Boolean) -> Unit,
    onTriggerStart: () -> Unit,
    onTriggerReset: () -> Unit,
    onClearLogs: () -> Unit,
    onSendSerialCommand: (String) -> Unit
) {
    val context = LocalContext.current
    var ipInput by remember(preferences.esp32WifiIp) { mutableStateOf(preferences.esp32WifiIp) }
    var copiedBanner by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = EspressoSurface,
                border = BorderStroke(1.dp, EspressoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Wifi, contentDescription = null, tint = TelemetryCyan)
                        Text("ESP32 Wi-Fi HTTP Controller", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    }

                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        label = { Text("ESP32 Host / IP (Default: 192.168.4.1)") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.labelMedium.copy(color = TextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TelemetryCyan,
                            unfocusedBorderColor = EspressoBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("esp32_wifi_ip_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Simulation Mode", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                text = if (preferences.esp32SimulationMode) "Virtual ESP32 responder active" else "Physical LAN/AP HTTP requests",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = preferences.esp32SimulationMode,
                            onCheckedChange = onToggleSimulation,
                            colors = SwitchDefaults.colors(checkedThumbColor = EspressoBg, checkedTrackColor = VanillaAmber)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Poll GET /api/status (1500ms)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Real-time telemetry refresh", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Switch(
                            checked = wifiPollingActive,
                            onCheckedChange = onTogglePolling,
                            colors = SwitchDefaults.colors(checkedThumbColor = EspressoBg, checkedTrackColor = TelemetryCyan)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onSaveAndTest(ipInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = TelemetryCyan, contentColor = EspressoBg),
                            modifier = Modifier.weight(1f).testTag("wifi_save_test_button")
                        ) {
                            Text("Save & Test", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onTriggerStart,
                            colors = ButtonDefaults.buttonColors(containerColor = DispenseEmerald, contentColor = EspressoBg),
                            modifier = Modifier.weight(1f).testTag("wifi_post_start_button")
                        ) {
                            Text("POST /start", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onTriggerReset,
                            border = BorderStroke(1.dp, StrawberryPink)
                        ) {
                            Text("Reset 0°", color = StrawberryPink)
                        }
                    }
                }
            }
        }

        // Live 3-card Servo GPIO 18 Telemetry
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryMetricCard(
                    title = "GPIO 18 ANGLE",
                    value = "${wifiStatus.servoAngle}°",
                    sub = "${wifiStatus.servoHomeAngle}° -> ${wifiStatus.servoActionAngle}°",
                    accent = TelemetryCyan,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricCard(
                    title = "PAYMENT TRIGGER",
                    value = "Immediate",
                    sub = "${wifiStatus.latencyMs}ms • ${wifiStatus.networkMode}",
                    accent = DispenseEmerald,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricCard(
                    title = "HOLD & RETURN",
                    value = "90° (2s) -> 0°",
                    sub = "Cycles: ${wifiStatus.cycles}",
                    accent = StrawberryPink,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            LiveSerialMonitorCard(
                logs = serialLogs,
                onSendCommand = onSendSerialCommand,
                onClearLogs = onClearLogs,
                heightDp = 190
            )
        }

        item {
            SketchCopyCard(
                title = "ESP32 Wi-Fi HTTP + Serial Monitor Sketch (.ino)",
                sketch = Esp32Sketches.wifiHttpSketch,
                copied = copiedBanner,
                onCopy = {
                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("ESP32 WiFi Sketch", Esp32Sketches.wifiHttpSketch))
                    copiedBanner = true
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Esp32OtgBtTab(
    preferences: KioskPreferences,
    usbDevices: List<UsbDeviceSummary>,
    usbConnected: Boolean,
    usbDeviceName: String?,
    pairedBtDevices: List<PairedBluetoothSummary>,
    btConnected: Boolean,
    btDeviceName: String?,
    servoAngle: Int,
    serialLogs: List<SerialLogEntry>,
    onRefreshUsb: () -> Unit,
    onConnectUsb: (Int?) -> Unit,
    onDisconnectUsb: () -> Unit,
    onRefreshBt: () -> Unit,
    onConnectBt: (String?) -> Unit,
    onDisconnectBt: () -> Unit,
    onSetServoAngle: (Int) -> Unit,
    onTestDispense: () -> Unit,
    onResetHome: () -> Unit,
    onToggleAutoDispense: (Boolean) -> Unit,
    onSendCommand: (String) -> Unit,
    onClearLogs: () -> Unit
) {
    val context = LocalContext.current
    var copiedSketch by remember { mutableStateOf(false) }

    val btPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    var pendingBtAddress by remember { mutableStateOf<String?>(null) }
    val btPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            onRefreshBt()
            onConnectBt(pendingBtAddress)
        }
    }

    val connectBluetoothWithPermission: (String?) -> Unit = { address ->
        val granted = btPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) {
            onRefreshBt()
            onConnectBt(address)
        } else {
            pendingBtAddress = address
            btPermLauncher.launch(btPermissions)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // USB-OTG & Bluetooth SPP Connections
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = EspressoSurface,
                border = BorderStroke(1.dp, EspressoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Native Android USB-OTG & Bluetooth SPP (Uno / ESP32)", style = MaterialTheme.typography.titleLarge, color = TextPrimary)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { if (usbConnected) onDisconnectUsb() else onConnectUsb(null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (usbConnected) ErrorRose else TelemetryCyan,
                                contentColor = EspressoBg
                            ),
                            modifier = Modifier.weight(1f).testTag("connect_usb_otg_button")
                        ) {
                            Icon(Icons.Filled.Usb, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (usbConnected) "Disconnect USB" else "USB-OTG (115200)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { if (btConnected) onDisconnectBt() else connectBluetoothWithPermission(null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (btConnected) ErrorRose else DispenseEmerald,
                                contentColor = EspressoBg
                            ),
                            modifier = Modifier.weight(1f).testTag("connect_bt_spp_button")
                        ) {
                            Icon(Icons.Filled.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (btConnected) "Disconnect BT" else "BT HC-05 / ESP32",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Text(
                        text = buildString {
                            append("USB: ${usbDeviceName ?: "Not attached"}  •  ")
                            append("BT: ${btDeviceName ?: "Disconnected"}")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = TelemetryCyan
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Detected USB-OTG Bridges (${usbDevices.size})", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        OutlinedButton(onClick = onRefreshUsb, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan", fontSize = 12.sp)
                        }
                    }
                    if (usbDevices.isEmpty()) {
                        Text("Plug ESP32 via USB-OTG adapter (CP210x / CH340 / FTDI / ESP32-S3) or use Android Bridge.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    } else {
                        usbDevices.forEach { dev ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EspressoSurfaceElevated,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onConnectUsb(dev.deviceId) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${dev.productName} (${dev.vendorIdHex}:${dev.productIdHex})", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                                    Text(dev.driverLabel, style = MaterialTheme.typography.labelSmall, color = TelemetryCyan)
                                }
                            }
                        }
                    }

                    if (pairedBtDevices.isNotEmpty()) {
                        Text("Paired Bluetooth Devices:", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            pairedBtDevices.forEach { bt ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = EspressoSurfaceElevated,
                                    border = BorderStroke(1.dp, if (bt.isSellMoreTarget) DispenseEmerald else EspressoBorder),
                                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { connectBluetoothWithPermission(bt.address) }
                                ) {
                                    Text(
                                        text = "${bt.name} (${bt.address})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (bt.isSellMoreTarget) DispenseEmerald else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Animated Servo Horn Dial & Controls
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = EspressoSurface,
                border = BorderStroke(1.dp, EspressoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("360° Continuous Servo Speed Dial (Pin 9 / GPIO 18)", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    ServoHornDial(angle = servoAngle, modifier = Modifier.fillMaxWidth(0.85f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            0 to "0 Rev",
                            45 to "45",
                            90 to "90 STOP",
                            135 to "135",
                            180 to "180 Fwd"
                        ).forEach { (spd, label) ->
                            OutlinedButton(
                                onClick = { onSetServoAngle(spd) },
                                border = BorderStroke(1.dp, if (servoAngle == spd) TelemetryCyan else EspressoBorder),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f).testTag("servo_angle_$spd")
                            ) {
                                Text(label, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = if (servoAngle == spd) TelemetryCyan else TextPrimary)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onTestDispense,
                            colors = ButtonDefaults.buttonColors(containerColor = DispenseEmerald, contentColor = EspressoBg),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("test_dispense_0_90_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Dispense (2s Spin)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onResetHome,
                            border = BorderStroke(1.dp, StrawberryPink),
                            modifier = Modifier.height(48.dp).testTag("reset_home_0_button")
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = StrawberryPink)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop Servo (90)", color = StrawberryPink, fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto-Dispense on Payment", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Send DISPENSE (180 for 2s -> 90 Stop) on QR confirmation", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Switch(
                            checked = preferences.autoDispenseOnPayment,
                            onCheckedChange = onToggleAutoDispense,
                            colors = SwitchDefaults.colors(checkedThumbColor = EspressoBg, checkedTrackColor = DispenseEmerald)
                        )
                    }
                }
            }
        }

        // Full Live Serial Monitor
        item {
            LiveSerialMonitorCard(
                logs = serialLogs,
                onSendCommand = onSendCommand,
                onClearLogs = onClearLogs,
                showControls = true,
                heightDp = 220
            )
        }

        // Copyable Arduino Uno 360 + HC-05/HC-06 Sketch
        item {
            SketchCopyCard(
                title = "Arduino Uno 360° Continuous Servo + HC-05/HC-06 Sketch (.ino)",
                sketch = Esp32Sketches.bluetoothAndUsbOtgSketch,
                copied = copiedSketch,
                onCopy = {
                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("Arduino Uno 360 BT Sketch", Esp32Sketches.bluetoothAndUsbOtgSketch))
                    copiedSketch = true
                }
            )
        }
    }
}

@Composable
private fun TelemetryMetricCard(
    title: String,
    value: String,
    sub: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = EspressoSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accent)
            Spacer(modifier = Modifier.height(2.dp))
            Text(sub, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun SketchCopyCard(
    title: String,
    sketch: String,
    copied: Boolean,
    onCopy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = EspressoSurface,
        border = BorderStroke(1.dp, EspressoBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onCopy, border = BorderStroke(1.dp, TelemetryCyan)) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = TelemetryCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (copied) "Copied!" else "Copy .ino", color = TelemetryCyan, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = EspressoBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = sketch.lines().take(12).joinToString("\n") + "\n// ... (${sketch.lines().size} lines total)",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}
