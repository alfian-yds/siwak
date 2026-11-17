package com.mosque.prayerclock.ui.server

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.mosque.prayerclock.BuildConfig
import com.mosque.prayerclock.server.AppForegroundTracker
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Composable
private fun ScreenFrame(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    BackHandler { onBack() }
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onBack) { Text("Kembali") }
            }
            content()
        }
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() } }
}

private fun qrBitmap(
    text: String,
    size: Int = 360,
): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1))
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) for (y in 0 until size) bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    return bmp
}

// ---- Hubungkan ke server (App Flow §2.2) ---------------------------------------------

@Composable
fun PairingScreen(
    onBack: () -> Unit,
    onPaired: () -> Unit,
    viewModel: PairingViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val server by viewModel.server.collectAsStateWithLifecycle()
    var editingUrl by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.start() }
    LaunchedEffect(ui) { if (ui is PairingUi.Done) onPaired() }

    ScreenFrame("Hubungkan ke server SIWAK", onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "1. Buka ${server?.serverUrl?.removePrefix("https://") ?: "siwak.alfianys.id"}/admin di HP dan masuk.\n" +
                        "2. Pilih masjid → TV → Hubungkan TV.\n" +
                        "3. Ketik kode di bawah, atau tekan Scan QR dan arahkan kamera ke QR ini.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 30.sp,
                )
                when (val s = ui) {
                    is PairingUi.ShowCode -> {
                        Text(
                            "${s.code.take(4)} ${s.code.drop(4)}",
                            fontSize = 72.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 6.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Countdown(s.expiresAtMs)
                    }
                    is PairingUi.Loading -> Text("Meminta kode…", style = MaterialTheme.typography.titleLarge)
                    is PairingUi.Connecting, PairingUi.Done -> Text("Terhubung. Mengambil pengaturan masjid…", style = MaterialTheme.typography.titleLarge)
                    is PairingUi.Error ->
                        Text(s.message + "\nMencoba lagi otomatis…", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
                if (editingUrl) {
                    ServerUrlEditor(server?.serverUrl.orEmpty(), onSave = {
                        editingUrl = false
                        viewModel.setServerUrl(it)
                    }, onCancel = { editingUrl = false })
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Server: ${server?.serverUrl.orEmpty()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.width(12.dp))
                        TextButton(onClick = { editingUrl = true }) { Text("Ubah alamat server") }
                    }
                }
            }
            val code = (ui as? PairingUi.ShowCode)?.code
            Box(
                modifier = Modifier.size(320.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (code != null) {
                    // QR hanya berisi 8 angka, tanpa URL (TRD §6).
                    val bmp = remember(code) { qrBitmap(code) }
                    Image(bmp.asImageBitmap(), contentDescription = "Kode QR $code", modifier = Modifier.fillMaxSize())
                } else {
                    Text("QR", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun Countdown(expiresAtMs: Long) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(expiresAtMs) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            delay(1_000)
        }
    }
    val left = ((expiresAtMs - now) / 1000).coerceAtLeast(0)
    Text(
        "Berlaku ${left / 60}:${(left % 60).toString().padStart(2, '0')} lagi. Kode baru dibuat otomatis.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
    )
}

@Composable
private fun ServerUrlEditor(
    current: String,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var value by remember { mutableStateOf(current) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Alamat server") }, singleLine = true, modifier = Modifier.width(520.dp))
        Text(
            "Ubah hanya bila masjid Anda memakai server SIWAK sendiri.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { if (value.startsWith("http")) onSave(value) }) { Text("Simpan") }
            OutlinedButton(onClick = { onSave(BuildConfig.SIWAK_SERVER_URL) }) { Text("Kembalikan bawaan") }
            TextButton(onClick = onCancel) { Text("Batal") }
        }
    }
}

// ---- PIN (TRD §10) --------------------------------------------------------------------

@Composable
fun PinGate(
    onBack: () -> Unit,
    viewModel: ConnectedSettingsViewModel,
) {
    val message by viewModel.pinMessage.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    fun press(d: String) {
        if (pin.length < 6) pin += d
        if (pin.length == 6) {
            viewModel.checkPin(pin)
            pin = ""
        }
    }

    ScreenFrame("Masukkan PIN", onBack) {
        Column(
            modifier =
                Modifier.fillMaxWidth().onKeyEvent { e ->
                    // Tombol angka di remote TV juga bisa dipakai.
                    if (e.type != KeyEventType.KeyUp) return@onKeyEvent false
                    val digit = (0..9).firstOrNull { e.key == Key(android.view.KeyEvent.KEYCODE_0 + it) }
                    when {
                        digit != null -> {
                            press(digit.toString())
                            true
                        }
                        e.key == Key.Backspace || e.key == Key.Delete -> {
                            pin = pin.dropLast(1)
                            true
                        }
                        else -> false
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Pengaturan TV ini diatur dari admin web. PIN 6 angka bisa dilihat di admin web → TV → Kelola.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
            Text(
                (pin.map { "●" } + List(6 - pin.length) { "○" }).joinToString(" "),
                fontSize = 40.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium) }
            val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "OK"))
            rows.forEachIndexed { r, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEachIndexed { c, label ->
                        Button(
                            onClick = {
                                when (label) {
                                    "⌫" -> pin = pin.dropLast(1)
                                    "OK" ->
                                        if (pin.isNotEmpty()) {
                                            viewModel.checkPin(pin)
                                            pin = ""
                                        }
                                    else -> press(label)
                                }
                            },
                            modifier = Modifier.width(96.dp).height(56.dp).let { if (r == 0 && c == 0) it.focusRequester(first) else it },
                        ) { Text(label, fontSize = 22.sp) }
                    }
                }
            }
        }
    }
}

// ---- Pengaturan terbatas untuk TV terhubung (F-TV-17) --------------------------------

@Composable
fun ConnectedSettingsScreen(
    onBack: () -> Unit,
    onUnpaired: () -> Unit,
    viewModel: ConnectedSettingsViewModel = hiltViewModel(),
) {
    // rc.3: tanpa PIN. Pengaturan TV terhubung memang terbatas; perubahan utama dilakukan dari admin web.
    val server by viewModel.server.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmUnpair by remember { mutableStateOf(false) }
    val canOpen = AppForegroundTracker.canOpenFromBackground(context)
    val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val s = server

    ScreenFrame("Pengaturan TV", onBack) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                InfoCard {
                    Text("Terhubung ke server", style = MaterialTheme.typography.titleLarge)
                    Text("Masjid: ${s?.mosqueName ?: "—"}")
                    Text("Nama TV: ${s?.deviceName ?: "—"}")
                    Text("Server: ${s?.serverUrl ?: "—"}")
                    Text(
                        "Sinkron terakhir: " + (s?.lastSyncAt?.takeIf { it > 0 }?.let { fmt.format(Date(it)) } ?: "belum pernah") +
                            " · Kontak terakhir: " + (s?.lastContactAt?.takeIf { it > 0 }?.let { fmt.format(Date(it)) } ?: "belum pernah"),
                    )
                    status.lastError?.let { Text("Masalah terakhir: $it", color = MaterialTheme.colorScheme.error) }
                    Text(
                        "Pengaturan jadwal, suara, dan tampilan diubah dari admin web, bukan dari TV.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = viewModel::syncNow, enabled = !status.syncing) { Text(if (status.syncing) "Menyinkron…" else "Sinkron sekarang") }
                }
            }
            item {
                InfoCard {
                    Text("Tes suara", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { viewModel.testSound(true) }) { Text("Tes suara adzan") }
                        OutlinedButton(onClick = { viewModel.testSound(false) }) { Text("Tes suara iqamah") }
                    }
                }
            }
            item {
                InfoCard {
                    Text("Izin aplikasi", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Izin berikut dibutuhkan agar perintah dari HP (buka, sinkron, mulai ulang) selalu sampai dan bisa dijalankan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PermissionsList()
                }
            }
            item {
                InfoCard {
                    Text("Lepas dari server", style = MaterialTheme.typography.titleLarge)
                    Text("TV kembali ke mode mandiri dengan pengaturan terakhir. Untuk menghubungkan lagi dibutuhkan kode baru.")
                    OutlinedButton(onClick = { confirmUnpair = true }) { Text("Lepas dari server", color = MaterialTheme.colorScheme.error) }
                }
            }
            item {
                InfoCard {
                    Text("Tentang", style = MaterialTheme.typography.titleLarge)
                    Text("SIWAK versi ${BuildConfig.VERSION_NAME}")
                    Text("Kebijakan privasi & syarat: ${s?.serverUrl ?: "siwak.alfianys.id"}/privasi")
                    Text("Data cuaca dari OpenWeather")
                    if (s?.minAppVersion != null && isOlderVersion(BuildConfig.VERSION_NAME, s.minAppVersion)) {
                        Text("Tersedia pembaruan aplikasi. Perbarui agar semua fitur server berjalan.", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    if (confirmUnpair) {
        AlertDialog(
            onDismissRequest = { confirmUnpair = false },
            title = { Text("Lepas TV dari server?") },
            text = { Text("TV kembali ke mode mandiri dengan pengaturan terakhir.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnpair = false
                    viewModel.unpair(onUnpaired)
                }) { Text("Lepas") }
            },
            dismissButton = { TextButton(onClick = { confirmUnpair = false }) { Text("Batal") } },
        )
    }
}
