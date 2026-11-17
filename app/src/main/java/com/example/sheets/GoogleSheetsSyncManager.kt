package com.example.sheets

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import com.example.data.local.TransactionEntity
import com.example.model.KioskConstants
import com.example.model.OrderLineItem
import com.example.model.formatTimestamp
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

sealed interface SheetsAuthOutcome {
    data class Authorized(val accessToken: String) : SheetsAuthOutcome
    data class NeedsResolution(val intentSender: IntentSender) : SheetsAuthOutcome
    data class Failed(val reason: String) : SheetsAuthOutcome
}

class GoogleSheetsSyncManager(private val context: Context) {
    companion object {
        const val SHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun requestSheetsAuthorization(activity: Activity): SheetsAuthOutcome {
        return suspendCancellableCoroutine { cont ->
            val authRequest = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(SHEETS_SCOPE)))
                .build()
            Identity.getAuthorizationClient(activity)
                .authorize(authRequest)
                .addOnSuccessListener { result ->
                    if (result.hasResolution()) {
                        val pending = result.pendingIntent
                        if (pending != null) {
                            cont.resume(SheetsAuthOutcome.NeedsResolution(pending.intentSender))
                        } else {
                            cont.resume(SheetsAuthOutcome.Failed("Authorization requires resolution but PendingIntent was null."))
                        }
                    } else {
                        val token = result.accessToken
                        if (!token.isNullOrBlank()) {
                            cont.resume(SheetsAuthOutcome.Authorized(token))
                        } else {
                            cont.resume(SheetsAuthOutcome.Failed("No access token returned by Google AuthorizationClient."))
                        }
                    }
                }
                .addOnFailureListener { e ->
                    cont.resume(SheetsAuthOutcome.Failed(e.message ?: "Google authorization failed"))
                }
        }
    }

    fun extractTokenFromResult(intent: android.content.Intent?): Result<String> {
        return runCatching {
            val authResult: AuthorizationResult = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(intent)
            authResult.accessToken ?: error("Access token missing after consent")
        }
    }

    suspend fun verifySpreadsheet(accessToken: String, spreadsheetId: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("https://sheets.googleapis.com/v4/spreadsheets/${spreadsheetId.trim()}?fields=properties.title")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        error("Sheets API HTTP ${resp.code}: $body")
                    }
                    val title = JSONObject(body).optJSONObject("properties")?.optString("title")
                    title ?: spreadsheetId
                }
            }
        }

    suspend fun appendTransactions(
        accessToken: String,
        spreadsheetId: String,
        sheetName: String,
        rows: List<Pair<TransactionEntity, List<OrderLineItem>>>
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            if (rows.isEmpty()) return@runCatching 0
            val valuesArray = JSONArray()
            rows.forEach { (txn, items) ->
                val itemsText = items.joinToString(", ") { "${it.quantity}x ${it.name} [${it.slotCode}]" }
                val row = JSONArray().apply {
                    put(txn.orderRef)
                    put(KioskConstants.MACHINE_ID)
                    put(formatTimestamp(txn.timestamp, "yyyy-MM-dd HH:mm:ss"))
                    put(itemsText)
                    put(txn.subtotalUgx)
                    put(txn.vatUgx)
                    put(txn.totalUgx)
                    put(txn.paymentMethod)
                    put(txn.accountId ?: "GUEST")
                    put(txn.transportUsed)
                }
                valuesArray.put(row)
            }
            val payload = JSONObject().put("values", valuesArray).toString()
            val range = "${sheetName.ifBlank { "Transactions" }}!A1"
            val url =
                "https://sheets.googleapis.com/v4/spreadsheets/${spreadsheetId.trim()}/values/$range:append?valueInputOption=USER_ENTERED"
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    error("Sheets append HTTP ${resp.code}: $body")
                }
                rows.size
            }
        }
    }
}
