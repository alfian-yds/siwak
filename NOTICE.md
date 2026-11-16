# Pemberitahuan Hak Cipta & Pihak Ketiga

Dokumen ini mencantumkan karya pihak lain yang dipakai di aplikasi TV SIWAK, beserta lisensinya.
Kode sumber SIWAK sendiri berlisensi MIT (lihat [LICENSE](LICENSE)).

## 1. Proyek asal

SIWAK dikembangkan dari **MosqueClock** karya Mohamed Zumair
(<https://github.com/mhdzumair/MosqueClock>). README MosqueClock menyatakan proyek tersebut
berlisensi MIT. Pemberitahuan hak cipta MosqueClock dipertahankan di [LICENSE](LICENSE).

> Catatan: repositori MosqueClock tidak menyertakan file LICENSE terpisah. SIWAK berpegang pada
> pernyataan lisensi MIT di README-nya dan telah/akan meminta konfirmasi kepada pemilik proyek.

## 2. Nama dan logo

Nama **SIWAK** dan logonya tidak termasuk dalam lisensi MIT. Turunan (fork) boleh memakai kodenya,
tetapi diminta memakai nama dan logo sendiri agar tidak membingungkan pengguna.

## 3. Pustaka perangkat lunak

| Komponen | Lisensi |
|---|---|
| AndroidX (Core, AppCompat, Activity, Lifecycle, Navigation, Leanback, Room, DataStore, Hilt Navigation) | Apache License 2.0 |
| Jetpack Compose (UI, Material 3, Material Icons Extended) | Apache License 2.0 |
| Kotlin, kotlinx.coroutines, kotlinx-datetime | Apache License 2.0 |
| Dagger / Hilt | Apache License 2.0 |
| Retrofit, OkHttp | Apache License 2.0 |
| Gson | Apache License 2.0 |
| Coil | Apache License 2.0 |
| compose-markdown (jeziellago) | MIT License |
| ZXing core 3.3.3 (pembuat kode QR di layar Hubungkan) | Apache License 2.0 |

Teks lisensi: Apache 2.0 <https://www.apache.org/licenses/LICENSE-2.0>,
MIT <https://opensource.org/license/mit>.

**Riwayat versi 1.0 sampai 1.2:** APK versi tersebut masih menyertakan iText 5
(`com.itextpdf:itextg`, GNU AGPL v3, <https://www.gnu.org/licenses/agpl-3.0.html>) dan jsoup
(MIT) dari modul lama MosqueClock untuk jadwal Sri Lanka (ACJU) yang tidak aktif di SIWAK.
Sesuai AGPL v3, kode sumber lengkap versi-versi tersebut tetap tersedia di riwayat Git repositori
ini. Mulai versi 1.3 modul tersebut beserta iText dan jsoup telah dihapus dari aplikasi.

## 4. Data dan layanan pihak ketiga

| Layanan | Dipakai untuk | Ketentuan |
|---|---|---|
| AlAdhan API (<https://aladhan.com>) | Menghitung jadwal shalat metode Kemenag RI dari koordinat masjid | Mengikuti ketentuan penggunaan AlAdhan |
| OpenWeather (<https://openweathermap.org>) | Data cuaca | Data OpenWeather berlisensi CC BY-SA 4.0 / ODbL dan wajib mencantumkan atribusi: "Data cuaca dari OpenWeather" |
| GitHub Releases | Memeriksa dan mengunduh pembaruan aplikasi | Mengikuti ketentuan GitHub |

## 5. Aset suara

| File | Asal | Lisensi |
|---|---|---|
| `app/src/main/res/raw/beep_countdown_3s.mp3` | "Race Start Beeps" oleh transcendedlifting, dari Pixabay (ID 125125) | Pixabay Content License (<https://pixabay.com/service/license-summary/>) |
| `app/src/main/res/raw/countdown_ticking_5s.mp3` | Bawaan MosqueClock | MIT, mengikuti lisensi MosqueClock |

Pixabay Content License mengizinkan pemakaian di dalam aplikasi tanpa atribusi wajib, tetapi melarang
menjual atau membagikan berkas suara tersebut secara terpisah sebagai berkas mandiri. Berkas ini
hanya disertakan sebagai bagian dari aplikasi SIWAK.

## 6. Font

| Berkas | Font | Hak cipta | Lisensi |
|---|---|---|---|
| `app/src/main/res/font/amiri_regular.ttf`, `amiri_bold.ttf` | Amiri (subset Arab), untuk teks Arab hadits | Copyright 2010-2022 The Amiri Project Authors (<https://github.com/aliftype/amiri>) | SIL Open Font License 1.1, teks lengkap di [licenses/OFL-1.1.txt](licenses/OFL-1.1.txt) |

Berkas TTF dibuat dari WOFF paket npm `@fontsource/amiri` dengan `tools/woff_to_ttf.py` (hanya membungkus
ulang tabel font, isinya tidak diubah), sehingga nama font tetap "Amiri" sesuai ketentuan OFL.

## 7. Konten keagamaan

Teks Arab hadits adalah karya yang telah menjadi milik umum. Terjemah dan pustaka hadits SIWAK
mencatat sumbernya pada setiap entri. Konten yang ditambahkan pengurus masjid menjadi tanggung jawab
pengurus masjid tersebut (lihat [TERMS.md](TERMS.md)).
