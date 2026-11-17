# SIWAK — Jam & Jadwal Shalat untuk Layar Masjid

**SIWAK** (Sistem Informasi Waktu Shalat) adalah aplikasi Android TV untuk menampilkan jam,
jadwal shalat, tanggal Hijriah, cuaca, dan pengumuman berjalan (running text) di layar masjid
maupun musholla. Dirancang untuk layar besar dan navigasi remote (D-Pad), serta **offline-first**
sehingga tetap berjalan meski internet mati.

Mulai versi 2, TV dapat **dihubungkan ke server SIWAK** (<https://siwak.alfianys.id>) sehingga seluruh
pengaturan, konten, dan pengumuman diatur dari HP lewat admin web, dan setiap TV dapat dipantau dari jauh.
Tanpa server, aplikasi tetap berjalan dalam **mode mandiri** seperti versi 1.

> Proyek open source mandiri. Basis: [MosqueClock](https://github.com/mhdzumair/MosqueClock)
> (open-source, Android TV) oleh Mohamed Zumair, diadaptasi penuh untuk Indonesia. Lisensi MIT.

---

## 📸 Tampilan

<table>
  <tr>
    <td width="50%"><img src="pic/1.png" alt="Layar utama, jam digital" /><br/><sub><b>Layar utama:</b> jam digital, tanggal Hijriah, cuaca, jadwal, running text</sub></td>
    <td width="50%"><img src="pic/8.png" alt="Tema Amber Sunset" /><br/><sub><b>Tema warna:</b> salah satu dari 11 tema (Amber Sunset)</sub></td>
  </tr>
  <tr>
    <td><img src="pic/7.png" alt="Jam analog" /><br/><sub><b>Jam analog</b> dengan tanggal Hijriah &amp; Masehi</sub></td>
    <td><img src="pic/2.png" alt="Format 12 jam" /><br/><sub><b>Format 12 jam</b> (AM/PM); Imsak &amp; Terbit dapat disembunyikan</sub></td>
  </tr>
  <tr>
    <td><img src="pic/3.png" alt="Hitung mundur menuju adzan" /><br/><sub><b>Menuju adzan:</b> hitung mundur (flip clock) &amp; label Adzan/Iqamah</sub></td>
    <td><img src="pic/4.png" alt="Countdown layar penuh adzan" /><br/><sub><b>Countdown layar penuh</b> menjelang adzan</sub></td>
  </tr>
  <tr>
    <td><img src="pic/5.png" alt="Countdown iqamah" /><br/><sub><b>Countdown Iqamah;</b> jeda dapat diatur per waktu</sub></td>
    <td><img src="pic/6.png" alt="Telah masuk waktu shalat" /><br/><sub><b>Masuk waktu shalat:</b> imbauan silent HP + jam</sub></td>
  </tr>
  <tr>
    <td><img src="pic/9.png" alt="Layar Jumat" /><br/><sub><b>Layar Jumat:</b> info khutbah berlangsung (bisa dimatikan)</sub></td>
    <td><img src="pic/10.png" alt="Mode Istirahat Malam" /><br/><sub><b>Mode Istirahat Malam:</b> layar redup otomatis (hemat listrik, awetkan TV)</sub></td>
  </tr>
</table>

---

## ✨ Fitur Utama

### 🌐 Terhubung ke server (versi 2)
- **Hubungkan TV** dengan kode 8 angka atau scan QR dari admin web; TV dikunci PIN yang bisa dilihat dan
  diganti dari web.
- **Semua pengaturan dari HP:** jadwal, koreksi menit, suara (termasuk audio unggahan), tampilan, teks
  berjalan, slide, dan panel informasi (kas, petugas Jumat, agenda, hari besar). Perubahan sampai ke TV
  paling lama 1 menit.
- **Tata letak Galeri** dengan ilustrasi masjid yang berubah mengikuti waktu (fajar, siang, senja, malam)
  dan fase bulan, serta **Mutiara Hadits** berkala dari pustaka hadits yang diperiksa.
- **Pengumuman segera:** banner di atas layar untuk satu atau semua TV masjid, dengan masa tayang.
- **Pemantauan jarak jauh:** status online/offline, apakah SIWAK sedang tampil, selisih jam TV, riwayat
  kejadian, serta perintah **Buka SIWAK**, **Sinkron sekarang**, dan **Mulai ulang SIWAK**.
- **Multi-masjid** dengan peran super admin, admin masjid, dan operator.

Server dan admin web ada di repositori terpisah:
[alfian-yds/siwak-server](https://github.com/alfian-yds/siwak-server). Panduan pengurus:
<https://siwak.alfianys.id/panduan>.

### 🕌 Jadwal shalat yang akurat
Waktu shalat dihitung dari **koordinat lokasi** memakai **metode Kemenag RI** (AlAdhan, method 20),
bukan jadwal tetap. Pindah kota cukup ganti lokasi, seluruh jadwal menyesuaikan otomatis. Tersedia
**koreksi menit per-waktu** agar sama persis dengan jadwal cetak setempat, serta **jadwal manual** penuh.
Nama waktu memakai istilah Indonesia: Imsak, Subuh, Terbit, Dzuhur, Ashar, Maghrib, Isya. Terbit dan
Imsak dapat disembunyikan lewat sakelar.

### 🗓️ Penanggalan & jam
Tanggal **Hijriah** (hisab) dengan **penyesuaian ±hari** agar cocok pengumuman setempat, ditampilkan
berdampingan dengan tanggal Masehi. Jam tersedia mode **digital** maupun **analog**, format 24 jam atau
12 jam, dengan **ukuran yang dapat diperbesar** supaya terbaca dari kejauhan.

### 🔔 Siklus adzan & iqamah
- **Hitung mundur sebelum adzan** (jumlah menit dapat diatur), lengkap dengan tampilan layar penuh.
- **Suara penanda** yang dapat dipilih: bip/pip nyaring, tik-tik hitung mundur, atau **audio kustom**
  (mis. kumandang azan yang diunggah sendiri).
- **Layar "Telah masuk waktu shalat"** berisi imbauan menyenyapkan HP, tampil selama durasi yang diatur.
- **Hitung mundur Iqamah** dengan jeda per-waktu.
- **Layar Jumat** menampilkan info khutbah berlangsung tanpa hitung mundur, dan **bisa dimatikan** untuk
  musholla yang tidak menyelenggarakan shalat Jumat.

### 🎨 Tampilan & informasi
- **11 tema warna** (Emerald Garden, Classic Mosque, Midnight Blue, Ocean Teal, Royal Purple, dan lainnya).
- **Running text** multi-pesan bergilir untuk pengumuman, hadits, atau imbauan.
- **Cuaca** dari OpenWeather **berbasis koordinat** (satu lokasi dengan jadwal), dengan **ikon lokal
  untuk semua kondisi cuaca** dan **cache offline**. TV yang terhubung mendapat cuaca dari server SIWAK;
  pada mode mandiri, cuaca tampil bila pengurus mengisi kunci OpenWeather miliknya sendiri.
- **Media** dapat diunggah sendiri: logo masjid dan gambar latar (utama, layar shalat, iqamah).
- **Mode Istirahat Malam** meredupkan layar otomatis pada rentang jam malam untuk menghemat listrik dan
  memperpanjang umur TV.

### 📡 Andal & mudah dirawat
- **Offline-first:** jadwal & cuaca di-cache. Aplikasi **prefetch jadwal 7 hari** dan menyimpan cuaca
  terakhir, jadi tetap tampil saat jaringan tidak stabil, lalu menyegar otomatis begitu online.
- **Self-update** (flavor `github`) mendeteksi rilis baru di GitHub dan memasang pembaruan **tanpa perlu
  menyetel ulang TV**; seluruh pengaturan tetap tersimpan.

---

## 🛠️ Teknologi

- Kotlin + Jetpack Compose (UI) · Leanback (Android TV)
- Hilt (DI) · Room (cache jadwal/hijriah) · DataStore Preferences (pengaturan, **bertahan saat update**)
- Retrofit + OkHttp + Gson (AlAdhan, OpenWeather, server SIWAK) · Coil (gambar) · ZXing (QR)
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

Pada **mode mandiri**, semua konfigurasi ada di layar Settings, disusun agar mudah diakses remote.
Bila TV terhubung ke server, pengaturan dilakukan dari admin web dan menu di TV dikunci PIN.

1. **Pengaturan Sistem** (akses cepat di paling atas)
2. **Identitas Masjid:** nama, logo, background, bahasa
3. **Lokasi & Jadwal:** kota/koordinat, metode Kemenag, koreksi menit (on/off), jadwal manual, sembunyikan Terbit/Imsak, penyesuaian Hijriah
4. **Waktu & Countdown:** menit sebelum adzan, jeda iqamah, durasi layar silent HP
5. **Tampilan:** jam digital/analog, ukuran jam, tema warna, running text, Layar Jumat, Mode Istirahat Malam
6. **Suara:** adzan & iqamah (beep / tik-tik / audio kustom)
7. **Cuaca:** kunci OpenWeather milik sendiri (hanya mode mandiri), sinkronisasi (lokasi mengikuti Lokasi & Jadwal)
8. **Sistem:** Data Management (status cache jadwal/hijriah/cuaca), izin, tentang

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
│   └── service/      # AlAdhan & OpenWeather API, UpdateChecker (GitHub Releases)
├── domain/           # Aturan layar (prioritas, slide, pengumuman, periode ilustrasi)
├── server/           # Pairing, heartbeat, bundle, konten & media dari server SIWAK
├── service/          # SoundManager, PrayerNotificationService (siklus adzan/iqamah)
├── ui/
│   ├── components/    # DigitalClock, AnalogClock, WeatherCard, ...
│   ├── screens/       # MainScreen, SettingsScreen
│   ├── server/        # Layar Hubungkan, PIN, pengaturan terbatas
│   ├── gallery/       # Tata letak Galeri, ilustrasi, Mutiara Hadits
│   ├── announcement/  # Banner pengumuman segera
│   └── theme/         # ColorTheme (11 tema), Theme, Color
└── viewmodel/         # MainViewModel, SettingsViewModel
```

---

## ⚠️ Penafian jadwal shalat

Jadwal shalat dihitung dari koordinat masjid dengan metode Kemenag RI, sehingga dapat berbeda beberapa
menit dari jadwal resmi setempat. Pengurus masjid wajib mencocokkannya dengan jadwal Kementerian Agama
setempat dan memakai fitur koreksi menit bila perlu.

---

## 📜 Lisensi & Kredit

MIT.<br>
Berbasis MosqueClock (open-source) oleh Mohamed Zumair.<br>
Dikembangkan oleh Alfian Yudisurya.

| Dokumen | Isi |
|---|---|
| [LICENSE](LICENSE) | Lisensi kode (MIT) |
| [NOTICE.md](NOTICE.md) | Pustaka, data, dan aset pihak ketiga beserta lisensinya |
| [PRIVACY.md](PRIVACY.md) | Kebijakan privasi aplikasi dan layanan siwak.alfianys.id |
| [TERMS.md](TERMS.md) | Syarat penggunaan layanan |

Data cuaca dari [OpenWeather](https://openweathermap.org). Nama dan logo SIWAK tidak termasuk lisensi MIT.
