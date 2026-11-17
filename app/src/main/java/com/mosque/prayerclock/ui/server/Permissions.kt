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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/** Satu izin yang dibutuhkan agar SIWAK berjalan lancar di TV terhubung. */
data class SiwakPermission(
    val title: String,
    val why: String,
    val required: Boolean,
    val granted: Boolean,
    val action: String,
)

/** Daftar izin (TV terhubung): dipakai layar izin setelah terhubung dan di Pengaturan TV. */
fun siwakPermissions(context: Context): List<SiwakPermission> {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val list = mutableListOf<SiwakPermission>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        list +=
            SiwakPermission(
                "Tampil di atas aplikasi lain",
                "Agar pengurus bisa membuka dan memulai ulang SIWAK dari HP saat tertutup aplikasi lain.",
                required = true,
                granted = Settings.canDrawOverlays(context),
                action = "overlay",
            )
        list +=
            SiwakPermission(
                "Bebas dari penghemat baterai",
                "Agar layanan SIWAK tetap berjalan di latar belakang dan perintah dari HP selalu diterima.",
                required = true,
                granted = pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true,
                action = "battery",
            )
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        list +=
            SiwakPermission(
                "Notifikasi",
                "Android mewajibkan notifikasi kecil untuk layanan yang berjalan terus di latar belakang.",
                required = true,
                granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED,
                action = "notifications",
            )
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        list +=
            SiwakPermission(
                "Pasang pembaruan aplikasi",
                "Agar pembaruan SIWAK bisa dipasang langsung dari TV.",
                required = false,
                granted = context.packageManager.canRequestPackageInstalls(),
                action = "install",
            )
    }
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
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    val list = remember(tick) { siwakPermissions(context) }
    LaunchedEffect(list) { onChanged(list) }
    var focusAssigned = false
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        list.forEach { p ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F2219))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(if (p.granted) Color(0xFF1C805A) else if (p.required) Color(0xFFA83A2C) else Color(0xFF55665D)),
                    contentAlignment = Alignment.Center,
                ) { Text(if (p.granted) "✓" else "!", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                Column(Modifier.weight(1f)) {
                    Text(p.title + if (!p.required) " (opsional)" else "", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(p.why, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                }
                if (p.granted) {
                    Text("Sudah", color = Color(0xFF4CC38A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    val focusMod = if (!focusAssigned && firstFocus != null) Modifier.focusRequester(firstFocus).also { focusAssigned = true } else Modifier
                    Button(
                        onClick = {
                            if (p.action == "notifications" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                openFor(context, p.action)
                            }
                        },
                        modifier = focusMod,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C805A)),
                    ) { Text("Izinkan", fontSize = 14.sp) }
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
    Box(Modifier.fillMaxSize().background(Color(0xFF07140E)), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(0.82f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Izin agar SIWAK lancar", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                "Berikan izin berikut sekali saja. Tanpa izin ini, perintah dari HP (buka, sinkron, mulai ulang) bisa tidak sampai ke TV.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 15.sp,
            )
            PermissionsList(firstFocus = focus, onChanged = { l -> allRequired = l.count { it.required && !it.granted } })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (allRequired == 0) {
                    Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C805A))) { Text("Selesai", fontSize = 15.sp) }
                } else {
                    OutlinedButton(onClick = onDone) { Text("Lewati dulu", color = Color.White, fontSize = 15.sp) }
                }
            }
        }
    }
}
