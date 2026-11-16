package com.mosque.prayerclock.server

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Konten tayang dari server: slide, hadits, panel informasi, gambar (Backend Schema §5.1). */
data class ServerContent(
    val slides: List<SlideDto> = emptyList(),
    val content: List<ContentDto> = emptyList(),
    /** id media → berkas lokal yang sudah diverifikasi sha256-nya. */
    val mediaFiles: Map<String, String> = emptyMap(),
) {
    fun contentById(id: String?): ContentDto? = content.firstOrNull { it.id == id }

    val hadiths: List<ContentDto> get() = content.filter { it.type == "hadith" && !it.translation.isNullOrBlank() }

    fun mediaUri(id: String?): String? = id?.let { mediaFiles[it] }
}

/**
 * Menyimpan konten server di berkas JSON (ditulis atomik) dan media di `filesDir/media/<sha256>`.
 * Tidak memakai tabel Room agar pembaruan aplikasi tidak butuh migrasi basis data di TV lama.
 */
@Singleton
class ServerContentRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val gson = Gson()
        private val file = File(context.filesDir, "server-content.json")
        private val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
        private val _content = MutableStateFlow(load())
        val content: StateFlow<ServerContent> = _content.asStateFlow()

        private val http =
            OkHttpClient
                .Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

        private fun load(): ServerContent =
            runCatching { gson.fromJson(file.readText(), ServerContent::class.java) }.getOrNull() ?: ServerContent()

        fun mediaUriFor(sha256: String): String = Uri.fromFile(File(mediaDir, sha256)).toString()

        /**
         * Mengunduh semua media yang belum ada (TRD §7.2). Berkas yang rusak (ukuran atau sha256 tidak cocok)
         * diunduh ulang. Mengembalikan peta id → URI lokal, atau null bila ada yang gagal (bundle lama dipakai).
         */
        suspend fun syncMedia(
            serverUrl: String,
            token: String,
            refs: List<MediaRefDto>,
        ): Map<String, String>? =
            withContext(Dispatchers.IO) {
                val result = mutableMapOf<String, String>()
                for (m in refs) {
                    val id = m.id ?: continue
                    val sha = m.sha256?.lowercase()?.takeIf { it.matches(Regex("^[0-9a-f]{64}$")) } ?: continue
                    val target = File(mediaDir, sha)
                    if (!(target.exists() && target.length() == m.bytes && sha256(target) == sha)) {
                        if (!download(serverUrl, token, id, sha, target)) return@withContext null
                    }
                    result[id] = Uri.fromFile(target).toString()
                }
                result
            }

        private fun download(
            serverUrl: String,
            token: String,
            id: String,
            sha: String,
            target: File,
        ): Boolean {
            val tmp = File(mediaDir, "$sha.part")
            return try {
                val req =
                    Request
                        .Builder()
                        .url("${serverUrl.trimEnd('/')}/api/v1/device/media/$id")
                        .header("Authorization", "Device $token")
                        .build()
                http.newCall(req).execute().use { res ->
                    if (!res.isSuccessful) {
                        Log.w(TAG, "media $id ditolak: ${res.code}")
                        return false
                    }
                    res.body!!.byteStream().use { input -> tmp.outputStream().use { input.copyTo(it) } }
                }
                if (sha256(tmp) != sha) {
                    Log.w(TAG, "media $id rusak (sha256 tidak cocok), dibuang")
                    tmp.delete()
                    return false
                }
                tmp.renameTo(target)
            } catch (e: Exception) {
                Log.w(TAG, "gagal mengunduh media $id: ${e.message}")
                tmp.delete()
                false
            }
        }

        /** Menyimpan konten baru dan membuang berkas media yang tidak lagi dirujuk. */
        suspend fun save(
            bundle: BundleDto,
            mediaFiles: Map<String, String>,
        ) = withContext(Dispatchers.IO) {
            val next = ServerContent(bundle.slides.orEmpty(), bundle.content.orEmpty(), mediaFiles)
            val tmp = File(context.filesDir, "server-content.json.tmp")
            tmp.writeText(gson.toJson(next))
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
            _content.value = next
            val keep = mediaFiles.values.mapNotNull { Uri.parse(it).lastPathSegment }.toSet()
            mediaDir.listFiles()?.forEach { f -> if (f.name !in keep) f.delete() }
        }

        /** Saat TV dilepas dari server: konten server tidak lagi ditayangkan. */
        suspend fun clear() =
            withContext(Dispatchers.IO) {
                file.delete()
                _content.value = ServerContent()
            }

        private fun sha256(f: File): String {
            val md = MessageDigest.getInstance("SHA-256")
            f.inputStream().use { input ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    md.update(buf, 0, n)
                }
            }
            return md.digest().joinToString("") { "%02x".format(it) }
        }

        companion object {
            private const val TAG = "SiwakContent"
        }
    }
