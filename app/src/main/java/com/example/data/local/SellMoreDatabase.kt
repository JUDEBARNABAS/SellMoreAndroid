package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductEntity::class,
        AccountEntity::class,
        TransactionEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SellMoreDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: SellMoreDatabase? = null

        fun getInstance(context: Context): SellMoreDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SellMoreDatabase::class.java,
                    "sellmore_kiosk.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
