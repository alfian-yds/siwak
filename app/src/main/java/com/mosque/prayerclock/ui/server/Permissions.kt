package com.mosque.prayerclock.ui.server

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.mosque.prayerclock.ui.tv.Button
import com.mosque.prayerclock.ui.tv.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mosque.prayerclock.utils.PermissionsHelper

/** Satu izin yang dibutuhkan agar SIWAK berjalan lancar di TV (mode mandiri maupun terhubung). */
data class SiwakPermission(
    val title: String,
    val why: String,
    val required: Boolean,
    val granted: Boolean,
    val action: String,
    /** Izin runtime yang diminta langsung lewat dialog Android; null = dibuka lewat layar setelan. */
    val runtime: String? = null,
)

/**
 * Daftar izin SIWAK yang sama untuk mode mandiri dan mode terhubung, supaya pengurus melihat hal yang sama
 * di kedua mode. "Wajib" = dibutuhkan agar layar selalu tampil dan perintah dari HP sampai ke TV.
 */
fun siwakPermissions(context: Context): List<SiwakPermission> {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val list = mutableListOf<SiwakPermission>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        list +=
            SiwakPermission(
                "Tampil di atas aplikasi lain",
                "Agar SIWAK bisa muncul lagi otomatis saat tertutup aplikasi lain, dan bisa dibuka dari HP.",
                required = true,
                granted = Settings.canDrawOverlays(context),
                action = "overlay",
            )
        list +=
            SiwakPermission(
                "Bebas dari penghemat baterai",
                "Agar SIWAK tetap berjalan di latar dan tidak dimatikan sistem saat TV lama menyala.",
                required = true,
                granted = pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true,
                action = "battery",
            )
    }
    list +=
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            SiwakPermission(
                "Notifikasi",
                "Android mewajibkan notifikasi kecil untuk layanan yang berjalan terus di latar.",
                required = true,
                granted =
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED,
                action = "runtime",
                runtime = Manifest.permission.POST_NOTIFICATIONS,
            )
        } else {
            SiwakPermission(
                "Notifikasi",
                "Agar layanan SIWAK di latar bisa berjalan dan memberi tanda saat ada pembaruan.",
                required = false,
                granted = PermissionsHelper.areNotificationsEnabled(context),
                action = "notif_settings",
            )
        }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        list +=
            SiwakPermission(
                "Pasang pembaruan aplikasi",
                "Agar pembaruan SIWAK bisa diunduh dan dipasang langsung dari TV.",
                required = false,
                granted = context.packageManager.canRequestPackageInstalls(),
                action = "install",
            )
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val audioPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        list +=
            SiwakPermission(
                "Akses file audio",
                "Untuk memilih rekaman adzan/iqamah sendiri dari penyimpanan TV (mode mandiri).",
                required = false,
                granted = PermissionsHelper.hasAudioPermission(context),
                action = "runtime",
                runtime = audioPerm,
            )
    }
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        list +=
            SiwakPermission(
                "Akses penyimpanan",
                "Untuk menyimpan file pembaruan SIWAK sebelum dipasang (Android 9 ke bawah).",
                required = false,
                granted = PermissionsHelper.hasStoragePermission(context),
                action = "runtime",
                runtime = Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
    }
    list +=
        SiwakPermission(
            "Jadikan layar utama TV",
            "Bila dipilih, TV langsung membuka SIWAK setiap kali dinyalakan atau tombol Home ditekan.",
            required = false,
            granted = PermissionsHelper.isDefaultLauncher(context),
            action = "launcher",
        )
    list +=
        SiwakPermission(
            "Mulai otomatis saat TV menyala",
            "SIWAK terbuka sendiri setelah TV dinyalakan atau listrik kembali.",
            required = false,
            granted = PermissionsHelper.hasBootPermission(context),
            action = "none",
        )
    return list
}

fun missingRequiredPermissions(context: Context): Boolean = siwakPermissions(context).any { it.required && !it.granted }

private fun openFor(
    context: Context,
    action: String,
) {
    val pkg = Uri.parse("package:${context.packageName}")
    val intents =
        when (action) {
            "overlay" -> listOf(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkg))
            "battery" ->
                listOf(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg),
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                )
            "install" -> listOf(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, pkg))
            "launcher" -> listOf(Intent(Settings.ACTION_HOME_SETTINGS))
            "notif_settings" ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    listOf(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                } else {
                    emptyList()
                }
            else -> emptyList()
        } + Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
    // Sebagian TV box tidak punya layar setelan tertentu; coba satu per satu, terakhir layar info aplikasi.
    for (i in intents) {
        if (runCatching { context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
    }
}

/**
 * Daftar izin dengan tombol per izin. Status diperbarui saat kembali dari layar setelan Android.
 */
@Composable
fun PermissionsList(
    modifier: Modifier = Modifier,
    firstFocus: FocusRequester? = null,
    onChanged: (List<SiwakPermission>) -> Unit = {},
) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val runtimeLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            tick++
            // Android tidak menampilkan dialog lagi bila izin pernah ditolak permanen: arahkan ke layar info aplikasi.
            if (!granted) openFor(context, "app")
        }
    val list = remember(tick) { siwakPermissions(context) }
    LaunchedEffect(list) { onChanged(list) }
    var focusAssigned = false
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        list.forEach { p ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(if (p.granted) MaterialTheme.colorScheme.primary else if (p.required) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline),
                    contentAlignment = Alignment.Center,
                ) { Text(if (p.granted) "✓" else "!", color = if (p.granted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                Column(Modifier.weight(1f)) {
                    Text(p.title + if (!p.required) " (opsional)" else "", color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(p.why, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f), fontSize = 13.sp)
                }
                if (p.granted) {
                    Text("Sudah", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                } else if (p.action == "none") {
                    Text("Tidak tersedia", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
                } else {
                    val focusMod = if (!focusAssigned && firstFocus != null) Modifier.focusRequester(firstFocus).also { focusAssigned = true } else Modifier
                    Button(
                        onClick = {
                            val perm = p.runtime
                            if (perm != null) {
                                // Bila Android tidak lagi menampilkan dialog (pernah ditolak), buka layar info aplikasi.
                                runCatching { runtimeLauncher.launch(perm) }.onFailure { openFor(context, "app") }
                            } else {
                                openFor(context, p.action)
                            }
                        },
                        modifier = focusMod,
                    ) { Text(if (p.action == "launcher") "Pilih" else "Izinkan", fontSize = 14.sp) }
                }
            }
        }
    }
}

/** Layar izin setelah TV terhubung (atau dari Pengaturan TV). */
@Composable
fun PermissionsScreen(onDone: () -> Unit) {
    val focus = remember { FocusRequester() }
    var allRequired by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.fillMaxWidth(0.86f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Tombol selesai/lewati di atas agar selalu terlihat walau daftar izinnya panjang.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Izin agar SIWAK lancar",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (allRequired == 0) {
                    Button(onClick = onDone) { Text("Selesai", fontSize = 15.sp) }
                } else {
                    OutlinedButton(onClick = onDone) { Text("Lewati dulu", fontSize = 15.sp) }
                }
            }
            Text(
                "Berikan izin berikut sekali saja. Tanpa izin wajib, layar bisa tertutup aplikasi lain dan perintah dari HP " +
                    "(buka, perbarui, mulai ulang) bisa tidak sampai ke TV. Daftar ini ada lagi di Pengaturan → Sistem.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                fontSize = 15.sp,
            )
            PermissionsList(firstFocus = focus, onChanged = { l -> allRequired = l.count { it.required && !it.granted } })
        }
    }
}


/**
 * Kartu "Izin aplikasi" di Pengaturan TV, sama persis di mode mandiri dan mode terhubung:
 * ringkasan jumlah izin, lalu daftar lengkap dengan tombol Izinkan per izin.
 */
@Composable
fun PermissionsCard(modifier: Modifier = Modifier) {
    var summary by remember { mutableStateOf("") }
    var missingRequired by remember { mutableIntStateOf(0) }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Izin aplikasi", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            if (missingRequired > 0) {
                "$summary. Ada $missingRequired izin wajib yang belum diberikan; tekan Izinkan di bawah."
            } else {
                "$summary. Semua izin wajib sudah diberikan."
            },
            color = if (missingRequired > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
        )
        PermissionsList(onChanged = { l ->
            summary = "${l.count { it.granted }} dari ${l.size} izin sudah diberikan"
            missingRequired = l.count { it.required && !it.granted }
        })
    }
}
