package com.mosque.prayerclock.server

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Hasil panggilan ke server SIWAK. */
sealed class ApiResult<out T> {
    data class Ok<T>(val data: T) : ApiResult<T>()

    /** Server menjawab dengan galat (mis. 401 `device_unknown`, 404 `weather_unavailable`). */
    data class HttpError(val status: Int, val code: String?, val message: String?) : ApiResult<Nothing>()

    /** Tidak bisa menghubungi server (internet putus, DNS, waktu habis). */
    data class NetworkError(val message: String) : ApiResult<Nothing>()
}

/**
 * Klien rute `/api/v1` (TRD §12.1). Alamat server bisa diubah pengguna, jadi memakai OkHttp
 * langsung dengan URL dasar per panggilan, bukan Retrofit dengan base URL tetap.
 */
@Singleton
class SiwakApi
    @Inject
    constructor() {
        private val gson = Gson()
        private val json = "application/json; charset=utf-8".toMediaType()
        private val client =
            OkHttpClient
                .Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

        suspend fun startPairing(
            baseUrl: String,
            body: PairingStartRequest,
        ) = call<PairingStartResponse>(baseUrl, "POST", "api/v1/pairing", body, null)

        suspend fun pairingStatus(
            baseUrl: String,
            pairingId: String,
            nonce: String,
        ) = call<PairingStatusResponse>(baseUrl, "GET", "api/v1/pairing/$pairingId", null, null, mapOf("nonce" to nonce))

        suspend fun heartbeat(
            baseUrl: String,
            token: String,
            body: HeartbeatRequest,
        ) = call<HeartbeatResponse>(baseUrl, "POST", "api/v1/device/heartbeat", body, token)

        suspend fun bundle(
            baseUrl: String,
            token: String,
        ) = call<BundleDto>(baseUrl, "GET", "api/v1/device/bundle", null, token)

        /** Cuaca mentah (JSON) agar bisa disimpan apa adanya untuk mode offline. */
        suspend fun weatherJson(
            baseUrl: String,
            token: String,
        ) = call<String>(baseUrl, "GET", "api/v1/device/weather", null, token)

        suspend fun unpair(
            baseUrl: String,
            token: String,
        ) = call<Map<String, Any>>(baseUrl, "POST", "api/v1/device/unpair", emptyMap<String, String>(), token)

        private suspend inline fun <reified T> call(
            baseUrl: String,
            method: String,
            path: String,
            body: Any?,
            token: String?,
            query: Map<String, String> = emptyMap(),
        ): ApiResult<T> =
            withContext(Dispatchers.IO) {
                val base = baseUrl.trimEnd('/').plus("/").toHttpUrlOrNull()
                    ?: return@withContext ApiResult.NetworkError("Alamat server tidak sah")
                val url =
                    base
                        .newBuilder()
                        .addPathSegments(path)
                        .apply { query.forEach { (k, v) -> addQueryParameter(k, v) } }
                        .build()
                val request =
                    Request
                        .Builder()
                        .url(url)
                        .header("Accept", "application/json")
                        .apply {
                            if (token != null) header("Authorization", "Device $token")
                            if (method == "POST") post(gson.toJson(body ?: emptyMap<String, String>()).toRequestBody(json)) else get()
                        }.build()
                try {
                    client.newCall(request).execute().use { res ->
                        val text = res.body?.string().orEmpty()
                        if (!res.isSuccessful) {
                            val err =
                                runCatching { gson.fromJson(text, JsonObject::class.java).getAsJsonObject("error") }.getOrNull()
                            return@withContext ApiResult.HttpError(
                                res.code,
                                err?.get("code")?.asString,
                                err?.get("message")?.asString,
                            )
                        }
                        @Suppress("UNCHECKED_CAST")
                        val data: T = if (T::class == String::class) text as T else gson.fromJson(text, T::class.java)
                        ApiResult.Ok(data)
                    }
                } catch (e: IOException) {
                    ApiResult.NetworkError(e.message ?: e.javaClass.simpleName)
                } catch (e: RuntimeException) {
                    // Jawaban bukan JSON yang diharapkan (mis. halaman galat proxy).
                    ApiResult.NetworkError("Jawaban server tidak dikenali: ${e.message}")
                }
            }
    }
