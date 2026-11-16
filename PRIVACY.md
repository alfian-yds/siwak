# Kebijakan Privasi SIWAK

**Berlaku sejak:** 20 Oktober 2026\
**Diperbarui:** 16 November 2026 (data pemantauan TV, kamera untuk Scan QR, lokasi HP untuk isian koordinat)\
**Pengelola:** Alfian Yudisurya, pengembang SIWAK\
**Kontak privasi:** alfian123yudi@gmail.com

Kebijakan ini menjelaskan data apa yang dikumpulkan oleh aplikasi TV SIWAK dan layanan
<https://siwak.alfianys.id>, untuk apa, dan bagaimana data itu dilindungi. Kebijakan ini disusun dengan
mengacu pada Undang-Undang Nomor 27 Tahun 2022 tentang Pelindungan Data Pribadi.

## 1. Ringkasnya

- SIWAK **tidak mengumpulkan data jamaah**. Layar SIWAK hanya menampilkan informasi.
- Data pribadi yang disimpan hanya milik **pengurus** yang masuk ke admin web: nama, alamat email,
  dan foto profil dari akun Google.
- SIWAK **tidak menjual, menyewakan, atau membagikan** data pribadi untuk iklan.

## 2. Data yang dikumpulkan

### 2.1 Aplikasi TV

| Data | Keterangan | Dikirim ke |
|---|---|---|
| Koordinat lokasi masjid | Diisi pengurus, bukan dari GPS perangkat | AlAdhan (jadwal shalat); OpenWeather (cuaca, pada versi 1.x langsung dari TV, mulai versi 2 lewat server SIWAK) |
| Alamat IP perangkat | Tercatat otomatis saat TV menghubungi layanan internet | Layanan yang dihubungi |
| ID perangkat, nama dan model TV, versi aplikasi, waktu sinkron terakhir, selisih jam TV, ruang penyimpanan tersisa, pesan galat aplikasi | Hanya bila TV dihubungkan ke server SIWAK (versi 2) | Server SIWAK |
| Status tampilan: apakah SIWAK sedang tampil di layar atau berada di latar belakang (dan sejak kapan), serta apakah izin "Tampil di atas aplikasi lain" sudah diberikan | Hanya bila TV dihubungkan ke server SIWAK (versi 2). Dipakai agar pengurus bisa memantau TV dan membuka atau memulai ulang SIWAK dari jauh | Server SIWAK |

Aplikasi TV tidak mengakses kamera, mikrofon, kontak, maupun lokasi GPS perangkat. SIWAK **tidak**
mencatat aplikasi lain apa yang sedang dibuka di TV; yang dilaporkan hanya apakah SIWAK sendiri
sedang tampil atau tidak.
Semua pengaturan disimpan di perangkat TV itu sendiri.

### 2.2 Admin web (siwak.alfianys.id)

| Data | Sumber | Tujuan |
|---|---|---|
| Nama, alamat email, foto profil | Akun Google saat masuk | Mengenali pengurus dan menampilkan siapa yang sedang masuk |
| Peran dan masjid yang dikelola | Diisi super admin | Mengatur hak akses |
| Riwayat perubahan (log aktivitas) | Tercatat saat pengurus mengubah pengaturan atau konten | Akuntabilitas dan pemulihan bila ada kesalahan |
| Alamat IP dan jenis peramban | Log server | Keamanan dan penanganan gangguan |
| Cookie sesi | Dibuat saat masuk | Menjaga sesi login; tidak dipakai untuk pelacakan |
| Gambar kamera HP | Hanya saat pengurus menekan **Scan QR** dan memberi izin kamera | Membaca kode 8 angka di layar TV. Gambar diproses di peramban HP dan **tidak dikirim** ke server |
| Lokasi HP | Hanya saat pengurus menekan **Pakai lokasi saya** dan memberi izin lokasi | Mengisi koordinat masjid di formulir. Yang disimpan hanya koordinat masjid setelah pengurus menekan Simpan |

SIWAK hanya meminta izin dasar dari Google (nama, email, foto profil) untuk keperluan masuk.
SIWAK tidak membaca email, kontak, berkas, atau data lain di akun Google Anda.

### 2.3 Konten yang diunggah pengurus

Pengurus dapat mengunggah logo, foto, audio, dan konten seperti nama petugas Jumat atau laporan kas.
Bila konten tersebut memuat data pribadi orang lain (misalnya nama khatib), pengurus masjid
bertanggung jawab memastikan orang yang bersangkutan setuju namanya ditampilkan.

## 3. Dasar dan tujuan pemrosesan

Data diproses untuk: (a) menjalankan layanan sesuai permintaan pengurus, (b) menjaga keamanan
layanan, dan (c) memenuhi kewajiban hukum bila ada. Pengurus memberikan persetujuan saat masuk
dengan akun Google untuk pertama kali.

## 4. Pihak ketiga

| Pihak | Peran | Data yang diterima |
|---|---|---|
| Google | Layanan masuk (login) | Proses login; Google tunduk pada kebijakan privasinya sendiri |
| AlAdhan | Perhitungan jadwal shalat | Koordinat masjid, alamat IP TV |
| OpenWeather | Data cuaca | Koordinat masjid (dari server SIWAK atau, pada versi 1.x, dari TV) |
| GitHub | Pemeriksaan pembaruan aplikasi | Alamat IP TV |
| Penyedia server (VPS) | Tempat server SIWAK berjalan | Seluruh data layanan, tersimpan di Indonesia (Jawa Barat) |

Data layanan disimpan di Indonesia. Google, AlAdhan, OpenWeather, dan GitHub dapat memproses data
yang mereka terima (koordinat masjid, alamat IP, data login) di server di luar Indonesia, sesuai
kebijakan privasi masing-masing.

## 5. Penyimpanan dan penghapusan

| Data | Lama disimpan |
|---|---|
| Akun pengurus | Selama akun aktif; dihapus paling lambat 30 hari setelah akun dihapus |
| Log aktivitas | 1 tahun |
| Riwayat status perangkat | 90 hari |
| Riwayat perintah jarak jauh (buka/sinkron/mulai ulang SIWAK) | 30 hari |
| Cadangan (backup) database | 30 hari |

## 6. Keamanan

Seluruh lalu lintas memakai HTTPS. Akses admin dibatasi pada email yang didaftarkan. Setiap TV
memakai kunci perangkat sendiri yang dapat dicabut. PIN pengaturan TV disimpan terenkripsi.
Kunci layanan pihak ketiga hanya disimpan di server, tidak di dalam aplikasi.

## 7. Hak Anda

Sesuai UU Pelindungan Data Pribadi, Anda berhak meminta akses, perbaikan, atau penghapusan data
pribadi Anda, serta menarik persetujuan. Kirim permintaan ke alfian123yudi@gmail.com. Kami menanggapi dalam
jangka waktu yang ditetapkan UU Pelindungan Data Pribadi. Bila terjadi kebocoran data, kami akan memberitahukannya kepada pengurus
yang terdampak paling lambat 3 x 24 jam setelah diketahui.

## 8. Anak-anak

Admin web ditujukan bagi pengurus masjid dewasa. Kami tidak dengan sengaja mengumpulkan data anak.

## 9. Server pribadi (self-hosting)

SIWAK adalah perangkat lunak open source. Bila sebuah organisasi menjalankan server SIWAK sendiri,
organisasi tersebut menjadi pengelola data di servernya dan bertanggung jawab atas kebijakan
privasinya sendiri. Kebijakan ini hanya berlaku untuk layanan di siwak.alfianys.id.

## 10. Perubahan kebijakan

Perubahan akan diumumkan di halaman ini dan di repositori GitHub SIWAK, dengan tanggal berlaku baru.
