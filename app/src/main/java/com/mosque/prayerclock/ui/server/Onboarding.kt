package com.mosque.prayerclock.ui.server

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Zamrud = Color(0xFF0C5138)
private val ZamrudTerang = Color(0xFF1C805A)
private val Kuningan = Color(0xFFE0BA60)
private val Latar = Color(0xFF07140E)
private val Kartu = Color(0xFF0F2219)

/** Detik sebelum layar sambutan lanjut sendiri ke mode mandiri (TV di masjid sering tanpa remote). */
const val WELCOME_AUTO_SECONDS = 90

/**
 * Layar sambutan pada pemasangan baru: pilih menghubungkan ke server atau memakai mode mandiri.
 * Bila tidak ada yang memilih, lanjut otomatis ke mode mandiri agar layar jam tetap tampil.
 */
@Composable
fun WelcomeScreen(
    serverHost: String,
    onConnect: () -> Unit,
    onStandalone: () -> Unit,
) {
    var left by remember { mutableIntStateOf(WELCOME_AUTO_SECONDS) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focus.requestFocus() }
        while (left > 0) {
            delay(1000)
            left--
        }
        onStandalone()
    }
    Box(Modifier.fillMaxSize().background(Latar), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(0.88f).padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Selamat datang di SIWAK", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                "Jam dan jadwal shalat untuk layar masjid. Pilih cara memakai TV ini:",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ChoiceCard(
                    title = "Hubungkan ke server",
                    lines =
                        listOf(
                            "Atur jadwal, suara, tampilan, slide, dan pengumuman dari HP",
                            "Pengurus bisa memantau dan membuka SIWAK dari jauh",
                            "Butuh internet dan akun pengurus di $serverHost",
                        ),
                    primary = true,
                    modifier = Modifier.weight(1f),
                    buttonModifier = Modifier.focusRequester(focus),
                    onClick = onConnect,
                )
                ChoiceCard(
                    title = "Mode mandiri",
                    lines =
                        listOf(
                            "Diatur langsung lewat remote TV",
                            "Tetap jalan tanpa internet",
                            "Bisa dihubungkan ke server kapan saja dari Pengaturan",
                        ),
                    primary = false,
                    modifier = Modifier.weight(1f),
                    onClick = onStandalone,
                )
            }
            Text("Lanjut ke mode mandiri otomatis dalam $left detik", color = Color.White.copy(alpha = 0.55f), fontSize = 14.sp)
        }
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    lines: List<String>,
    primary: Boolean,
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Kartu)
            .border(2.dp, if (primary) Kuningan.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = if (primary) Kuningan else Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        lines.forEach { Text("• $it", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp) }
        Spacer(Modifier.height(6.dp))
        if (primary) {
            Button(onClick = onClick, modifier = buttonModifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ZamrudTerang)) {
                Text(title, fontSize = 16.sp)
            }
        } else {
            OutlinedButton(onClick = onClick, modifier = buttonModifier.fillMaxWidth()) {
                Text("Pakai mode mandiri", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

/**
 * Penjelasan sebelum membuka layar izin Android "Tampil di atas aplikasi lain". Tanpa izin ini SIWAK
 * tidak bisa dibuka lagi dari jauh. Menutup sendiri setelah 45 detik agar tidak menghalangi jam.
 */
@Composable
fun OverlayPermissionDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focus.requestFocus() }
        delay(45_000)
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Izinkan SIWAK dibuka dari jauh", fontSize = 20.sp) },
        text = {
            Text(
                "Agar pengurus bisa membuka atau memulai ulang SIWAK dari HP saat tertutup aplikasi lain, " +
                    "TV perlu izin \"Tampil di atas aplikasi lain\". Di layar berikutnya pilih SIWAK lalu aktifkan.",
                fontSize = 15.sp,
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
                modifier = Modifier.focusRequester(focus),
                colors = ButtonDefaults.buttonColors(containerColor = ZamrudTerang),
            ) { Text("Buka pengaturan izin", fontSize = 15.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Nanti", fontSize = 15.sp) } },
        containerColor = Kartu,
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.85f),
    )
}

/** Penanda kecil di sudut layar bila server tidak terjangkau; tampilan tetap memakai data terakhir. */
@Composable
fun ServerOfflineChip(
    sinceMs: Long?,
    modifier: Modifier = Modifier,
) {
    val since = sinceMs?.let { SimpleDateFormat("HH:mm", Locale("id")).format(Date(it)) }
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFD0584A)))
        Text(
            if (since != null) "Server tidak terjangkau sejak $since · memakai data terakhir" else "Belum tersambung ke server · memakai data terakhir",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
        )
    }
}

/** Konfirmasi singkat setelah TV berhasil dihubungkan. */
@Composable
fun PairedToast(
    visible: Boolean,
    mosqueName: String?,
    deviceName: String?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible, modifier = modifier, enter = fadeIn(tween(300)), exit = fadeOut(tween(300))) {
        Row(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Zamrud)
                .border(2.dp, Kuningan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("✓", color = Kuningan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Column {
                Text(
                    "Terhubung ke ${mosqueName ?: "server SIWAK"}" + (deviceName?.let { " sebagai TV $it" } ?: ""),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("Pengaturan dari admin web akan diterapkan otomatis.", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }
    }
}
