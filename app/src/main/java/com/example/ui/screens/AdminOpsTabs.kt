package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.AccountEntity
import com.example.data.local.ProductEntity
import com.example.data.local.TransactionEntity
import com.example.data.preferences.KioskPreferences
import com.example.model.KioskConstants
import com.example.model.OrderLineItem
import com.example.model.formatTimestamp
import com.example.model.formatUgx
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
import com.example.util.QrCodeGenerator

@Composable
fun InventoryTab(
    products: List<ProductEntity>,
    onRestock: (ProductEntity, Int) -> Unit,
    onFillSlot: (ProductEntity) -> Unit,
    onEditConfig: (ProductEntity, Int, Int) -> Unit,
    onAddSlot: (String, String, String, Int, Int) -> Unit
) {
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddSlotDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Slot Inventory", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text("${products.sumOf { it.stock }} bottles chilled", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Button(
                    onClick = { showAddSlotDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = StrawberryPink, contentColor = TextPrimary),
                    modifier = Modifier.testTag("add_slot_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Slot")
                }
            }
        }

        items(products, key = { it.id }) { product ->
            val ratio = (product.stock.toFloat() / product.maxCapacity.coerceAtLeast(1)).coerceIn(0f, 1f)
            val color = when {
                product.stock <= 0 -> ErrorRose
                product.stock <= product.lowStockThreshold -> VanillaAmber
                else -> DispenseEmerald
            }
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "[${product.slotCode}] ${product.name}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "${formatUgx(product.priceUgx)} • Alert <= ${product.lowStockThreshold}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                        IconButton(onClick = { editingProduct = product }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Slot", tint = TelemetryCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Stock Level", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("${product.stock} / ${product.maxCapacity}", style = MaterialTheme.typography.labelMedium, color = color)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { ratio },
                        color = color,
                        trackColor = EspressoSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onRestock(product, 1) },
                            border = BorderStroke(1.dp, TelemetryCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+1 Bottle", color = TelemetryCyan)
                        }
                        Button(
                            onClick = { onFillSlot(product) },
                            colors = ButtonDefaults.buttonColors(containerColor = DispenseEmerald, contentColor = EspressoBg),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Fill Slot (${product.maxCapacity})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (editingProduct != null) {
        val p = editingProduct!!
        var priceInput by remember(p) { mutableStateOf(p.priceUgx.toString()) }
        var thresholdInput by remember(p) { mutableStateOf(p.lowStockThreshold.toString()) }
        Dialog(onDismissRequest = { editingProduct = null }) {
            Surface(shape = RoundedCornerShape(20.dp), color = EspressoSurface, border = BorderStroke(1.dp, EspressoBorder)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Edit ${p.slotCode} (${p.name})", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Unit Price (UGX)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = thresholdInput,
                        onValueChange = { thresholdInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Low Stock Alert Threshold") },
                        singleLine = true
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editingProduct = null }) { Text("Cancel") }
                        Button(onClick = {
                            onEditConfig(
                                p,
                                priceInput.toIntOrNull() ?: p.priceUgx,
                                thresholdInput.toIntOrNull() ?: p.lowStockThreshold
                            )
                            editingProduct = null
                        }) { Text("Save") }
                    }
                }
            }
        }
    }

    if (showAddSlotDialog) {
        var slotCode by remember { mutableStateOf("B1") }
        var name by remember { mutableStateOf("Mango Yogurt") }
        var flavor by remember { mutableStateOf("Mango") }
        var price by remember { mutableStateOf("5500") }
        var cap by remember { mutableStateOf("20") }
        Dialog(onDismissRequest = { showAddSlotDialog = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = EspressoSurface, border = BorderStroke(1.dp, EspressoBorder)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add Dispenser Slot", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    OutlinedTextField(value = slotCode, onValueChange = { slotCode = it }, label = { Text("Slot Code") }, singleLine = true)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, singleLine = true)
                    OutlinedTextField(value = flavor, onValueChange = { flavor = it }, label = { Text("Flavor Tag") }, singleLine = true)
                    OutlinedTextField(value = price, onValueChange = { price = it.filter { c -> c.isDigit() } }, label = { Text("Price (UGX)") }, singleLine = true)
                    OutlinedTextField(value = cap, onValueChange = { cap = it.filter { c -> c.isDigit() } }, label = { Text("Capacity") }, singleLine = true)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddSlotDialog = false }) { Text("Cancel") }
                        Button(onClick = {
                            if (slotCode.isNotBlank() && name.isNotBlank()) {
                                onAddSlot(slotCode, name, flavor, price.toIntOrNull() ?: 5000, cap.toIntOrNull() ?: 20)
                                showAddSlotDialog = false
                            }
                        }) { Text("Create Slot") }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountsTab(
    accounts: List<AccountEntity>,
    onTopUp: (AccountEntity, Int) -> Unit,
    onToggleActive: (AccountEntity) -> Unit,
    onCreateAccount: (String, String, Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    var selectedQrAccount by remember { mutableStateOf<AccountEntity?>(null) }

    val filtered = accounts.filter {
        query.isBlank() ||
            it.holderName.contains(query, true) ||
            it.id.contains(query, true) ||
            it.email.contains(query, true)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search member ID (SV-78201)...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TelemetryCyan,
                        unfocusedBorderColor = EspressoBorder
                    ),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { showCreate = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TelemetryCyan, contentColor = EspressoBg)
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = "Add Account")
                }
            }
        }

        items(filtered, key = { it.id }) { acc ->
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${acc.holderName} (${acc.id})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(acc.email, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (acc.isActive) DispenseEmerald.copy(alpha = 0.16f) else ErrorRose.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = if (acc.isActive) "ACTIVE" else "SUSPENDED",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (acc.isActive) DispenseEmerald else ErrorRose,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatUgx(acc.balanceUgx),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TelemetryCyan
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onTopUp(acc, 10000) },
                            colors = ButtonDefaults.buttonColors(containerColor = DispenseEmerald, contentColor = EspressoBg),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+10k UGX", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { selectedQrAccount = acc },
                            border = BorderStroke(1.dp, TelemetryCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.QrCode2, contentDescription = null, tint = TelemetryCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("QR Card", color = TelemetryCyan, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onToggleActive(acc) },
                            border = BorderStroke(1.dp, if (acc.isActive) ErrorRose else DispenseEmerald)
                        ) {
                            Text(if (acc.isActive) "Pause" else "Enable", color = if (acc.isActive) ErrorRose else DispenseEmerald, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (selectedQrAccount != null) {
        val acc = selectedQrAccount!!
        val bmp = remember(acc.qrPayload) { QrCodeGenerator.generateQrBitmap(acc.qrPayload, 380) }
        Dialog(onDismissRequest = { selectedQrAccount = null }) {
            Surface(shape = RoundedCornerShape(22.dp), color = EspressoSurface, border = BorderStroke(1.dp, TelemetryCyan)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(acc.holderName, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text("${acc.id} • ${formatUgx(acc.balanceUgx)}", style = MaterialTheme.typography.labelMedium, color = TelemetryCyan)
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                        if (bmp != null) {
                            Image(bitmap = bmp, contentDescription = "Member QR", modifier = Modifier.size(200.dp).padding(10.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(acc.qrPayload, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { selectedQrAccount = null }) { Text("Done") }
                }
            }
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var balance by remember { mutableStateOf("25000") }
        Dialog(onDismissRequest = { showCreate = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = EspressoSurface, border = BorderStroke(1.dp, EspressoBorder)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("New Member QR Account", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, singleLine = true)
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true)
                    OutlinedTextField(value = balance, onValueChange = { balance = it.filter { c -> c.isDigit() } }, label = { Text("Initial Deposit (UGX)") }, singleLine = true)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCreate = false }) { Text("Cancel") }
                        Button(onClick = {
                            if (name.isNotBlank()) {
                                onCreateAccount(name, email.ifBlank { "member@kdis.ug" }, balance.toIntOrNull() ?: 20000)
                                showCreate = false
                            }
                        }) { Text("Create Account") }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionsTab(
    transactions: List<TransactionEntity>,
    decodeItems: (String) -> List<OrderLineItem>,
    onViewReceipt: (TransactionEntity) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = transactions.filter {
        search.isBlank() ||
            it.orderRef.contains(search, true) ||
            it.paymentMethod.contains(search, true) ||
            it.transportUsed.contains(search, true)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Filter by order ref, payment, or transport...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StrawberryPink,
                    unfocusedBorderColor = EspressoBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(filtered, key = { it.id }) { txn ->
            val items = remember(txn.itemsJson) { decodeItems(txn.itemsJson) }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = EspressoSurface,
                border = BorderStroke(1.dp, EspressoBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewReceipt(txn) }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(txn.orderRef, style = MaterialTheme.typography.labelMedium, color = TelemetryCyan)
                        Text(
                            text = formatUgx(txn.totalUgx),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = DispenseEmerald
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = items.joinToString(", ") { "${it.quantity}x ${it.name}" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${txn.paymentMethod} • ${txn.transportUsed} • ${formatTimestamp(txn.timestamp)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (txn.syncedToSheets) {
                                Icon(Icons.Filled.CloudDone, contentDescription = "Synced", tint = DispenseEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Icon(Icons.Filled.Receipt, contentDescription = "View Receipt", tint = StrawberryPink, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleSheetsTab(
    preferences: KioskPreferences,
    isAuthorized: Boolean,
    statusMessage: String,
    unsyncedCount: Int,
    onSaveConfig: (String, String) -> Unit,
    onAuthorize: (Activity, (android.content.IntentSender) -> Unit) -> Unit,
    onAuthIntentResult: (android.content.Intent?) -> Unit,
    onSyncNow: () -> Unit
) {
    val context = LocalContext.current
    var sheetId by remember(preferences.googleSheetId) { mutableStateOf(preferences.googleSheetId) }
    var sheetName by remember(preferences.googleSheetName) { mutableStateOf(preferences.googleSheetName) }

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        onAuthIntentResult(result.data)
    }

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
                        Icon(Icons.Filled.CloudSync, contentDescription = null, tint = TelemetryCyan)
                        Text("Google Sheets Sync (${KioskConstants.MACHINE_ID})", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    }
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isAuthorized) DispenseEmerald else VanillaAmber
                    )

                    OutlinedTextField(
                        value = sheetId,
                        onValueChange = { sheetId = it },
                        label = { Text("Google Spreadsheet ID") },
                        placeholder = { Text("1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("google_sheet_id_input")
                    )
                    OutlinedTextField(
                        value = sheetName,
                        onValueChange = { sheetName = it },
                        label = { Text("Sheet Tab Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onSaveConfig(sheetId, sheetName) },
                            border = BorderStroke(1.dp, TelemetryCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Sheet ID", color = TelemetryCyan)
                        }
                        Button(
                            onClick = {
                                val act = context as? Activity
                                if (act != null) {
                                    onAuthorize(act) { sender ->
                                        authLauncher.launch(IntentSenderRequest.Builder(sender).build())
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StrawberryPink, contentColor = TextPrimary),
                            modifier = Modifier.weight(1f).testTag("authorize_google_sheets_button")
                        ) {
                            Text(if (isAuthorized) "Re-Authorize" else "Sign In & Auth")
                        }
                    }

                    Button(
                        onClick = onSyncNow,
                        colors = ButtonDefaults.buttonColors(containerColor = DispenseEmerald, contentColor = EspressoBg),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sync_sheets_now_button")
                    ) {
                        Text("Append $unsyncedCount Unsynced Order(s) to Sheet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
