package com.mosque.prayerclock.data.service

import android.util.Log
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import javax.inject.Inject
import javax.inject.Singleton

data class GitHubReleaseAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val browserDownloadUrl: String,
)

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String,
    @SerializedName("body") val body: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("published_at") val publishedAt: String,
    @SerializedName("prerelease") val prerelease: Boolean,
    @SerializedName("assets") val assets: List<GitHubReleaseAsset>,
)

interface GitHubApi {
    @GET("repos/alfian-yds/siwak/releases/latest")
    suspend fun getLatestRelease(): Response<GitHubRelease>
}

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val downloadUrl: String,
    val releaseNotes: String,
)

@Singleton
class UpdateChecker
    @Inject
    constructor() {
        private val githubApi: GitHubApi by lazy {
            Retrofit
                .Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GitHubApi::class.java)
        }

        // SIWAK: repositori GitHub untuk auto-update
        private val repoOwner = "alfian-yds"
        private val repoName = "siwak"

        suspend fun checkForUpdates(currentVersion: String): UpdateInfo? =
            try {
                Log.d("UpdateChecker", "Checking for updates. Current version: $currentVersion")
                val response = githubApi.getLatestRelease()

                if (response.isSuccessful && response.body() != null) {
                    val release = response.body()!!
                    val latestVersion = release.tagName.removePrefix("v")

                    Log.d("UpdateChecker", "Latest version from GitHub: $latestVersion")

                    val hasUpdate = compareVersions(latestVersion, currentVersion) > 0

                    // Find the APK asset (MosqueClock-v*.apk)
                    val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                    val downloadUrl = apkAsset?.browserDownloadUrl ?: release.htmlUrl

                    Log.d("UpdateChecker", "APK download URL: $downloadUrl")

                    UpdateInfo(
                        hasUpdate = hasUpdate,
                        latestVersion = latestVersion,
                        currentVersion = currentVersion,
                        downloadUrl = downloadUrl,
                        releaseNotes = release.body,
                    )
                } else {
                    Log.e("UpdateChecker", "Failed to check for updates: ${response.code()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("UpdateChecker", "Error checking for updates", e)
                null
            }

        /**
         * Compare two version strings
         * Returns: 1 if v1 > v2, -1 if v1 < v2, 0 if equal
         */
        private fun compareVersions(
            v1: String,
            v2: String,
        ): Int = compareAppVersions(v1, v2)
    }

/**
 * Membandingkan versi aplikasi ala SemVer: 1 bila [v1] lebih baru, -1 bila lebih lama, 0 bila sama.
 * Bagian angka dibandingkan dulu ("1.3" < "2.0.0"); bila sama, versi tanpa akhiran lebih baru
 * daripada pra-rilis ("2.0.0-rc.1" < "2.0.0"), dan pra-rilis dibandingkan per bagian ("rc.1" < "rc.2").
 * Akhiran build debug ("-DEBUG") diperlakukan sebagai pra-rilis.
 */
fun compareAppVersions(
    v1: String,
    v2: String,
): Int {
    fun split(v: String): Pair<List<Int>, String?> {
        val clean = v.trim().removePrefix("v")
        val dash = clean.indexOf('-')
        val core = if (dash >= 0) clean.substring(0, dash) else clean
        val pre = if (dash >= 0) clean.substring(dash + 1).ifBlank { null } else null
        return core.split(".").map { it.toIntOrNull() ?: 0 } to pre
    }
    val (n1, p1) = split(v1)
    val (n2, p2) = split(v2)
    for (i in 0 until maxOf(n1.size, n2.size)) {
        val a = n1.getOrNull(i) ?: 0
        val b = n2.getOrNull(i) ?: 0
        if (a != b) return if (a > b) 1 else -1
    }
    if (p1 == p2) return 0
    if (p1 == null) return 1
    if (p2 == null) return -1
    val a = p1.split(".", "-")
    val b = p2.split(".", "-")
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrNull(i) ?: return -1
        val y = b.getOrNull(i) ?: return 1
        val xi = x.toIntOrNull()
        val yi = y.toIntOrNull()
        val c =
            when {
                xi != null && yi != null -> xi.compareTo(yi)
                xi != null -> -1
                yi != null -> 1
                else -> x.compareTo(y, ignoreCase = true)
            }
        if (c != 0) return if (c > 0) 1 else -1
    }
    return 0
}
