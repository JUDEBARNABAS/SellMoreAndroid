package com.example.model

import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object KioskConstants {
    const val BRAND_NAME = "SellMore Automated Yogurt Dispenser"
    const val MACHINE_ID = "SM-KLA-042"
    const val LOCATION = "KDIS Innovation Hub, Level 2 - Kampala, Uganda"
    const val DEFAULT_ADMIN_PIN = "1234"
    const val DEFAULT_ESP32_IP = "192.168.4.1"
    const val BLUETOOTH_DEVICE_NAME = "SellMore-ESP32"
    const val SPP_UUID_STRING = "00001101-0000-1000-8000-00805f9b34fb"
    const val USB_BAUD_RATE = 115200
    const val MOMO_MERCHANT = "SELLMORE_UG08"
}

fun formatUgx(amount: Int): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "UGX ${formatter.format(amount)}"
}

fun calculateInclusiveVatUgx(subtotalUgx: Int): Int {
    return Math.round(subtotalUgx * 18.0 / 118.0).toInt()
}

fun formatTimestamp(millis: Long, pattern: String = "MMM dd, HH:mm:ss"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}

@Serializable
data class OrderLineItem(
    val productId: String,
    val slotCode: String,
    val name: String,
    val quantity: Int,
    val unitPriceUgx: Int
) {
    val lineTotalUgx: Int get() = quantity * unitPriceUgx
}

data class CartItem(
    val productId: String,
    val slotCode: String,
    val name: String,
    val flavorTag: String,
    val drawableKey: String,
    val unitPriceUgx: Int,
    val quantity: Int,
    val maxStock: Int
) {
    val lineTotalUgx: Int get() = unitPriceUgx * quantity
}

enum class HardwareTransport(val displayName: String, val badgeCode: String) {
    USB_OTG("USB-OTG Serial", "USB-OTG"),
    BLUETOOTH_SPP("Bluetooth SPP", "BT-SPP"),
    WIFI_HTTP("ESP32 Wi-Fi HTTP", "WI-FI"),
    ANDROID_BRIDGE("Android Bridge", "BRIDGE")
}

enum class LogDirection {
    TX, RX, SYS, ERR
}

data class SerialLogEntry(
    val id: Long,
    val timestamp: String,
    val direction: LogDirection,
    val transport: String,
    val message: String
)

data class Esp32WifiStatus(
    val state: String = "IDLE",
    val servoAngle: Int = 0,
    val servoHomeAngle: Int = 0,
    val servoActionAngle: Int = 90,
    val networkMode: String = "OFFLINE",
    val lastEvent: String = "Awaiting telemetry",
    val cycles: Int = 0,
    val latencyMs: Long = 0L,
    val isReachable: Boolean = false
)

enum class DispenseStep {
    PAYMENT_CONFIRMED,
    DISPENSING_SERVO,
    READY_TO_ENJOY
}

data class DispenseUiState(
    val isActive: Boolean = false,
    val step: DispenseStep = DispenseStep.PAYMENT_CONFIRMED,
    val currentServoAngle: Int = 90,
    val transport: HardwareTransport = HardwareTransport.ANDROID_BRIDGE,
    val orderRef: String = "",
    val statusNote: String = "Payment confirmed. Preparing 360° continuous servo (Stop=90)..."
)

@Serializable
sealed interface AppRoute {
    @Serializable
    data object Kiosk : AppRoute

    @Serializable
    data object Admin : AppRoute
}
