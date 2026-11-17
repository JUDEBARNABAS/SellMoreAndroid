package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.KioskConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.kioskDataStore: DataStore<Preferences> by preferencesDataStore(name = "sellmore_kiosk_prefs")

data class KioskPreferences(
    val esp32WifiIp: String = KioskConstants.DEFAULT_ESP32_IP,
    val esp32SimulationMode: Boolean = false,
    val autoDispenseOnPayment: Boolean = true,
    val preferredTransport: String = "AUTO",
    val googleSheetId: String = "",
    val googleSheetName: String = "Transactions",
    val googleAccountEmail: String = "",
    val adminPin: String = KioskConstants.DEFAULT_ADMIN_PIN
)

class KioskPreferencesRepository(private val context: Context) {
    private object Keys {
        val ESP32_WIFI_IP = stringPreferencesKey("esp32_wifi_ip")
        val ESP32_SIM_MODE = booleanPreferencesKey("esp32_sim_mode")
        val AUTO_DISPENSE = booleanPreferencesKey("auto_dispense_on_payment")
        val PREFERRED_TRANSPORT = stringPreferencesKey("preferred_transport")
        val GOOGLE_SHEET_ID = stringPreferencesKey("google_sheet_id")
        val GOOGLE_SHEET_NAME = stringPreferencesKey("google_sheet_name")
        val GOOGLE_ACCOUNT_EMAIL = stringPreferencesKey("google_account_email")
        val ADMIN_PIN = stringPreferencesKey("admin_pin")
    }

    val preferencesFlow: Flow<KioskPreferences> = context.kioskDataStore.data.map { prefs ->
        KioskPreferences(
            esp32WifiIp = prefs[Keys.ESP32_WIFI_IP] ?: KioskConstants.DEFAULT_ESP32_IP,
            esp32SimulationMode = prefs[Keys.ESP32_SIM_MODE] ?: false,
            autoDispenseOnPayment = prefs[Keys.AUTO_DISPENSE] ?: true,
            preferredTransport = prefs[Keys.PREFERRED_TRANSPORT] ?: "AUTO",
            googleSheetId = prefs[Keys.GOOGLE_SHEET_ID] ?: "",
            googleSheetName = prefs[Keys.GOOGLE_SHEET_NAME] ?: "Transactions",
            googleAccountEmail = prefs[Keys.GOOGLE_ACCOUNT_EMAIL] ?: "",
            adminPin = prefs[Keys.ADMIN_PIN] ?: KioskConstants.DEFAULT_ADMIN_PIN
        )
    }

    suspend fun setEsp32WifiIp(ip: String) {
        context.kioskDataStore.edit { it[Keys.ESP32_WIFI_IP] = ip.trim() }
    }

    suspend fun setEsp32SimulationMode(enabled: Boolean) {
        context.kioskDataStore.edit { it[Keys.ESP32_SIM_MODE] = enabled }
    }

    suspend fun setAutoDispenseOnPayment(enabled: Boolean) {
        context.kioskDataStore.edit { it[Keys.AUTO_DISPENSE] = enabled }
    }

    suspend fun setPreferredTransport(transport: String) {
        context.kioskDataStore.edit { it[Keys.PREFERRED_TRANSPORT] = transport }
    }

    suspend fun setGoogleSheetConfig(sheetId: String, sheetName: String) {
        context.kioskDataStore.edit {
            it[Keys.GOOGLE_SHEET_ID] = sheetId.trim()
            it[Keys.GOOGLE_SHEET_NAME] = sheetName.trim().ifEmpty { "Transactions" }
        }
    }

    suspend fun setGoogleAccountEmail(email: String) {
        context.kioskDataStore.edit { it[Keys.GOOGLE_ACCOUNT_EMAIL] = email }
    }
}
