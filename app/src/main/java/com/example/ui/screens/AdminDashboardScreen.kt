package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.TransactionEntity
import com.example.model.KioskConstants
import com.example.ui.components.HardwareStatusBadge
import com.example.ui.theme.EspressoBg
import com.example.ui.theme.EspressoBorder
import com.example.ui.theme.EspressoSurface
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.SellMoreViewModel

@Composable
fun AdminDashboardScreen(
    viewModel: SellMoreViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val products by viewModel.products.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val activeTransport by viewModel.activeTransport.collectAsStateWithLifecycle()
    val serialLogs by viewModel.serialLogs.collectAsStateWithLifecycle()
    val servoAngle by viewModel.servoAngle.collectAsStateWithLifecycle()
    val wifiStatus by viewModel.wifiStatus.collectAsStateWithLifecycle()
    val wifiPollingActive by viewModel.wifiPollingActive.collectAsStateWithLifecycle()
    val usbDevices by viewModel.usbDevices.collectAsStateWithLifecycle()
    val usbConnected by viewModel.usbConnected.collectAsStateWithLifecycle()
    val usbDeviceName by viewModel.usbDeviceName.collectAsStateWithLifecycle()
    val pairedBtDevices by viewModel.pairedBtDevices.collectAsStateWithLifecycle()
    val btConnected by viewModel.btConnected.collectAsStateWithLifecycle()
    val btDeviceName by viewModel.btDeviceName.collectAsStateWithLifecycle()
    val sheetsToken by viewModel.sheetsAccessToken.collectAsStateWithLifecycle()
    val sheetsStatusMsg by viewModel.sheetsStatusMessage.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var inspectedTxn by remember { mutableStateOf<TransactionEntity?>(null) }

    val tabTitles = listOf(
        "Inventory",
        "Accounts",
        "Transactions",
        "Google Sheets",
        "ESP32 Wi-Fi",
        "OTG / BT & Servo",
        "Analytics",
        "Audit Logs"
    )

    Scaffold(
        containerColor = EspressoBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(
                color = EspressoSurface,
                border = BorderStroke(1.dp, EspressoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("admin_back_to_kiosk")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Kiosk",
                                    tint = TextPrimary
                                )
                            }
                            Column {
                                Text("Service & Maintenance", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                Text(KioskConstants.MACHINE_ID, style = MaterialTheme.typography.labelSmall, color = TelemetryCyan)
                            }
                        }
                        HardwareStatusBadge(
                            transport = activeTransport,
                            onClick = { selectedTab = 5 }
                        )
                    }

                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = EspressoSurface,
                        contentColor = TelemetryCyan,
                        edgePadding = 12.dp
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == index) TelemetryCyan else TextSecondary
                                    )
                                },
                                modifier = Modifier.testTag("admin_tab_$index")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> InventoryTab(
                    products = products,
                    onRestock = { p, delta -> viewModel.restockProduct(p, delta) },
                    onFillSlot = { p -> viewModel.fillSlotToCapacity(p) },
                    onEditConfig = { p, price, threshold -> viewModel.updateProductConfig(p, price, threshold) },
                    onAddSlot = { code, name, flavor, price, cap ->
                        viewModel.addNewSlotItem(code, name, flavor, price, cap)
                    }
                )
                1 -> AccountsTab(
                    accounts = accounts,
                    onTopUp = { acc, amt -> viewModel.topUpAccount(acc, amt) },
                    onToggleActive = { acc -> viewModel.toggleAccountActive(acc) },
                    onCreateAccount = { name, email, bal -> viewModel.createMemberAccount(name, email, bal) }
                )
                2 -> TransactionsTab(
                    transactions = transactions,
                    decodeItems = { json -> viewModel.repository.decodeItems(json) },
                    onViewReceipt = { txn -> inspectedTxn = txn }
                )
                3 -> GoogleSheetsTab(
                    preferences = preferences,
                    isAuthorized = !sheetsToken.isNullOrBlank(),
                    statusMessage = sheetsStatusMsg,
                    unsyncedCount = transactions.count { !it.syncedToSheets },
                    onSaveConfig = { id, name -> viewModel.saveGoogleSheetConfig(id, name) },
                    onAuthorize = { act, onRes -> viewModel.authorizeGoogleSheets(act, onRes) },
                    onAuthIntentResult = { data -> viewModel.handleSheetsAuthIntentResult(data) },
                    onSyncNow = { viewModel.syncAllUnsyncedTransactions() }
                )
                4 -> Esp32WifiTab(
                    preferences = preferences,
                    wifiStatus = wifiStatus,
                    wifiPollingActive = wifiPollingActive,
                    serialLogs = serialLogs,
                    onSaveAndTest = { ip -> viewModel.testWifiConnection(ip) },
                    onToggleSimulation = { enabled -> viewModel.setSimulationMode(enabled) },
                    onTogglePolling = { enabled -> viewModel.hardwareController.setWifiPolling(enabled) },
                    onTriggerStart = { viewModel.triggerWifiStart() },
                    onTriggerReset = { viewModel.triggerWifiReset() },
                    onClearLogs = { viewModel.hardwareController.clearLogs() },
                    onSendSerialCommand = { cmd -> viewModel.hardwareController.sendSerialCommand(cmd) }
                )
                5 -> Esp32OtgBtTab(
                    preferences = preferences,
                    usbDevices = usbDevices,
                    usbConnected = usbConnected,
                    usbDeviceName = usbDeviceName,
                    pairedBtDevices = pairedBtDevices,
                    btConnected = btConnected,
                    btDeviceName = btDeviceName,
                    servoAngle = servoAngle,
                    serialLogs = serialLogs,
                    onRefreshUsb = { viewModel.hardwareController.usbManager.refreshDevices() },
                    onConnectUsb = { id -> viewModel.hardwareController.usbManager.connectFirstOrDevice(id) },
                    onDisconnectUsb = { viewModel.hardwareController.usbManager.disconnect() },
                    onRefreshBt = { viewModel.hardwareController.bluetoothManager.refreshPairedDevices() },
                    onConnectBt = { addr -> viewModel.hardwareController.bluetoothManager.connectToTargetOrAddress(addr) },
                    onDisconnectBt = { viewModel.hardwareController.bluetoothManager.disconnect() },
                    onSetServoAngle = { deg -> viewModel.hardwareController.setServoAngle(deg) },
                    onTestDispense = { viewModel.hardwareController.testDispenseCycle() },
                    onResetHome = { viewModel.hardwareController.resetServoHome() },
                    onToggleAutoDispense = { enabled -> viewModel.setAutoDispense(enabled) },
                    onSendCommand = { cmd -> viewModel.hardwareController.sendSerialCommand(cmd) },
                    onClearLogs = { viewModel.hardwareController.clearLogs() }
                )
                6 -> SalesAnalyticsTab(
                    transactions = transactions,
                    decodeItems = { json -> viewModel.repository.decodeItems(json) }
                )
                7 -> AuditLogsTab(
                    logs = auditLogs,
                    onClearLogs = { viewModel.clearAuditLogs() }
                )
            }
        }
    }

    inspectedTxn?.let { txn ->
        DigitalReceiptDialog(
            transaction = txn,
            items = viewModel.repository.decodeItems(txn.itemsJson),
            onDismiss = { inspectedTxn = null }
        )
    }
}
