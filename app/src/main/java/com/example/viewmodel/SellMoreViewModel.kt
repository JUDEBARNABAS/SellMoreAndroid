package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountEntity
import com.example.data.local.AuditLogEntity
import com.example.data.local.ProductEntity
import com.example.data.local.SellMoreDatabase
import com.example.data.local.TransactionEntity
import com.example.data.preferences.KioskPreferences
import com.example.data.preferences.KioskPreferencesRepository
import com.example.data.repository.SellMoreRepository
import com.example.hardware.HardwareController
import com.example.hardware.PairedBluetoothSummary
import com.example.hardware.UsbDeviceSummary
import com.example.model.CartItem
import com.example.model.DispenseUiState
import com.example.model.Esp32WifiStatus
import com.example.model.HardwareTransport
import com.example.model.KioskConstants
import com.example.model.OrderLineItem
import com.example.model.SerialLogEntry
import com.example.model.calculateInclusiveVatUgx
import com.example.sheets.GoogleSheetsSyncManager
import com.example.sheets.SheetsAuthOutcome
import com.example.util.KioskAudioManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class KioskUiBanner(
    val message: String,
    val isError: Boolean = false
)

class SellMoreViewModel(application: Application) : AndroidViewModel(application) {
    private val db = SellMoreDatabase.getInstance(application)
    val repository = SellMoreRepository(db)
    private val prefsRepo = KioskPreferencesRepository(application)
    val audioManager = KioskAudioManager(application)
    val sheetsManager = GoogleSheetsSyncManager(application)

    val hardwareController = HardwareController(
        context = application,
        scope = viewModelScope,
        onHardwareAudit = { action, details, severity ->
            viewModelScope.launch {
                repository.logAudit("HARDWARE", action, details, severity)
            }
        }
    )

    val products: StateFlow<List<ProductEntity>> = repository.products.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val accounts: StateFlow<List<AccountEntity>> = repository.accounts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val transactions: StateFlow<List<TransactionEntity>> = repository.transactions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val preferences: StateFlow<KioskPreferences> = prefsRepo.preferencesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), KioskPreferences()
    )

    val serialLogs: StateFlow<List<SerialLogEntry>> = hardwareController.serialLogs
    val servoAngle: StateFlow<Int> = hardwareController.servoAngle
    val dispenseCycles: StateFlow<Int> = hardwareController.dispenseCycles
    val wifiStatus: StateFlow<Esp32WifiStatus> = hardwareController.wifiStatus
    val wifiPollingActive: StateFlow<Boolean> = hardwareController.wifiPollingActive
    val dispenseState: StateFlow<DispenseUiState> = hardwareController.dispenseState
    val usbDevices: StateFlow<List<UsbDeviceSummary>> = hardwareController.usbManager.availableDevices
    val usbConnected: StateFlow<Boolean> = hardwareController.usbManager.isConnected
    val usbDeviceName: StateFlow<String?> = hardwareController.usbManager.connectedDeviceName
    val pairedBtDevices: StateFlow<List<PairedBluetoothSummary>> = hardwareController.bluetoothManager.pairedDevices
    val btConnected: StateFlow<Boolean> = hardwareController.bluetoothManager.isConnected
    val btDeviceName: StateFlow<String?> = hardwareController.bluetoothManager.connectedDeviceName

    val activeTransport: StateFlow<HardwareTransport> = combine(
        usbConnected,
        btConnected,
        wifiStatus,
        preferences
    ) { usb, bt, wifi, prefs ->
        when {
            usb -> HardwareTransport.USB_OTG
            bt -> HardwareTransport.BLUETOOTH_SPP
            wifi.isReachable && !prefs.esp32SimulationMode -> HardwareTransport.WIFI_HTTP
            else -> HardwareTransport.ANDROID_BRIDGE
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HardwareTransport.ANDROID_BRIDGE)

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _currentTimeText = MutableStateFlow("")
    val currentTimeText: StateFlow<String> = _currentTimeText.asStateFlow()

    private val _isScreensaverVisible = MutableStateFlow(false)
    val isScreensaverVisible: StateFlow<Boolean> = _isScreensaverVisible.asStateFlow()

    private val _isCartOpen = MutableStateFlow(false)
    val isCartOpen: StateFlow<Boolean> = _isCartOpen.asStateFlow()

    private val _isPaymentSheetOpen = MutableStateFlow(false)
    val isPaymentSheetOpen: StateFlow<Boolean> = _isPaymentSheetOpen.asStateFlow()

    private val _currentOrderRef = MutableStateFlow(generateOrderRef())
    val currentOrderRef: StateFlow<String> = _currentOrderRef.asStateFlow()

    private val _latestReceipt = MutableStateFlow<TransactionEntity?>(null)
    val latestReceipt: StateFlow<TransactionEntity?> = _latestReceipt.asStateFlow()

    private val _banner = MutableStateFlow<KioskUiBanner?>(null)
    val banner: StateFlow<KioskUiBanner?> = _banner.asStateFlow()

    private val _sheetsAccessToken = MutableStateFlow<String?>(null)
    val sheetsAccessToken: StateFlow<String?> = _sheetsAccessToken.asStateFlow()

    private val _sheetsStatusMessage = MutableStateFlow("Not authorized yet. Sign in to sync with Google Sheets.")
    val sheetsStatusMessage: StateFlow<String> = _sheetsStatusMessage.asStateFlow()

    private val _voiceStatusText = MutableStateFlow<String?>(null)
    val voiceStatusText: StateFlow<String?> = _voiceStatusText.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
        viewModelScope.launch {
            preferences.collect { prefs ->
                hardwareController.updateSettings(prefs.esp32WifiIp, prefs.esp32SimulationMode)
            }
        }
        viewModelScope.launch {
            val clockFormat = SimpleDateFormat("EEE, MMM dd • HH:mm:ss", Locale.US)
            while (isActive) {
                _currentTimeText.value = clockFormat.format(Date())
                delay(1000)
            }
        }
    }

    private fun generateOrderRef(): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        return "${KioskConstants.MACHINE_ID}-$stamp"
    }

    fun showBanner(message: String, isError: Boolean = false) {
        _banner.value = KioskUiBanner(message, isError)
        viewModelScope.launch {
            delay(3600)
            if (_banner.value?.message == message) {
                _banner.value = null
            }
        }
    }

    fun dismissBanner() {
        _banner.value = null
    }

    fun setScreensaverVisible(visible: Boolean) {
        _isScreensaverVisible.value = visible
        if (!visible) {
            audioManager.playTapBeep()
            audioManager.speak("Welcome to SellMore Yogurt. Tap any 330ml chilled bottle to begin.")
        }
    }

    fun openCart(open: Boolean) {
        _isCartOpen.value = open
    }

    fun openPaymentSheet() {
        if (_cart.value.isEmpty()) {
            showBanner("Add at least one yogurt bottle to your cart first.", true)
            return
        }
        _currentOrderRef.value = generateOrderRef()
        _isCartOpen.value = false
        _isPaymentSheetOpen.value = true
    }

    fun closePaymentSheet() {
        _isPaymentSheetOpen.value = false
    }

    fun addToCart(product: ProductEntity, quantity: Int = 1) {
        if (product.stock <= 0) {
            showBanner("${product.name} (${product.slotCode}) is currently out of stock.", true)
            return
        }
        audioManager.playTapBeep()
        _cart.update { items ->
            val existing = items.firstOrNull { it.productId == product.id }
            if (existing != null) {
                val nextQty = (existing.quantity + quantity).coerceAtMost(product.stock)
                items.map {
                    if (it.productId == product.id) it.copy(quantity = nextQty, maxStock = product.stock) else it
                }
            } else {
                items + CartItem(
                    productId = product.id,
                    slotCode = product.slotCode,
                    name = product.name,
                    flavorTag = product.flavorTag,
                    drawableKey = product.drawableKey,
                    unitPriceUgx = product.priceUgx,
                    quantity = quantity.coerceAtMost(product.stock),
                    maxStock = product.stock
                )
            }
        }
        showBanner("Added ${product.name} (${product.slotCode}) to cart")
    }

    fun updateCartQuantity(productId: String, delta: Int) {
        audioManager.playTapBeep()
        _cart.update { items ->
            items.mapNotNull { item ->
                if (item.productId != productId) item
                else {
                    val next = item.quantity + delta
                    if (next <= 0) null
                    else item.copy(quantity = next.coerceAtMost(item.maxStock))
                }
            }
        }
    }

    fun removeFromCart(productId: String) {
        _cart.update { it.filterNot { item -> item.productId == productId } }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    fun handleVoiceTranscript(transcript: String) {
        val currentProducts = products.value
        val matches = KioskAudioManager.parseVoiceOrder(transcript, currentProducts)
        if (matches.isEmpty()) {
            _voiceStatusText.value = "Heard: \"$transcript\" — no matching flavor found."
            audioManager.speak("Sorry, please say Strawberry, Vanilla, or Chocolate yogurt.")
            return
        }
        val summaryParts = mutableListOf<String>()
        matches.forEach { match ->
            addToCart(match.product, match.quantity)
            summaryParts.add("${match.quantity} ${match.product.name}")
        }
        val msg = "Added ${summaryParts.joinToString(" and ")} to your cart."
        _voiceStatusText.value = msg
        audioManager.speak(msg)
    }

    fun clearVoiceStatus() {
        _voiceStatusText.value = null
    }

    fun confirmPhoneQrPayment() {
        val cartSnapshot = _cart.value
        if (cartSnapshot.isEmpty()) return
        val total = cartSnapshot.sumOf { it.lineTotalUgx }
        val lineItems = cartSnapshot.map {
            OrderLineItem(it.productId, it.slotCode, it.name, it.quantity, it.unitPriceUgx)
        }
        executePaymentAndDispense(
            orderRef = _currentOrderRef.value,
            lineItems = lineItems,
            totalUgx = total,
            paymentMethod = "Phone MoMo QR",
            accountId = null,
            remainingBalance = null
        )
    }

    fun processScannedQrPayment(rawQr: String) {
        val payload = rawQr.trim()
        val cartSnapshot = _cart.value
        if (cartSnapshot.isEmpty()) {
            showBanner("Cart is empty.", true)
            return
        }
        val total = cartSnapshot.sumOf { it.lineTotalUgx }
        val lineItems = cartSnapshot.map {
            OrderLineItem(it.productId, it.slotCode, it.name, it.quantity, it.unitPriceUgx)
        }

        if (payload.startsWith("momo://", ignoreCase = true) ||
            payload.startsWith("MOMO:", ignoreCase = true) ||
            payload.startsWith("VOUCHER:", ignoreCase = true)
        ) {
            val label = if (payload.startsWith("VOUCHER:", true)) "Voucher QR" else "Phone MoMo QR"
            executePaymentAndDispense(
                orderRef = _currentOrderRef.value,
                lineItems = lineItems,
                totalUgx = total,
                paymentMethod = label,
                accountId = null,
                remainingBalance = null
            )
            return
        }

        viewModelScope.launch {
            val account = repository.findAccountByQrOrId(payload)
            if (account == null) {
                showBanner("Unrecognized QR card: $payload", true)
                repository.logAudit("PAYMENT", "QR Scan Rejected", "Unrecognized payload: $payload", "WARN")
                return@launch
            }
            if (!account.isActive) {
                showBanner("Account ${account.id} (${account.holderName}) is suspended.", true)
                repository.logAudit("PAYMENT", "Suspended Account Attempt", "${account.id} rejected", "WARN")
                return@launch
            }
            if (account.balanceUgx < total) {
                showBanner(
                    "Insufficient balance on ${account.holderName} (${account.id}). Needs UGX $total, has UGX ${account.balanceUgx}.",
                    true
                )
                return@launch
            }
            val remaining = account.balanceUgx - total
            executePaymentAndDispense(
                orderRef = _currentOrderRef.value,
                lineItems = lineItems,
                totalUgx = total,
                paymentMethod = "Member QR (${account.id})",
                accountId = account.id,
                remainingBalance = remaining
            )
        }
    }

    private fun executePaymentAndDispense(
        orderRef: String,
        lineItems: List<OrderLineItem>,
        totalUgx: Int,
        paymentMethod: String,
        accountId: String?,
        remainingBalance: Int?
    ) {
        _isPaymentSheetOpen.value = false
        audioManager.playSuccessChime()
        val transport = activeTransport.value
        val autoDispense = preferences.value.autoDispenseOnPayment

        viewModelScope.launch {
            val txn = repository.recordOrderAndDecrementStock(
                orderRef = orderRef,
                lineItems = lineItems,
                totalUgx = totalUgx,
                paymentMethod = paymentMethod,
                accountId = accountId,
                remainingBalanceUgx = remainingBalance,
                transportUsed = transport.displayName
            )
            _cart.value = emptyList()
            hardwareController.startOrderDispenseFlow(
                orderRef = orderRef,
                autoDispense = autoDispense,
                onCompleted = {
                    _latestReceipt.value = txn
                    audioManager.speak("Order ready! Please collect your sealed 330ml yogurt bottle.")
                    val token = _sheetsAccessToken.value
                    val sheetId = preferences.value.googleSheetId
                    if (!token.isNullOrBlank() && sheetId.isNotBlank()) {
                        viewModelScope.launch {
                            syncSingleTransactionToSheets(txn, lineItems, token, sheetId)
                        }
                    }
                }
            )
        }
    }

    fun dismissReceiptModal() {
        _latestReceipt.value = null
    }

    fun showReceiptForTransaction(txn: TransactionEntity) {
        _latestReceipt.value = txn
    }

    // --- Inventory & Account Admin Mutations ---
    fun restockProduct(product: ProductEntity, delta: Int) {
        viewModelScope.launch {
            val next = (product.stock + delta).coerceIn(0, product.maxCapacity)
            repository.updateProduct(product.copy(stock = next))
            repository.logAudit("INVENTORY", "Restocked ${product.slotCode}", "${product.name} stock updated to $next/${product.maxCapacity}", "INFO")
        }
    }

    fun fillSlotToCapacity(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(stock = product.maxCapacity))
            repository.logAudit("INVENTORY", "Slot ${product.slotCode} Filled", "${product.name} refilled to ${product.maxCapacity}/${product.maxCapacity}", "SUCCESS")
        }
    }

    fun updateProductConfig(product: ProductEntity, priceUgx: Int, threshold: Int) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(priceUgx = priceUgx.coerceAtLeast(500), lowStockThreshold = threshold.coerceAtLeast(1)))
            repository.logAudit("INVENTORY", "Updated ${product.slotCode} Config", "Price UGX $priceUgx, low threshold $threshold", "INFO")
        }
    }

    fun addNewSlotItem(slotCode: String, name: String, flavorTag: String, priceUgx: Int, maxCapacity: Int) {
        viewModelScope.launch {
            val id = "prod_${slotCode.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")}_${System.currentTimeMillis() % 1000}"
            val drawableKey = when {
                flavorTag.contains("straw", true) -> "strawberry"
                flavorTag.contains("choc", true) -> "chocolate"
                else -> "vanilla"
            }
            val newProduct = ProductEntity(
                id = id,
                slotCode = slotCode.uppercase(Locale.US),
                name = name,
                subtitle = "330ml Sealed Bottle",
                priceUgx = priceUgx,
                calories = 195,
                probioticsBillion = 12,
                stock = maxCapacity,
                maxCapacity = maxCapacity,
                lowStockThreshold = 5,
                badge = "New",
                flavorTag = flavorTag,
                drawableKey = drawableKey
            )
            repository.updateProduct(newProduct)
            repository.logAudit("INVENTORY", "Added Slot ${newProduct.slotCode}", "${newProduct.name} @ UGX $priceUgx", "SUCCESS")
        }
    }

    fun topUpAccount(account: AccountEntity, amountUgx: Int) {
        viewModelScope.launch {
            val next = account.balanceUgx + amountUgx
            repository.upsertAccount(account.copy(balanceUgx = next))
            repository.logAudit("PAYMENT", "Account Top-Up (${account.id})", "+UGX $amountUgx -> New balance UGX $next", "SUCCESS")
        }
    }

    fun toggleAccountActive(account: AccountEntity) {
        viewModelScope.launch {
            val next = !account.isActive
            repository.upsertAccount(account.copy(isActive = next))
            repository.logAudit("SYSTEM", "Account ${if (next) "Activated" else "Suspended"}", "${account.holderName} (${account.id})", "WARN")
        }
    }

    fun createMemberAccount(holderName: String, email: String, initialBalanceUgx: Int) {
        viewModelScope.launch {
            val randomNum = (10000..99999).random()
            val id = "SV-$randomNum"
            val initials = holderName.trim().split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
            val qr = "SV_ACC:$id:SEC_KEY_${initials}${randomNum % 100}"
            val acc = AccountEntity(
                id = id,
                holderName = holderName.trim(),
                email = email.trim(),
                balanceUgx = initialBalanceUgx.coerceAtLeast(0),
                qrPayload = qr,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
            repository.upsertAccount(acc)
            repository.logAudit("SYSTEM", "Created Member Account", "$holderName ($id) • Initial UGX $initialBalanceUgx", "SUCCESS")
        }
    }

    // --- Preferences & Hardware Settings ---
    fun saveWifiIp(ip: String) {
        viewModelScope.launch {
            prefsRepo.setEsp32WifiIp(ip)
            repository.logAudit("HARDWARE", "ESP32 Wi-Fi IP Saved", "Target endpoint set to $ip", "INFO")
        }
    }

    fun setSimulationMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setEsp32SimulationMode(enabled)
            repository.logAudit("HARDWARE", "ESP32 Mode Changed", if (enabled) "Simulation mode enabled" else "Physical hardware mode enabled", "INFO")
        }
    }

    fun setAutoDispense(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setAutoDispenseOnPayment(enabled)
        }
    }

    fun testWifiConnection(ip: String) {
        viewModelScope.launch {
            prefsRepo.setEsp32WifiIp(ip)
            hardwareController.testWifiConnection(ip)
        }
    }

    fun triggerWifiStart() {
        viewModelScope.launch { hardwareController.testWifiStart() }
    }

    fun triggerWifiReset() {
        viewModelScope.launch { hardwareController.testWifiReset() }
    }

    // --- Google Sheets OAuth & Sync ---
    fun saveGoogleSheetConfig(sheetId: String, sheetName: String) {
        viewModelScope.launch {
            prefsRepo.setGoogleSheetConfig(sheetId, sheetName)
            repository.logAudit("SHEETS", "Spreadsheet Config Saved", "ID: $sheetId • Tab: $sheetName", "INFO")
        }
    }

    fun authorizeGoogleSheets(
        activity: Activity,
        onNeedResolution: (android.content.IntentSender) -> Unit
    ) {
        viewModelScope.launch {
            _sheetsStatusMessage.value = "Requesting Google Sheets OAuth authorization..."
            when (val outcome = sheetsManager.requestSheetsAuthorization(activity)) {
                is SheetsAuthOutcome.Authorized -> {
                    _sheetsAccessToken.value = outcome.accessToken
                    _sheetsStatusMessage.value = "Google Sheets authorized and ready to sync."
                    repository.logAudit("SHEETS", "OAuth Authorized", "Google Sheets token acquired", "SUCCESS")
                    verifySheetIfConfigured(outcome.accessToken)
                }
                is SheetsAuthOutcome.NeedsResolution -> {
                    onNeedResolution(outcome.intentSender)
                }
                is SheetsAuthOutcome.Failed -> {
                    _sheetsStatusMessage.value = "Auth failed: ${outcome.reason}"
                    repository.logAudit("SHEETS", "OAuth Failed", outcome.reason, "ERROR")
                }
            }
        }
    }

    fun handleSheetsAuthIntentResult(data: Intent?) {
        val result = sheetsManager.extractTokenFromResult(data)
        result.onSuccess { token ->
            _sheetsAccessToken.value = token
            _sheetsStatusMessage.value = "Google Sheets authorized!"
            viewModelScope.launch {
                repository.logAudit("SHEETS", "OAuth Consent Granted", "Google Sheets access token ready", "SUCCESS")
                verifySheetIfConfigured(token)
            }
        }.onFailure { err ->
            _sheetsStatusMessage.value = "Authorization cancelled or failed: ${err.message}"
        }
    }

    private suspend fun verifySheetIfConfigured(token: String) {
        val id = preferences.value.googleSheetId
        if (id.isBlank()) return
        sheetsManager.verifySpreadsheet(token, id)
            .onSuccess { title ->
                _sheetsStatusMessage.value = "Connected to Sheet: \"$title\""
            }
            .onFailure { err ->
                _sheetsStatusMessage.value = "Connected OAuth, Sheet check error: ${err.message}"
            }
    }

    fun syncAllUnsyncedTransactions() {
        val token = _sheetsAccessToken.value
        val prefs = preferences.value
        if (token.isNullOrBlank()) {
            _sheetsStatusMessage.value = "Please sign in and authorize Google Sheets first."
            return
        }
        if (prefs.googleSheetId.isBlank()) {
            _sheetsStatusMessage.value = "Enter a Google Spreadsheet ID before syncing."
            return
        }
        viewModelScope.launch {
            val unsynced = transactions.value.filter { !it.syncedToSheets }
            if (unsynced.isEmpty()) {
                _sheetsStatusMessage.value = "All transactions are already synced to Google Sheets."
                return@launch
            }
            val payload = unsynced.map { txn -> txn to repository.decodeItems(txn.itemsJson) }
            _sheetsStatusMessage.value = "Syncing ${payload.size} transactions..."
            sheetsManager.appendTransactions(token, prefs.googleSheetId, prefs.googleSheetName, payload)
                .onSuccess { count ->
                    repository.markTransactionsSynced(unsynced.map { it.id })
                    _sheetsStatusMessage.value = "Synced $count transaction(s) to Google Sheets!"
                    repository.logAudit("SHEETS", "Batch Sync Complete", "Appended $count rows to ${prefs.googleSheetName}", "SUCCESS")
                }
                .onFailure { err ->
                    _sheetsStatusMessage.value = "Sync failed: ${err.message}"
                    repository.logAudit("SHEETS", "Batch Sync Failed", err.message ?: "Unknown error", "ERROR")
                }
        }
    }

    private suspend fun syncSingleTransactionToSheets(
        txn: TransactionEntity,
        items: List<OrderLineItem>,
        token: String,
        sheetId: String
    ) {
        sheetsManager.appendTransactions(
            token,
            sheetId,
            preferences.value.googleSheetName,
            listOf(txn to items)
        ).onSuccess {
            repository.markTransactionsSynced(listOf(txn.id))
            repository.logAudit("SHEETS", "Auto-Synced ${txn.orderRef}", "Appended to Google Sheet", "SUCCESS")
        }.onFailure { err ->
            repository.logAudit("SHEETS", "Auto-Sync Error", err.message ?: "Failed", "WARN")
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            repository.clearAuditLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        hardwareController.release()
        audioManager.release()
    }
}
