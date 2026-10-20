# Panduan Signing Rilis — SIWAK

APK rilis harus **ditandatangani dengan keystore yang sama** setiap kali update.
Kalau tanda tangannya beda, Android akan **menolak install update** (harus uninstall dulu,
dan semua pengaturan TV hilang). Jadi: **buat keystore SEKALI, simpan baik-baik, pakai selamanya.**

---

## 1. Buat keystore (sekali saja)

Buka terminal di folder project (`SIWAK`), jalankan (keytool ikut JDK 17):

```bash
keytool -genkeypair -v -keystore siwak-release.keystore -alias siwak -keyalg RSA -keysize 2048 -validity 10000
```

Akan ditanya:
- **password keystore** — catat, jangan lupa
- nama, organisasi, kota → boleh diisi bebas (mis. "SIWAK", "Yogyakarta")
- **password key** — boleh sama dengan password keystore (tekan Enter untuk menyamakan)

Hasilnya: file `siwak-release.keystore` di folder project.

> ⚠️ **BACKUP file `siwak-release.keystore` + kedua password ke tempat aman** (Google Drive
> pribadi / password manager). Kalau hilang, Anda **tidak bisa lagi** merilis update untuk TV
> yang sudah terpasang — harus install ulang dari nol.

---

## 2. Daftarkan keystore ke `local.properties`

File `local.properties` di root project **tidak ikut Git** (aman untuk rahasia).
Tambahkan 4 baris ini (sesuaikan password & alias):

```properties
RELEASE_STORE_FILE=siwak-release.keystore
RELEASE_STORE_PASSWORD=passwordKeystoreAnda
RELEASE_KEY_ALIAS=siwak
RELEASE_KEY_PASSWORD=passwordKeyAnda
```

`build.gradle.kts` otomatis membaca 4 nilai ini saat build release. Kalau keempatnya tidak ada,
build release tetap jalan tapi **tidak ditandatangani** (tidak bisa dipakai untuk update).

---

## 3. Build APK rilis yang ditandatangani

```bash
gradlew.bat clean :app:assembleGithubRelease
```

Hasil: `app/build/outputs/apk/github/release/app-github-release.apk`

APK ini yang di-**upload ke GitHub Releases** dan yang di-install ke TV.

---

## 4. Cek tanda tangan (opsional)

```bash
keytool -printcert -jarfile app/build/outputs/apk/github/release/app-github-release.apk
```

Pastikan SHA-256 sama dengan rilis sebelumnya → berarti update akan diterima TV.

---

## Ringkasan alur update TV (tanpa setup ulang)

1. Naikkan `versionCode` (+1) & `versionName` di `app/build.gradle.kts`.
2. Build `assembleGithubRelease` (pakai keystore yang SAMA).
3. Buat rilis baru di GitHub `alfian-yds/siwak`, upload APK-nya.
4. TV yang sudah terpasang akan mendeteksi update (fitur self-update), download, install.
5. **Pengaturan tetap** — nama masjid, lokasi, koreksi menit, tema, dll tersimpan di DataStore
   dan tidak terhapus saat update (asal `applicationId` & tanda tangan tetap sama).
