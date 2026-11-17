package com.example.hardware

import com.example.model.Esp32WifiStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class Esp32WifiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .writeTimeout(2500, TimeUnit.MILLISECONDS)
        .build()

    private fun normalizeBaseUrl(ipOrHost: String): String {
        val cleaned = ipOrHost.trim().removeSuffix("/")
        return if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
            cleaned
        } else {
            "http://$cleaned"
        }
    }

    suspend fun fetchStatus(ip: String): Result<Esp32WifiStatus> = withContext(Dispatchers.IO) {
        runCatching {
            val start = System.currentTimeMillis()
            val req = Request.Builder()
                .url("${normalizeBaseUrl(ip)}/api/status")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                val latency = System.currentTimeMillis() - start
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code}: $body")
                }
                val json = JSONObject(body)
                Esp32WifiStatus(
                    state = json.optString("state", "READY"),
                    servoAngle = json.optInt("servoAngle", 0),
                    servoHomeAngle = json.optInt("servoHomeAngle", 0),
                    servoActionAngle = json.optInt("servoActionAngle", 90),
                    networkMode = json.optString("networkMode", "WIFI_AP"),
                    lastEvent = json.optString("lastEvent", "OK"),
                    cycles = json.optInt("cycles", 0),
                    latencyMs = latency,
                    isReachable = true
                )
            }
        }
    }

    suspend fun triggerStart(ip: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val emptyBody = "".toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("${normalizeBaseUrl(ip)}/api/start")
                .post(emptyBody)
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code}: $body")
                }
                body.ifBlank { """{"status":"dispensing","angle":90}""" }
            }
        }
    }

    suspend fun triggerReset(ip: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val emptyBody = "".toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("${normalizeBaseUrl(ip)}/api/reset")
                .post(emptyBody)
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code}: $body")
                }
                body.ifBlank { """{"status":"reset","angle":0}""" }
            }
        }
    }
}
