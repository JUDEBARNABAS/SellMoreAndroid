package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val slotCode: String,
    val name: String,
    val subtitle: String,
    val priceUgx: Int,
    val calories: Int,
    val probioticsBillion: Int,
    val stock: Int,
    val maxCapacity: Int,
    val lowStockThreshold: Int,
    val badge: String,
    val flavorTag: String,
    val drawableKey: String
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val holderName: String,
    val email: String,
    val balanceUgx: Int,
    val qrPayload: String,
    val isActive: Boolean,
    val createdAt: Long
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val orderRef: String,
    val itemsJson: String,
    val subtotalUgx: Int,
    val vatUgx: Int,
    val totalUgx: Int,
    val paymentMethod: String,
    val accountId: String?,
    val remainingBalanceUgx: Int?,
    val transportUsed: String,
    val syncedToSheets: Boolean,
    val timestamp: Long
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val action: String,
    val details: String,
    val severity: String,
    val timestamp: Long
)
