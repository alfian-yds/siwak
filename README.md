# SIWAK — Jam & Jadwal Shalat untuk Layar Masjid

**SIWAK** (Sistem Informasi Waktu Shalat) adalah aplikasi Android TV untuk menampilkan jam,
jadwal shalat, tanggal Hijriah, cuaca, dan pengumuman berjalan (running text) di layar masjid
maupun musholla. Dirancang untuk layar besar dan navigasi remote (D-Pad), serta **offline-first**
— tetap berjalan meski internet mati.

> Proyek Fakultas Teknik, Universitas Negeri Yogyakarta. Basis: [MosqueClock](https://github.com/mhdzumair/MosqueClock)
> (open-source, Android TV) oleh Mohamed Zumair — diadaptasi penuh untuk Indonesia. Lisensi MIT.

---

## ✨ Fitur Utama

- **Jadwal shalat metode Kemenag RI** — dihitung dari koordinat via AlAdhan API (method 20).
  Pindah kota → jadwal otomatis menyesuaikan. Ada **koreksi menit per-waktu** dan **jadwal manual**.
- **Nama waktu Indonesia** — Imsak · Subuh · Terbit · Dzuhur · Ashar · Maghrib · Isya.
- **Sembunyikan Terbit / Imsak** — sakelar tampil/sembunyi.
- **Tanggal Hijriah** — hisab + penyesuaian ±hari agar cocok pengumuman setempat.
- **Jam digital & analog** — ukuran jam dapat diperbesar agar terbaca dari jauh.
- **11 tema warna** (Emerald Garden, Classic Mosque, Midnight Blue, Ocean Teal, Royal Purple, dll).
- **Siklus waktu shalat**:
  - Countdown sebelum adzan (menit dapat diatur)
  - Suara adzan/iqamah: **beep/pip nyaring**, tik-tik hitung mundur, atau **audio kustom** (kumandang azan)
  - Layar "Telah masuk waktu · silent HP" selama X menit (dapat diatur)
  - Countdown Iqamah (jeda per-waktu)
  - **Layar Jumat** (info silent HP + jam, tanpa countdown) — bisa dimatikan untuk musholla
- **Mode Istirahat Malam** — layar redup otomatis pada rentang jam malam (hemat listrik, awetkan TV).
- **Running text** multi-pesan bergilir untuk pengumuman.
- **Media** — unggah logo masjid & gambar latar (utama / layar shalat / iqamah).
- **Cuaca** — via OpenWeatherMap, dengan cache lokal untuk tampilan offline.
- **Self-update** (flavor `github`) — deteksi & pasang update dari GitHub Releases **tanpa setup ulang TV**.

---

## 🛠️ Teknologi

- Kotlin + Jetpack Compose (UI) · Leanback (Android TV)
- Hilt (DI) · Room (cache jadwal/hijriah) · DataStore Preferences (pengaturan, **bertahan saat update**)
- Retrofit + Gson (AlAdhan & OpenWeatherMap) · Coil (gambar)
- minSdk 21 · targetSdk/compileSdk 34

---

## 🚀 Build

**Prasyarat: JDK 17** (mis. Microsoft OpenJDK 17). JBR 25 bawaan terlalu baru untuk Gradle 8.13.

```bash
gradlew.bat clean :app:assembleGithubDebug
```

APK debug: `app/build/outputs/apk/github/debug/app-github-debug.apk`

Untuk build rilis yang ditandatangani (untuk update TV), lihat **[SIGNING.md](SIGNING.md)**:

```bash
gradlew.bat clean :app:assembleGithubRelease
```

### Flavor distribusi

| Flavor      | Self-update | Untuk                           |
|-------------|-------------|---------------------------------|
| `github`    | ✅ aktif     | Sideload / distribusi langsung  |
| `playstore` | ❌ nonaktif  | Google Play (kebijakan Google)  |

---

## ⚙️ Pengaturan (via remote TV)

Semua konfigurasi ada di layar Settings, disusun agar mudah diakses remote:

1. **Pengaturan Sistem** (akses cepat di paling atas)
2. **Identitas Masjid** — nama, logo, background, bahasa
3. **Lokasi & Jadwal** — kota/koordinat, metode Kemenag, koreksi menit (on/off), jadwal manual, sembunyikan Terbit/Imsak, penyesuaian Hijriah
4. **Waktu & Countdown** — menit sebelum adzan, jeda iqamah, durasi layar silent HP
5. **Tampilan** — jam digital/analog, ukuran jam, tema warna, running text, Layar Jumat, Mode Istirahat Malam
6. **Suara** — adzan & iqamah (beep / tik-tik / audio kustom)
7. **Cuaca** — API key OpenWeatherMap, sinkronisasi
8. **Sistem** — Data Management (status cache jadwal/hijriah/cuaca), izin, tentang

### Update tanpa setup ulang

Pengaturan disimpan di **DataStore** yang bertahan lintas update aplikasi. Selama `applicationId`
dan tanda tangan APK **tetap sama** (lihat SIGNING.md), TV yang di-update tidak perlu dikonfigurasi ulang.

---

## 📁 Struktur Kode

```
app/src/main/java/com/mosque/prayerclock/
├── data/
│   ├── model/        # Settings, PrayerTimes, WeatherInfo, enum (SoundType, ClockType, ...)
│   ├── repository/   # SettingsRepository (DataStore), PrayerTimesRepository, WeatherRepository, HijriDateRepository
│   └── service/      # AlAdhan & OpenWeatherMap API, UpdateChecker (GitHub Releases)
├── service/          # SoundManager, PrayerNotificationService (siklus adzan/iqamah)
├── ui/
│   ├── components/    # DigitalClock, AnalogClock, WeatherCard, ...
│   ├── screens/       # MainScreen, SettingsScreen
│   └── theme/         # ColorTheme (11 tema), Theme, Color
└── viewmodel/         # MainViewModel, SettingsViewModel
```

---

## 📜 Lisensi & Kredit

MIT. Berbasis MosqueClock (open-source) oleh Mohamed Zumair.
Dikembangkan oleh Alfian Yudisurya — Departemen Pendidikan Teknik Elektro, FT UNY.
