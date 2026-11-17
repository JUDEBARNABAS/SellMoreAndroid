package com.example.data.repository

import com.example.data.local.AccountEntity
import com.example.data.local.AuditLogEntity
import com.example.data.local.ProductEntity
import com.example.data.local.SellMoreDatabase
import com.example.data.local.TransactionEntity
import com.example.model.OrderLineItem
import com.example.model.calculateInclusiveVatUgx
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SellMoreRepository(private val db: SellMoreDatabase) {
    private val json = Json { ignoreUnknownKeys = true }

    val products: Flow<List<ProductEntity>> = db.productDao().observeAll()
    val accounts: Flow<List<AccountEntity>> = db.accountDao().observeAll()
    val transactions: Flow<List<TransactionEntity>> = db.transactionDao().observeAll()
    val auditLogs: Flow<List<AuditLogEntity>> = db.auditLogDao().observeRecent()

    suspend fun ensureSeeded() {
        if (db.productDao().getAllOnce().isEmpty()) {
            val now = System.currentTimeMillis()
            db.productDao().insertAll(
                listOf(
                    ProductEntity(
                        id = "prod_a1",
                        slotCode = "A1",
                        name = "Strawberry Yogurt",
                        subtitle = "330ml Sealed Bottle",
                        priceUgx = 5000,
                        calories = 190,
                        probioticsBillion = 12,
                        stock = 15,
                        maxCapacity = 20,
                        lowStockThreshold = 5,
                        badge = "Popular",
                        flavorTag = "Strawberry",
                        drawableKey = "strawberry"
                    ),
                    ProductEntity(
                        id = "prod_a2",
                        slotCode = "A2",
                        name = "Vanilla Yogurt",
                        subtitle = "330ml Sealed Bottle",
                        priceUgx = 5000,
                        calories = 180,
                        probioticsBillion = 15,
                        stock = 14,
                        maxCapacity = 20,
                        lowStockThreshold = 5,
                        badge = "Classic",
                        flavorTag = "Vanilla",
                        drawableKey = "vanilla"
                    ),
                    ProductEntity(
                        id = "prod_a3",
                        slotCode = "A3",
                        name = "Chocolate Yogurt",
                        subtitle = "330ml Sealed Bottle",
                        priceUgx = 5500,
                        calories = 220,
                        probioticsBillion = 10,
                        stock = 12,
                        maxCapacity = 20,
                        lowStockThreshold = 5,
                        badge = "Gourmet",
                        flavorTag = "Chocolate",
                        drawableKey = "chocolate"
                    )
                )
            )

            db.accountDao().insertAll(
                listOf(
                    AccountEntity(
                        id = "SV-78201",
                        holderName = "Alex Rivera",
                        email = "alex.rivera@kdis.ug",
                        balanceUgx = 85000,
                        qrPayload = "SV_ACC:SV-78201:SEC_KEY_AR92",
                        isActive = true,
                        createdAt = now - 86400_000L * 14
                    ),
                    AccountEntity(
                        id = "SV-44192",
                        holderName = "Elena Rostova",
                        email = "elena.rostova@kdis.ug",
                        balanceUgx = 42500,
                        qrPayload = "SV_ACC:SV-44192:SEC_KEY_ER44",
                        isActive = true,
                        createdAt = now - 86400_000L * 10
                    ),
                    AccountEntity(
                        id = "SV-91044",
                        holderName = "Marcus Chen",
                        email = "marcus.chen@kdis.ug",
                        balanceUgx = 120000,
                        qrPayload = "SV_ACC:SV-91044:SEC_KEY_MC91",
                        isActive = true,
                        createdAt = now - 86400_000L * 7
                    ),
                    AccountEntity(
                        id = "SV-12883",
                        holderName = "Sarah Jenkins",
                        email = "sarah.jenkins@kdis.ug",
                        balanceUgx = 28000,
                        qrPayload = "SV_ACC:SV-12883:SEC_KEY_SJ12",
                        isActive = true,
                        createdAt = now - 86400_000L * 3
                    )
                )
            )

            val sample1Items = listOf(
                OrderLineItem("prod_a1", "A1", "Strawberry Yogurt", 2, 5000)
            )
            val sample2Items = listOf(
                OrderLineItem("prod_a2", "A2", "Vanilla Yogurt", 1, 5000),
                OrderLineItem("prod_a3", "A3", "Chocolate Yogurt", 1, 5500)
            )
            val sample3Items = listOf(
                OrderLineItem("prod_a3", "A3", "Chocolate Yogurt", 2, 5500)
            )

            db.transactionDao().insertAll(
                listOf(
                    TransactionEntity(
                        id = "TXN-1001",
                        orderRef = "SM-KLA-042-1001",
                        itemsJson = json.encodeToString(sample1Items),
                        subtotalUgx = 10000,
                        vatUgx = calculateInclusiveVatUgx(10000),
                        totalUgx = 10000,
                        paymentMethod = "Member QR (SV-78201)",
                        accountId = "SV-78201",
                        remainingBalanceUgx = 85000,
                        transportUsed = "USB-OTG",
                        syncedToSheets = true,
                        timestamp = now - 3600_000L * 5
                    ),
                    TransactionEntity(
                        id = "TXN-1002",
                        orderRef = "SM-KLA-042-1002",
                        itemsJson = json.encodeToString(sample2Items),
                        subtotalUgx = 10500,
                        vatUgx = calculateInclusiveVatUgx(10500),
                        totalUgx = 10500,
                        paymentMethod = "Phone MoMo QR",
                        accountId = null,
                        remainingBalanceUgx = null,
                        transportUsed = "Wi-Fi HTTP",
                        syncedToSheets = false,
                        timestamp = now - 3600_000L * 3
                    ),
                    TransactionEntity(
                        id = "TXN-1003",
                        orderRef = "SM-KLA-042-1003",
                        itemsJson = json.encodeToString(sample3Items),
                        subtotalUgx = 11000,
                        vatUgx = calculateInclusiveVatUgx(11000),
                        totalUgx = 11000,
                        paymentMethod = "Member QR (SV-91044)",
                        accountId = "SV-91044",
                        remainingBalanceUgx = 120000,
                        transportUsed = "Bluetooth SPP",
                        syncedToSheets = false,
                        timestamp = now - 3600_000L
                    )
                )
            )

            db.auditLogDao().insertAll(
                listOf(
                    AuditLogEntity(
                        category = "SYSTEM",
                        action = "Kiosk Booted",
                        details = "SM-KLA-042 initialized at KDIS Innovation Hub, Level 2 - Kampala",
                        severity = "INFO",
                        timestamp = now - 7200_000L
                    ),
                    AuditLogEntity(
                        category = "HARDWARE",
                        action = "Servo GPIO 18 Calibrated",
                        details = "Home position set to 0 deg, dispense angle 90 deg (hold 2000ms)",
                        severity = "SUCCESS",
                        timestamp = now - 7100_000L
                    ),
                    AuditLogEntity(
                        category = "INVENTORY",
                        action = "Slot Audit Verified",
                        details = "A1: 15/20, A2: 14/20, A3: 12/20 330ml bottles chilled at 4.2°C",
                        severity = "INFO",
                        timestamp = now - 7000_000L
                    )
                )
            )
        }
    }

    fun decodeItems(itemsJson: String): List<OrderLineItem> {
        return runCatching { json.decodeFromString<List<OrderLineItem>>(itemsJson) }
            .getOrDefault(emptyList())
    }

    fun encodeItems(items: List<OrderLineItem>): String = json.encodeToString(items)

    suspend fun findAccountByQrOrId(payload: String): AccountEntity? {
        val trimmed = payload.trim()
        val exact = db.accountDao().findByQrOrId(trimmed)
        if (exact != null) return exact
        if (trimmed.startsWith("SV_ACC:")) {
            val parts = trimmed.split(":")
            if (parts.size >= 2) {
                return db.accountDao().getById(parts[1])
            }
        }
        return null
    }

    suspend fun recordOrderAndDecrementStock(
        orderRef: String,
        lineItems: List<OrderLineItem>,
        totalUgx: Int,
        paymentMethod: String,
        accountId: String?,
        remainingBalanceUgx: Int?,
        transportUsed: String
    ): TransactionEntity {
        if (accountId != null && remainingBalanceUgx != null) {
            val acc = db.accountDao().getById(accountId)
            if (acc != null) {
                db.accountDao().upsert(acc.copy(balanceUgx = remainingBalanceUgx))
            }
        }

        for (item in lineItems) {
            val product = db.productDao().getById(item.productId)
            if (product != null) {
                val updatedStock = (product.stock - item.quantity).coerceAtLeast(0)
                db.productDao().update(product.copy(stock = updatedStock))
            }
        }

        val txn = TransactionEntity(
            id = "TXN-${System.currentTimeMillis() % 100000}",
            orderRef = orderRef,
            itemsJson = json.encodeToString(lineItems),
            subtotalUgx = totalUgx,
            vatUgx = calculateInclusiveVatUgx(totalUgx),
            totalUgx = totalUgx,
            paymentMethod = paymentMethod,
            accountId = accountId,
            remainingBalanceUgx = remainingBalanceUgx,
            transportUsed = transportUsed,
            syncedToSheets = false,
            timestamp = System.currentTimeMillis()
        )
        db.transactionDao().insert(txn)
        logAudit(
            category = "PAYMENT",
            action = "Order Completed ($orderRef)",
            details = "$paymentMethod • ${lineItems.sumOf { it.quantity }} bottle(s) • UGX $totalUgx via $transportUsed",
            severity = "SUCCESS"
        )
        return txn
    }

    suspend fun updateProduct(product: ProductEntity) {
        db.productDao().upsert(product)
    }

    suspend fun upsertAccount(account: AccountEntity) {
        db.accountDao().upsert(account)
    }

    suspend fun markTransactionsSynced(ids: List<String>) {
        if (ids.isNotEmpty()) {
            db.transactionDao().markSynced(ids)
        }
    }

    suspend fun logAudit(category: String, action: String, details: String, severity: String = "INFO") {
        db.auditLogDao().insert(
            AuditLogEntity(
                category = category,
                action = action,
                details = details,
                severity = severity,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearAuditLogs() {
        db.auditLogDao().clearAll()
    }
}
