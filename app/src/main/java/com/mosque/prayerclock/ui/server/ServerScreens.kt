package com.mosque.prayerclock.ui.server

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import com.mosque.prayerclock.ui.tv.tvFocusHighlight
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
import com.mosque.prayerclock.ui.tv.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import com.mosque.prayerclock.ui.tv.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.mosque.prayerclock.ui.tv.TextButton
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
import androidx.compose.ui.graphics.Brush
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
private fun InfoCard(
    /** Kartu berisi teks saja dibuat bisa difokus; kartu yang punya tombol membiarkan tombolnya yang difokus. */
    focusable: Boolean = false,
    content: @Composable () -> Unit,
) {
    Card(
        // Kartu bisa menerima fokus remote, jadi tombol bawah berpindah kartu demi kartu dan isinya
        // sempat terbaca, tidak melompat ke tombol berikutnya yang jauh di bawah.
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .tvFocusHighlight(RoundedCornerShape(16.dp))
                .then(if (focusable) Modifier.focusable() else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        androidx.compose.material3.ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        }
    }
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

    ScreenFrame("Hubungkan ke website SIWAK", onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "1. Buka ${server?.serverUrl?.removePrefix("https://") ?: "siwak.alfianys.id"}/admin di HP dan masuk.\n" +
                        "2. Pilih masjid → Beranda → Hubungkan TV.\n" +
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
                            "Alamat website: ${server?.serverUrl.orEmpty()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.width(12.dp))
                        TextButton(onClick = { editingUrl = true }) { Text("Ubah alamat website") }
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
        OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Alamat website") }, singleLine = true, modifier = Modifier.width(520.dp))
        Text(
            "Ubah hanya bila masjid Anda memakai website SIWAK sendiri (dipasang di server sendiri).",
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

// ---- Pengaturan TV terhubung (F-TV-17) ------------------------------------------------
// Kelompok dan urutannya sama dengan mode mandiri dan menu website (Koneksi, Jadwal, Suara, Tampilan, Konten,
// Sistem). Yang diatur dari website tampil sebagai ringkasan agar pengurus tahu nilainya dan di mana mengubahnya.

@Composable
fun ConnectedSettingsScreen(
    onBack: () -> Unit,
    onUnpaired: () -> Unit,
    viewModel: ConnectedSettingsViewModel = hiltViewModel(),
) {
    val server by viewModel.server.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    var confirmUnpair by remember { mutableStateOf(false) }
    val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val s = server
    val localVm: com.mosque.prayerclock.viewmodel.SettingsViewModel = hiltViewModel()
    val local by localVm.settings.collectAsStateWithLifecycle()
    val web = (s?.serverUrl ?: BuildConfig.SIWAK_SERVER_URL).removePrefix("https://").removeSuffix("/")
    val onOff = { b: Boolean -> if (b) "Aktif" else "Mati" }

    // Bingkai sama dengan Pengaturan mode mandiri: latar gradasi tema, tombol Kembali di kiri, judul Pengaturan.
    BackHandler { onBack() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface))),
    ) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
        com.mosque.prayerclock.ui.screens.SettingsHeader(onNavigateBack = onBack)
        Spacer(Modifier.height(24.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { com.mosque.prayerclock.ui.screens.SettingsGroupHeader("Koneksi") }
            item {
                InfoCard {
                    Text("Terhubung ke website SIWAK", style = MaterialTheme.typography.titleMedium)
                    Text("Masjid: ${s?.mosqueName ?: "—"}")
                    Text("Nama TV: ${s?.deviceName ?: "—"}")
                    Text("Alamat website: $web")
                    Text(
                        "Terakhir diperbarui: " + (s?.lastSyncAt?.takeIf { it > 0 }?.let { fmt.format(Date(it)) } ?: "belum pernah") +
                            " · Terakhir menghubungi website: " + (s?.lastContactAt?.takeIf { it > 0 }?.let { fmt.format(Date(it)) } ?: "belum pernah"),
                    )
                    status.lastError?.let { Text("Masalah terakhir: $it", color = MaterialTheme.colorScheme.error) }
                    Text(
                        "Jadwal, suara, tampilan, dan konten diubah dari HP: buka $web/admin → pilih masjid → Pengaturan atau Konten. " +
                            "Urutan menunya sama dengan halaman ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = viewModel::syncNow, enabled = !status.syncing) { Text(if (status.syncing) "Memperbarui…" else "Perbarui sekarang") }
                }
            }

            item { com.mosque.prayerclock.ui.screens.SettingsGroupHeader("Jadwal") }
            item {
                SummaryCard(
                    "Diatur dari website: Pengaturan → Jadwal",
                    listOf(
                        "Nama masjid" to local.mosqueName.ifBlank { s?.mosqueName ?: "—" },
                        "Sumber jadwal" to local.prayerServiceType.displayName,
                        "Waktu yang ditampilkan" to listOfNotNull("Imsak".takeIf { local.showImsak }, "Terbit".takeIf { local.showSunrise }).ifEmpty { listOf("Hanya 5 waktu shalat") }.joinToString(", "),
                        "Koreksi menit" to onOff(local.prayerOffsetEnabled),
                        "Iqamah" to
                            "Subuh ${local.fajrIqamahGap} · Dzuhur ${local.dhuhrIqamahGap} · Ashar ${local.asrIqamahGap} · " +
                            "Maghrib ${local.maghribIqamahGap} · Isya ${local.ishaIqamahGap} menit",
                        "Hitung mundur layar penuh" to if (local.fullScreenCountdownEnabled) "Aktif, ${local.fullScreenCountdownMinutes} menit sebelum adzan" else "Mati",
                        "Layar waktu shalat & Jumat" to
                            "Layar shalat ${local.duaDisplayDurationMinutes} menit · " +
                            if (local.showJummahScreen) "layar Jumat ${local.jummahDurationMinutes} menit" else "tanpa layar Jumat",
                        "Bayan malam Jumat" to if (local.jummaNightBayanEnabled) "Aktif, ${local.jummaNightBayanMinutes} menit" else "Mati",
                    ),
                )
            }

            item { com.mosque.prayerclock.ui.screens.SettingsGroupHeader("Suara") }
            item {
                InfoCard {
                    SummaryRows(
                        listOf(
                            "Suara adzan" to if (local.azanSoundEnabled) local.azanSoundType.displayName else "Mati",
                            "Suara iqamah" to if (local.iqamahSoundEnabled) local.iqamahSoundType.displayName else "Mati",
                        ),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { viewModel.testSound(true) }) { Text("Tes suara adzan") }
                        OutlinedButton(onClick = { viewModel.testSound(false) }) { Text("Tes suara iqamah") }
                    }
                    WebHint("Diatur dari website: Pengaturan → Suara")
                }
            }

            item { com.mosque.prayerclock.ui.screens.SettingsGroupHeader("Tampilan") }
            item {
                SummaryCard(
                    "Diatur dari website: Pengaturan → Tampilan",
                    listOf(
                        "Logo & gambar latar" to
                            listOfNotNull(
                                "logo".takeIf { local.mosqueLogoUri.isNotBlank() },
                                "latar utama".takeIf { local.backgroundMainUri.isNotBlank() },
                                "latar waktu shalat".takeIf { local.backgroundPrayerUri.isNotBlank() },
                                "latar iqamah".takeIf { local.backgroundIqamahUri.isNotBlank() },
                            ).ifEmpty { listOf("belum ada") }.joinToString(", "),
                        "Tema warna" to local.colorTheme.split('_').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } },
                        "Bahasa" to local.language.displayName,
                        "Jam" to (if (local.show24HourFormat) "24 jam" else "12 jam") + if (local.showSeconds) ", dengan detik" else ", tanpa detik",
                        "Mode malam" to
                            if (local.nightModeEnabled) {
                                "Aktif, %02d.00–%02d.00".format(local.nightStartHour, local.nightEndHour)
                            } else {
                                "Mati"
                            },
                        "Cuaca" to if (local.showWeather) "Tampil" else "Tidak tampil",
                    ),
                )
            }

            item { com.mosque.prayerclock.ui.screens.SettingsGroupHeader("Konten") }
            item {
                SummaryCard(
                    "Diatur dari website: Konten → Pengumuman, Teks berjalan, Slide, Hadits",
                    listOf(
                        "Slide & hadits" to
                            if (!local.contentEnabled) {
                                "Mati (jam & jadwal saja)"
                            } else {
                                listOfNotNull(
                                    "di samping jam".takeIf { local.layout == "GALERI" },
                                    "layar penuh setiap ${local.mutiaraEveryMinutes} menit".takeIf { local.mutiaraHaditsEnabled },
                                ).ifEmpty { listOf("belum dipilih cara tampilnya") }.joinToString(" + ").replaceFirstChar { it.uppercase() }
                            },
                        "Teks berjalan & pengumuman" to "Dikirim dari website dan tampil di bagian bawah layar",
                    ),
                )
            }

            item { com.mosque.prayerclock.ui.screens.SettingsGroupHeader("Sistem") }
            item { PermissionsCard() }
            item { com.mosque.prayerclock.ui.screens.SystemSettingsAccess() }
            item {
                // Versi, pengembang, kredit, tautan legal, dan pembaruan aplikasi: sama dengan mode mandiri.
                com.mosque.prayerclock.ui.screens.AboutSettings(
                    autoUpdateCheckEnabled = local.autoUpdateCheckEnabled,
                    onAutoUpdateCheckEnabledChange = localVm::updateAutoUpdateCheckEnabled,
                )
            }
            if (s?.minAppVersion != null && isOlderVersion(BuildConfig.VERSION_NAME, s.minAppVersion)) {
                item {
                    InfoCard {
                        Text("Website SIWAK membutuhkan versi aplikasi yang lebih baru. Perbarui lewat tombol di atas.", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item {
                InfoCard {
                    Text("Putuskan dari website", style = MaterialTheme.typography.titleMedium)
                    Text("TV kembali ke mode mandiri dengan pengaturan terakhir, lalu bisa diatur langsung dengan remote. Untuk menghubungkan lagi dibutuhkan kode baru.")
                    OutlinedButton(onClick = { confirmUnpair = true }) { Text("Putuskan dari website", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        }
    }

    if (confirmUnpair) {
        AlertDialog(
            onDismissRequest = { confirmUnpair = false },
            title = { Text("Putuskan TV dari website?") },
            text = { Text("TV kembali ke mode mandiri dengan pengaturan terakhir.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnpair = false
                    viewModel.unpair(onUnpaired)
                }) { Text("Putuskan") }
            },
            dismissButton = { TextButton(onClick = { confirmUnpair = false }) { Text("Batal") } },
        )
    }
}

/** Ringkasan pengaturan yang diubah dari website (hanya dibaca di TV). */
@Composable
private fun SummaryCard(
    hint: String,
    rows: List<Pair<String, String>>,
) {
    InfoCard(focusable = true) {
        SummaryRows(rows)
        WebHint(hint)
    }
}

@Composable
private fun SummaryRows(rows: List<Pair<String, String>>) {
    rows.forEach { (label, value) ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(label, modifier = Modifier.width(260.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun WebHint(text: String) {
    Text("✎ $text", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
}

