# Jadwal Adzan (widget Android, gratis, tanpa iklan)

Aplikasi Android native (Kotlin, tanpa Compose) berisi widget jadwal sholat.
Data dari API MyQuran v2 (sumber Kemenag RI). Default: Kota Banjarbaru (WITA).

- minSdk 26, targetSdk 34, AGP 8.5.2, Kotlin 1.9.24, JDK 17, Gradle 8.7
- Widget: nama lokasi, sholat berikutnya (tanda "besok" setelah Isya), lima waktu, sorotan amber. Klik widget membuka aplikasi.
- Jadwal hari ini dan besok disimpan di SharedPreferences, jadi tetap tampil saat offline.
- Alarm tepat waktu (`setExactAndAllowWhileIdle`), otomatis dijadwalkan ulang setelah alarm bunyi, reboot, dan aplikasi diperbarui.

## Cara build

### A. Online lewat GitHub Actions (tanpa PC)
1. Buat repositori baru di GitHub, lalu unggah seluruh isi proyek ini (lihat bagian berikut).
2. Buka tab **Actions** → workflow **Build APK** (jalan otomatis setiap push; bisa juga **Run workflow**).
3. Setelah hijau, buka run tersebut → bagian **Artifacts** → unduh `JadwalAdzan-debug-apk` (zip berisi `app-debug.apk`).
4. Ekstrak, buka `app-debug.apk` di HP, izinkan "Pasang aplikasi tidak dikenal" bila diminta.

### B. Android Studio
1. Ekstrak zip, **File → Open** → pilih folder `JadwalAdzan`.
2. Biarkan Gradle sync (Gradle 8.7, JDK 17 bawaan Android Studio).
3. Jalankan ke HP, atau **Build → Build APK(s)**.

## Mengunggah ke GitHub dari HP
Web GitHub di HP tidak bisa mengunggah folder (dan folder tersembunyi `.github`), jadi cara paling andal:

**Termux** (pasang dari F-Droid):
```
pkg update && pkg install git unzip
termux-setup-storage
cd ~
unzip ~/storage/downloads/JadwalAdzan.zip
cd JadwalAdzan
git init -b main
git add .
git commit -m "Jadwal Adzan"
git remote add origin https://github.com/USERNAME/NAMA-REPO.git
git push -u origin main
```
- Saat diminta password, pakai **Personal Access Token** (GitHub → Settings → Developer settings → Tokens). Centang scope **repo** dan **workflow** (wajib agar file `.github/workflows` bisa diunggah).
- Alternatif: buka repo di **github.com/codespaces** lewat browser HP, unggah zip lewat Explorer, `unzip`, lalu `git push`.

## Suara adzan kustom (opsional)
Letakkan file di `app/src/main/res/raw/adzan.mp3` sebelum build. Tanpa file itu dipakai nada notifikasi bawaan. Suara diputar lewat saluran notifikasi dengan volume **alarm**.

## Agar adzan tidak terlewat di OriginOS
Nama menu bisa sedikit berbeda antar versi; intinya sama:
1. Buka aplikasi → **Izin notifikasi** → Izinkan.
2. **Izin alarm tepat waktu** → aktifkan "Alarm & pengingat" untuk aplikasi ini.
3. **Pengaturan aplikasi** → **Autostart / Mulai otomatis** → aktifkan.
4. Di pengaturan yang sama (atau Pengaturan → Baterai) → **Baterai / Penggunaan daya latar belakang** → pilih **Tanpa batasan** (jangan "Hemat"/"Cerdas").
5. Notifikasi → saluran **Waktu Sholat** → Penting/Berbunyi, tampilkan di layar kunci.
6. Jangan "Hentikan paksa" aplikasi (itu menghapus alarm; buka aplikasi sekali untuk menjadwalkan ulang). Jika tersedia, kunci aplikasi di layar aplikasi terbaru.
7. Pastikan volume **alarm** tidak nol dan mode Jangan Ganggu tidak memblokir aplikasi ini.
8. Tekan **Tes notifikasi** untuk memastikan semuanya berfungsi.

## Catatan
- Pilih zona waktu (WIB/WITA/WIT) sesuai kota; jadwal dari API berupa jam lokal kota.
- Jika izin alarm tepat waktu belum aktif, alarm memakai cadangan `setAndAllowWhileIdle` (bisa meleset beberapa menit).
