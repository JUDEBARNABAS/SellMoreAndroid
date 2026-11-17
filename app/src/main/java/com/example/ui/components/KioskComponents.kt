package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.HardwareTransport
import com.example.model.LogDirection
import com.example.model.SerialLogEntry
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
import kotlin.math.cos
import kotlin.math.sin

fun productImageRes(drawableKey: String): Int {
    return when (drawableKey.lowercase()) {
        "strawberry" -> R.drawable.img_yogurt_strawberry
        "vanilla" -> R.drawable.img_yogurt_vanilla
        "chocolate" -> R.drawable.img_yogurt_chocolate
        else -> R.drawable.img_yogurt_vanilla
    }
}

@Composable
fun HardwareStatusBadge(
    transport: HardwareTransport,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeColor by animateColorAsState(
        targetValue = when (transport) {
            HardwareTransport.USB_OTG -> TelemetryCyan
            HardwareTransport.BLUETOOTH_SPP -> DispenseEmerald
            HardwareTransport.WIFI_HTTP -> TelemetryCyan
            HardwareTransport.ANDROID_BRIDGE -> VanillaAmber
        },
        label = "badgeColor"
    )
    val icon = when (transport) {
        HardwareTransport.USB_OTG -> Icons.Filled.Usb
        HardwareTransport.BLUETOOTH_SPP -> Icons.Filled.BluetoothConnected
        HardwareTransport.WIFI_HTTP -> Icons.Filled.Wifi
        HardwareTransport.ANDROID_BRIDGE -> Icons.Filled.Memory
    }
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .testTag("hardware_status_badge"),
        shape = RoundedCornerShape(50),
        color = badgeColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(pulseAlpha)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Icon(
                imageVector = icon,
                contentDescription = transport.displayName,
                tint = badgeColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = transport.badgeCode,
                style = MaterialTheme.typography.labelMedium,
                color = badgeColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ServoHornDial(
    angle: Int,
    modifier: Modifier = Modifier
) {
    val animatedAngle by animateFloatAsState(
        targetValue = angle.toFloat().coerceIn(0f, 180f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.72f),
        label = "servoNeedle"
    )

    val needleColor = when {
        angle >= 150 -> DispenseEmerald
        angle in 85..95 -> TelemetryCyan
        angle < 30 -> VanillaAmber
        else -> StrawberryPink
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(210.dp, 130.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension * 0.85f
                val center = Offset(size.width / 2f, size.height - 18f)
                val arcTopLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2f, radius * 2f)

                // Track arc (180 -> 0 deg in screen coordinates)
                drawArc(
                    color = EspressoBorder,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 16f, cap = StrokeCap.Round)
                )

                // Active sweep arc
                drawArc(
                    brush = Brush.horizontalGradient(
                        listOf(StrawberryPink, TelemetryCyan, DispenseEmerald)
                    ),
                    startAngle = 180f,
                    sweepAngle = animatedAngle,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 16f, cap = StrokeCap.Round)
                )

                // Tick marks for 0, 45, 90, 135, 180
                listOf(0, 45, 90, 135, 180).forEach { tick ->
                    val rad = Math.toRadians((180.0 + tick))
                    val inner = radius - 22f
                    val outer = radius + 6f
                    drawLine(
                        color = if (tick == 90) DispenseEmerald else TextMuted,
                        start = Offset(
                            center.x + (inner * cos(rad)).toFloat(),
                            center.y + (inner * sin(rad)).toFloat()
                        ),
                        end = Offset(
                            center.x + (outer * cos(rad)).toFloat(),
                            center.y + (outer * sin(rad)).toFloat()
                        ),
                        strokeWidth = if (tick == 90 || tick == 0) 4f else 2f
                    )
                }

                // Servo horn arm
                val needleRad = Math.toRadians(180.0 + animatedAngle)
                val armLength = radius - 14f
                val tip = Offset(
                    center.x + (armLength * cos(needleRad)).toFloat(),
                    center.y + (armLength * sin(needleRad)).toFloat()
                )
                drawLine(
                    color = needleColor,
                    start = center,
                    end = tip,
                    strokeWidth = 9f,
                    cap = StrokeCap.Round
                )
                drawCircle(color = needleColor, radius = 13f, center = center)
                drawCircle(color = EspressoBg, radius = 5f, center = center)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = "$angle",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = needleColor
                )
                Text(
                    text = when {
                        angle in 85..95 -> "STOP (90)"
                        angle > 95 -> "FWD SPIN ($angle)"
                        else -> "REV SPIN ($angle)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0 Full Rev", style = MaterialTheme.typography.labelSmall, color = VanillaAmber)
            Text("90 STOP", style = MaterialTheme.typography.labelSmall, color = TelemetryCyan)
            Text("180 Dispense", style = MaterialTheme.typography.labelSmall, color = DispenseEmerald)
        }
    }
}

@Composable
fun LiveSerialMonitorCard(
    logs: List<SerialLogEntry>,
    onSendCommand: ((String) -> Unit)? = null,
    onClearLogs: (() -> Unit)? = null,
    showControls: Boolean = true,
    heightDp: Int = 220,
    modifier: Modifier = Modifier
) {
    var customCmd by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.lastIndex)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF070606),
        border = BorderStroke(1.dp, EspressoBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Terminal,
                        contentDescription = "Serial Monitor",
                        tint = TelemetryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "LIVE ESP32 SERIAL MONITOR (115200 BAUD)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TelemetryCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (onClearLogs != null) {
                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("clear_serial_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Clear serial monitor",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp.dp)
                    .background(Color(0xFF040404), RoundedCornerShape(10.dp))
                    .border(1.dp, EspressoBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (logs.isEmpty()) {
                    item {
                        Text(
                            text = "// Waiting for 115200 baud TX/RX packets...",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
                items(logs, key = { it.id }) { entry ->
                    val prefixColor = when (entry.direction) {
                        LogDirection.TX -> StrawberryPink
                        LogDirection.RX -> DispenseEmerald
                        LogDirection.SYS -> TelemetryCyan
                        LogDirection.ERR -> ErrorRose
                    }
                    val arrow = when (entry.direction) {
                        LogDirection.TX -> "TX >>"
                        LogDirection.RX -> "RX <<"
                        LogDirection.SYS -> "SYS •"
                        LogDirection.ERR -> "ERR !"
                    }
                    Text(
                        text = "[${entry.timestamp}] $arrow [${entry.transport}] ${entry.message}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = prefixColor,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (showControls && onSendCommand != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf("DISPENSE", "SERVO:0_TO_180", "SERVO:180", "SERVO:90", "SERVO:0", "PING", "STATUS")
                    presets.forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSendCommand(preset) }
                                .testTag("serial_preset_$preset"),
                            color = EspressoSurfaceElevated,
                            border = BorderStroke(1.dp, EspressoBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = preset,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = TelemetryCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customCmd,
                        onValueChange = { customCmd = it },
                        placeholder = {
                            Text(
                                "Send serial command (e.g. SERVO:45)",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.labelMedium.copy(color = TextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TelemetryCyan,
                            unfocusedBorderColor = EspressoBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("serial_command_input")
                    )
                    Button(
                        onClick = {
                            if (customCmd.isNotBlank()) {
                                onSendCommand(customCmd)
                                customCmd = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TelemetryCyan, contentColor = EspressoBg),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                        modifier = Modifier.testTag("serial_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPinDialog(
    expectedPin: String,
    onDismiss: () -> Unit,
    onAuthenticated: () -> Unit
) {
    var entered by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = EspressoSurface,
            border = BorderStroke(1.dp, EspressoBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = StrawberryPink.copy(alpha = 0.16f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Admin Lock",
                            tint = StrawberryPink
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Service & Maintenance",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Enter technician PIN (Default: 1234)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // PIN Dots
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { idx ->
                        val filled = idx < entered.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (filled) StrawberryPink else EspressoSurfaceElevated)
                                .border(1.dp, if (filled) StrawberryPink else EspressoBorder, CircleShape)
                        )
                    }
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMsg!!, color = ErrorRose, style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("DEMO", "0", "DEL")
                )
                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { key ->
                            FilledTonalButton(
                                onClick = {
                                    errorMsg = null
                                    when (key) {
                                        "DEL" -> if (entered.isNotEmpty()) entered = entered.dropLast(1)
                                        "DEMO" -> {
                                            entered = expectedPin
                                            onAuthenticated()
                                        }
                                        else -> {
                                            if (entered.length < 4) {
                                                entered += key
                                                if (entered.length == 4) {
                                                    if (entered == expectedPin) {
                                                        onAuthenticated()
                                                    } else {
                                                        errorMsg = "Incorrect PIN. Try 1234."
                                                        entered = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("pin_key_$key"),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (key == "DEMO") TelemetryCyan.copy(alpha = 0.16f) else EspressoSurfaceElevated,
                                    contentColor = if (key == "DEMO") TelemetryCyan else TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (key == "DEL") {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (key == "DEMO") 11.sp else 16.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    OutlinedButton(
                        onClick = {
                            if (entered == expectedPin) onAuthenticated()
                            else errorMsg = "Enter 4-digit PIN (1234)"
                        },
                        border = BorderStroke(1.dp, StrawberryPink)
                    ) {
                        Text("Unlock", color = StrawberryPink)
                    }
                }
            }
        }
    }
}
