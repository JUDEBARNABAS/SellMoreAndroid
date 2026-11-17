package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AccountEntity
import com.example.data.local.TransactionEntity
import com.example.model.CartItem
import com.example.model.DispenseStep
import com.example.model.DispenseUiState
import com.example.model.KioskConstants
import com.example.model.OrderLineItem
import com.example.model.SerialLogEntry
import com.example.model.calculateInclusiveVatUgx
import com.example.model.formatTimestamp
import com.example.model.formatUgx
import com.example.ui.components.CameraQrScannerView
import com.example.ui.components.LiveSerialMonitorCard
import com.example.ui.theme.DispenseEmerald
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
import com.example.util.QrCodeGenerator
import com.example.util.ReceiptPrinter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QrPaymentDialog(
    orderRef: String,
    cartItems: List<CartItem>,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirmPhoneQrPayment: () -> Unit,
    onQrPayloadScanned: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val totalUgx = cartItems.sumOf { it.lineTotalUgx }
    val vatUgx = calculateInclusiveVatUgx(totalUgx)
    val momoUri = remember(orderRef, totalUgx) {
        "momo://pay?merchant=${KioskConstants.MOMO_MERCHANT}&ref=$orderRef&amount=$totalUgx&currency=UGX"
    }
    val qrBitmap = remember(momoUri) { QrCodeGenerator.generateQrBitmap(momoUri, 420) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = EspressoSurface,
            border = BorderStroke(1.dp, EspressoBorder)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Confirm Phone or QR Payment",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Ref: $orderRef • ${cartItems.sumOf { it.quantity }} bottle(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = TelemetryCyan
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_payment_dialog")) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = EspressoSurfaceElevated,
                    border = BorderStroke(1.dp, EspressoBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Due (VAT 18% incl.)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text("VAT Component: ${formatUgx(vatUgx)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Text(
                            text = formatUgx(totalUgx),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = StrawberryPink
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = EspressoBg,
                    contentColor = TelemetryCyan,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Scan Kiosk QR", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_scan_kiosk_qr")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Camera QR Scanner", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_camera_qr_scanner")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap,
                                    contentDescription = "SellMore Mobile Money Payment QR",
                                    modifier = Modifier
                                        .size(210.dp)
                                        .padding(12.dp)
                                )
                            } else {
                                Box(modifier = Modifier.size(210.dp), contentAlignment = Alignment.Center) {
                                    Text("QR Ready", color = Color.Black)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = momoUri,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onConfirmPhoneQrPayment,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DispenseEmerald,
                                contentColor = EspressoBg
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("confirm_phone_qr_payment_button")
                        ) {
                            Icon(Icons.Filled.PhoneAndroid, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Confirm Phone QR Payment",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                } else {
                    CameraQrScannerView(
                        onQrDetected = { raw ->
                            onQrPayloadScanned(raw)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Quick-Test Account & MoMo QR Cards:",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.forEach { acc ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onQrPayloadScanned(acc.qrPayload) }
                                    .testTag("quick_qr_${acc.id}"),
                                color = EspressoSurfaceElevated,
                                border = BorderStroke(1.dp, TelemetryCyan.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CreditCard,
                                        contentDescription = null,
                                        tint = TelemetryCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "${acc.holderName} (${acc.id})",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TextPrimary,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = formatUgx(acc.balanceUgx),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DispenseEmerald
                                        )
                                    }
                                }
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onQrPayloadScanned(momoUri) }
                                .testTag("quick_qr_momo"),
                            color = StrawberryPink.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, StrawberryPink.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = StrawberryPink, modifier = Modifier.size(16.dp))
                                Text("Mobile Money QR", style = MaterialTheme.typography.labelLarge, color = StrawberryPink)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DispenseProgressDialog(
    state: DispenseUiState,
    serialLogs: List<SerialLogEntry>,
    onDismiss: () -> Unit
) {
    if (!state.isActive) return

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = EspressoSurface,
            border = BorderStroke(1.5.dp, DispenseEmerald.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Immediate Servo Dispense",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "${state.orderRef} • ${state.transport.displayName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TelemetryCyan
                        )
                    }
                    val isSpinning = state.currentServoAngle > 95 || state.currentServoAngle < 85
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSpinning) DispenseEmerald.copy(alpha = 0.18f) else TelemetryCyan.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, if (isSpinning) DispenseEmerald else TelemetryCyan)
                    ) {
                        Text(
                            text = if (isSpinning) "SPEED: ${state.currentServoAngle} (FWD)" else "STOP: ${state.currentServoAngle}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (isSpinning) DispenseEmerald else TelemetryCyan,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val steps = listOf(
                    DispenseStep.PAYMENT_CONFIRMED to "1. Payment Confirmed",
                    DispenseStep.DISPENSING_SERVO to "2. Dispensing (360° Spin 2000ms)",
                    DispenseStep.READY_TO_ENJOY to "3. Ready to Enjoy (Sealed 330ml bottle)"
                )

                steps.forEach { (stepEnum, label) ->
                    val reached = state.step.ordinal >= stepEnum.ordinal
                    val current = state.step == stepEnum
                    val stepColor by animateColorAsState(
                        targetValue = when {
                            reached && !current -> DispenseEmerald
                            current -> TelemetryCyan
                            else -> TextMuted
                        },
                        label = "stepColor"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (current && state.step != DispenseStep.READY_TO_ENJOY) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = TelemetryCyan
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = label,
                                tint = stepColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (reached) TextPrimary else TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = state.statusNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DispenseEmerald
                )

                Spacer(modifier = Modifier.height(14.dp))
                LiveSerialMonitorCard(
                    logs = serialLogs.takeLast(8),
                    showControls = false,
                    heightDp = 135
                )

                if (state.step == DispenseStep.READY_TO_ENJOY) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun DigitalReceiptDialog(
    transaction: TransactionEntity,
    items: List<OrderLineItem>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = EspressoSurface,
            border = BorderStroke(1.dp, DispenseEmerald.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DispenseEmerald.copy(alpha = 0.18f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = DispenseEmerald)
                            }
                        }
                        Column {
                            Text("Digital Receipt", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Text(transaction.orderRef, style = MaterialTheme.typography.labelMedium, color = TelemetryCyan)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_receipt_dialog")) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = EspressoBorder)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "${KioskConstants.BRAND_NAME} • ${KioskConstants.MACHINE_ID}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = formatTimestamp(transaction.timestamp, "yyyy-MM-dd HH:mm:ss"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.quantity}x ${item.name} [${item.slotCode}]",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatUgx(item.lineTotalUgx),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = EspressoBorder)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("VAT (18% inclusive)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(formatUgx(transaction.vatUgx), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Paid", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        formatUgx(transaction.totalUgx),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DispenseEmerald
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Payment: ${transaction.paymentMethod} • Port: ${transaction.transportUsed}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                if (transaction.remainingBalanceUgx != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Remaining Account Balance: ${formatUgx(transaction.remainingBalanceUgx)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TelemetryCyan
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { ReceiptPrinter.printReceipt(context, transaction, items) },
                        border = BorderStroke(1.dp, TelemetryCyan),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("print_receipt_button")
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, tint = TelemetryCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print", color = TelemetryCyan)
                    }
                    Button(
                        onClick = { ReceiptPrinter.shareReceipt(context, transaction, items) },
                        colors = ButtonDefaults.buttonColors(containerColor = StrawberryPink, contentColor = TextPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_receipt_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            }
        }
    }
}
