# Product Requirements Document (PRD) — Selaras

| Metadata | Keterangan |
|---|---|
| **Nama Produk** | Selaras (sebelumnya tercantum sebagai *Serasi* pada artefak desain awal) |
| **Platform** | Android (Min SDK 24, Target SDK 37) |
| **Arsitektur Utama** | **Offline-First Ready, Local Encrypted Storage (SQLCipher), Jetpack Compose** |
| **Tema Desain** | Tema Tunggal Terstandar (*Single Standard Theme* / Light Tone) |
| **Status Dokumen** | **Verified (Living Document — Sinkron dengan Revisi Figma & Arsitektur Offline-First ADR-014)** |
| **Versi Dokumen** | 1.2.0 |
| **Terakhir Diperbarui** | 2026-11-21 |

---

## 1. Product Overview

**Selaras** adalah aplikasi pendamping kesehatan emosional mandiri (*self-care companion*) berbasis Android yang dirancang dengan pendekatan etis, privat, dan **Offline-First Ready**. Aplikasi ini memfasilitasi pencatatan suasana hati harian (*Daily Check-in*), latihan relaksasi pernapasan terpandu (*Guided Breathing*), penapisan gejala depresi menggunakan instrumen baku **PHQ-9**, serta pembuatan ringkasan terstruktur (*Counselor Summary*) yang dapat diekspor ke dalam format PDF untuk dibawa ke sesi konsultasi profesional (konselor/psikolog).

Selaras menempatkan privasi dan kedaulatan data di tangan pengguna:
- **Operasional Mandiri 100% Offline**: Seluruh fitur inti berjalan secara penuh tanpa koneksi internet dan tanpa mewajibkan pembuatan akun cloud.
- **Kesiapan Sinkronisasi Terdistribusi (Offline-First Ready)**: Skema data dan lapisan abstraksi repository dirancang untuk mendukung sinkronisasi dua arah ke backend server (Node.js + Express.js + PostgreSQL) secara opsional di masa depan (ADR-014) untuk kebutuhan backup akun dan akses multi-device tanpa merombak arsitektur antarmuka.

---

## 2. Problem Statement

1. **Stigma dan Keraguan Mencari Bantuan**: Banyak individu merasakan penurunan kondisi emosional namun kesulitan mengartikulasikan apa yang dirasakan, serta ragu untuk berkonsultasi karena tidak memiliki catatan historis yang obyektif.
2. **Kekhawatiran Privasi Data Kesehatan Mental**: Data kesehatan mental adalah data yang sangat sensitif. Pengguna kerap enggan menggunakan aplikasi kesehatan mental komersial yang mengharuskan pendaftaran email/nomor telepon dan menyimpan catatan pribadi di server pihak ketiga (*cloud*).
3. **Kesenjangan Komunikasi dengan Tenaga Profesional**: Ketika pengguna akhirnya menemui psikolog/konselor, waktu sesi sering kali tersita untuk mengingat kembali pola tidur, energi, dan suasana hati beberapa minggu terakhir secara retrospektif yang rentan bias ingatan (*recall bias*).
4. **Beban Emosional Berlebih pada Antarmuka**: Banyak aplikasi menghadirkan antarmuka yang terlalu ramai, fitur sosial yang memicu kecemasan, atau *gamifikasi* yang tidak sesuai dengan kondisi psikologis pengguna yang sedang tertekan.

---

## 3. Product Goals

1. **Menyediakan Ruang Refleksi Mandiri yang Aman**: Memberikan sarana pencatatan emosi, energi, dan tidur yang cepat, tenang, dan bebas dari distraksi sosial.
2. **Menjamin Kedaulatan Privasi 100%**: Menjamin bahwa seluruh data emosional dan hasil penapisan tersimpan secara lokal dan terenkripsi pada perangkat pengguna tanpa analitik pihak ketiga yang invasif.
3. **Membantu Penapisan Awal yang Bertanggung Jawab**: Menyediakan instrumen PHQ-9 yang terstandarisasi dengan batasan medis yang jelas: hasil penapisan adalah panduan refleksi dan bukan vonis diagnosis klinis.
4. **Menjembatani Pengguna dan Konselor**: Menyediakan fitur ringkasan data terverifikasi (*Counselor Summary*) untuk meningkatkan efektivitas konsultasi tatap muka atau daring bersama konselor berlisensi.

---

## 4. Target Users

- **Dewasa Muda dan Mahasiswa (Usia 18–35 tahun)**: Individu yang mengalami tekanan akademik, pekerjaan, atau fase transisi hidup dan membutuhkan pelacak kondisi emosional berkala.
- **Individu dalam Pendampingan Konseling**: Klien yang dianjurkan oleh psikolog atau konselor untuk melakukan pemantauan mandiri (*mood tracking*) dan membawa rekap data saat sesi terapi.
- **Pengguna Sadar Privasi (*Privacy-conscious Individuals*)**: Pengguna yang menolak aplikasi berbasis langganan atau aplikasi yang menuntut login cloud untuk data kesehatan mental.

---

## 5. Scope & Out of Scope

Mengacu pada ketentuan pengikat handoff desain (**Figma node `5:154`**) dan implementasi aktual:

### In-Scope (MVP Lengkap)
- **Splash & Onboarding**: Layar selamat datang, pengenalan nilai aplikasi (*value propositions*), dan panduan awal.
- **Informed Consent**: Lembar persetujuan eksplisit mengenai batas tanggung jawab medis dan kebijakan penyimpanan lokal terenkripsi.
- **Kunci Aplikasi (*App Lock*) & Pemulihan**: Pengamanan akses berbasis PIN 6-digit lokal, proteksi salah PIN, dan alur pemulihan (*Recovery Code* 16-karakter) untuk reset PIN tanpa merusak database.
- **Dashboard / Beranda**: Visualisasi status check-in hari ini, akses cepat latihan napas & skrining, dan tombol bantuan darurat permanen.
- **F1 · Skrining PHQ-9**: Kuesioner 9 butir terstandarisasi, kuota berkala (1 manual + 1 rutin per minggu), kalkulasi skor lokal (0–27), dan protokol krisis.
- **F2 · Latihan Relaksasi**: Latihan pernapasan terpandu dengan pola bawaan serta pembuatan, pengeditan, dan penghapusan pola kustom (*custom patterns*).
- **F3 · Daily Check-in & Analisis Pola**:
  - Penilaian **Suasana Hati (*Mood*)** berskala **1–5**.
  - Penilaian **Tingkat Energi (*Energy*)** berskala **1–10** (1 = sangat rendah, 10 = sangat tinggi).
  - Durasi tidur malam (dalam menit / jam) dan catatan jurnal refleksi teks bebas.
  - Visualisasi ringkasan 7 hari dan analisis pola harian/mingguan.
- **F4 · Ringkasan Konselor (*Counselor Summary*)**: Kompilasi hasil skrining aktif dan minimal 4 hari catatan check-in menjadi ringkasan PDF lokal siap cetak/bagikan.
- **Arsitektur Data Offline-First Ready (ADR-014)**: Skema Room dengan UUID v4, soft-delete, tracking status sinkronisasi, serta kontrak RemoteDataSource & SyncManager yang siap dihubungkan ke backend Retrofit kapan pun tersedia.
- **Pengaturan & Pengingat Lokal**: Konfigurasi alarm pengingat harian (default jam 21.00), izin notifikasi, dan tombol darurat **Hapus Semua Data** dengan konfirmasi 2 langkah.

### Out of Scope (Batasan Eksternal)
- **Tidak ada kewajiban akun daring untuk penggunaan**: Aplikasi tidak memblokir atau memaksa pengguna mendaftar akun cloud untuk dapat menggunakan seluruh fungsionalitas aplikasi.
- **Tidak ada Chatbot AI / Analisis NLP**: Catatan teks bebas tidak dianalisis menggunakan pemrosesan bahasa alami.
- **Tidak ada konsultasi in-app / telekonseling daring**: Aplikasi bukan platform penyedia layanan psikolog langsung.
- **Tidak ada forum komunitas / media sosial internal**: Mencegah perbandingan sosial yang destruktif.
- **Tidak ada integrasi perangkat pintar (*wearable*)**: Tidak ada sinkronisasi smartwatch/fitness tracker.
- **Tidak ada platform iOS atau Web**: Aplikasi dikembangkan eksklusif untuk platform Android.
- **Tidak ada fitur multi-tema / Dark Theme**: Aplikasi menggunakan **satu tema standar yang menenangkan** (*nature-inspired warm light tone*), sehingga tidak memerlukan konfigurasi dark theme.

---

## 6. Functional Requirements

### 6.1 Onboarding & Informed Consent
- **FR-ONB-01**: Aplikasi wajib menampilkan *Splash Screen* (`Selaras · 01 Splash` / `173:2271`) saat pertama kali dibuka, diikuti rangkaian perkenalan fitur (`Selaras · 02 Selamat datang` s.d. `05 Pahami dirimu`).
- **FR-ONB-02**: Menampilkan lembar privasi dan persetujuan (`Selaras · 06 Privasi dan persetujuan` / `173:2301` dan `REVISI Selaras / Pendukung · 02 Persetujuan` / `168:2345`). Pengguna wajib menyetujui *informed consent* sebelum melanjutkan ke pembuatan PIN atau dashboard.
- **FR-ONB-03**: Menghadirkan opsi pembuatan PIN 6-digit (`Selaras · 07 Buat PIN` / `173:2307`) dan konfirmasi ulang (`Selaras · 08 Konfirmasi PIN` / `173:2313`).
- **FR-ONB-04**: Menawarkan pengaturan pengingat check-in harian opsional (`Selaras · 09 Pengingat opsional` / `173:2319`) sebelum diarahkan ke beranda pertama kali (`Selaras · 11 Beranda pertama` / `173:2331`).

### 6.2 App Lock & Pemulihan Akses (Security Flow)
- **FR-SEC-01**: Jika PIN diaktifkan, setiap kali aplikasi dibuka dari kondisi mati atau *background*, aplikasi wajib menampilkan layar kunci (`Selaras · 12 Buka kunci` / `173:2337`).
- **FR-SEC-02**: Memberikan umpan balik visual instan per digit yang dimasukkan (state 1 s.d. 6 digit) dan menampilkan pesan kesalahan jika PIN tidak cocok (`Selaras · 16 PIN tidak cocok`) atau salah verifikasi (`Selaras · 17 PIN salah`).
- **FR-SEC-03**: Menyediakan tombol "Lupa PIN" yang mengarahkan ke alur pemulihan (`REVISI Selaras / Pendukung · 05 Pemulihan PIN` / `168:2348`).
- **FR-SEC-04**: Pengguna dapat memasukkan 16-karakter *Recovery Code* untuk mereset PIN baru (`REVISI Selaras / Pendukung · 06 Reset PIN` / `168:2349`) tanpa menghapus basis data yang tersimpan.

### 6.3 Dashboard / Beranda Utama
- **FR-DSH-01**: Menampilkan sapaan kontekstual, indikator status check-in hari ini (belum check-in pada `F3/Beranda` atau sudah check-in pada `F3/Beranda tercatat`), serta kartu pintasan cepat ke latihan relaksasi dan skrining PHQ-9.
- **FR-DSH-02**: Menempatkan navigasi utama *Bottom Navigation Bar* (`V2/BottomNav`) yang memuat 5 tab: Beranda, Check-in, Skrining, Riwayat, dan Bantuan.
- **FR-DSH-03**: Tab Bantuan selalu aktif dan dapat diakses setiap saat tanpa terblokir oleh status loading fitur lain.

### 6.4 F1 · Skrining PHQ-9
- **FR-F1-01**: Menyediakan 9 butir pertanyaan baku instrumen PHQ-9 dengan rentang opsi respon 0 hingga 3 (Tidak pernah, Beberapa hari, Separuh waktu, Hampir setiap hari).
- **FR-F1-02**: Mendukung mode penapisan **Rutin** dan **Manual**, dengan penegakan kuota mingguan maksimal **1 kali Rutin** dan **1 kali Manual** per minggu kalender (`41:999` s.d. `41:1004`).
- **FR-F1-03**: Menghitung total skor (0–27) dan mengklasifikasikan ke dalam 5 kategori: Minimal (0–4), Ringan (5–9), Sedang (10–14), Sedang Berat (15–19), dan Berat (20–27).
- **FR-F1-04 (Penanganan Krisis)**: Jika butir nomor 9 bernilai $\ge 1$ atau skor total $\ge 20$, sistem wajib menampilkan layar krisis (`F1/Crisis F1` / `41:1020`) yang memprioritaskan kontak darurat (Hotline 119/Sejiwa) sebelum menampilkan skor akhir.
- **FR-F1-05**: Menghubungkan hasil skrining yang telah tervalidasi ke fitur ringkasan konselor (`F1/Handoff ke F4 - Pilihan Ringkasan` / `41:1021`).

### 6.5 F2 · Latihan Relaksasi Pernapasan
- **FR-F2-01**: Menyediakan katalog pola pernapasan bawaan (`F2/Pilih Pola` / `45:999`) dan daftar pola kustom (`F2/Daftar Custom` / `45:1006`).
- **FR-F2-02**: Formulir pembuatan dan pengeditan pola kustom (`F2/Buat Pola` / `45:1008`) dengan validasi durasi tarik dan embus napas dalam satuan detik.
- **FR-F2-03**: Panduan animasi visual pernapasan berirama (fase Tarik, Jeda Tarik, Embus, Jeda Embus) yang dapat dijeda sewaktu-waktu (`WORK · F2 Revisi / Sesi Dijeda` / `122:2499`).

### 6.6 F3 · Daily Check-in & Analisis Pola (Sesuai Revisi Figma `168:2319–2332`)
- **FR-F3-01**: Alur pengisian check-in bertahap (4 langkah):
  1. **Langkah 1 (Mood)**: Pemilihan suasana hati berskala **1–5** (`REVISI Selaras / F3 · 01 Mood` / `168:2319`).
  2. **Langkah 2 (Energi)**: Pemilihan tingkat energi berskala **1–10** (`REVISI Selaras / F3 · 02 Energi` / `168:2320`), di mana 1 = sangat rendah dan 10 = sangat tinggi.
  3. **Langkah 3 (Tidur)**: Pemilihan durasi istirahat tidur malam (`REVISI Selaras / F3 · 03 Durasi tidur` / `168:2321`).
  4. **Langkah 4 (Catatan)**: Input catatan refleksi teks bebas opsional (`REVISI Selaras / F3 · 04 Catatan pribadi` / `168:2322`).
- **FR-F3-02**: Memastikan batas 1 entri per hari (aturan unik tanggal). Jika sudah check-in, layar menampilkan status `REVISI Selaras / F3 · 14 Sudah check-in`.
- **FR-F3-03**: Menghitung ringkasan 7 hari dan analisis tren (`REVISI Selaras / F3 · 07 Ringkasan 7 hari` dan `08 Pola terdeteksi`), serta menangani kondisi data kosong (`11 Belum ada data`) dan proses kalkulasi (`12 Memuat analisis`).

### 6.7 F4 · Ringkasan Konselor (Sesuai Revisi Figma `168:2333–2343`)
- **FR-F4-01**: Pemilihan sumber data untuk dikompilasi (`REVISI Selaras / F4 · 01 Pilih ringkasan`).
- **FR-F4-02 (Syarat Kelayakan 4 Hari)**: Jika data check-in kurang dari 4 hari dalam periode observasi, sistem menampilkan status penolakan `REVISI Selaras / F4 · 02 Analisis belum tersedia` beserta keterangan jumlah hari yang masih dibutuhkan.
- **FR-F4-03**: Menampilkan pratinjau ringkasan lengkap terstruktur (`REVISI Selaras / F4 · 04 Pratinjau lengkap`).
- **FR-F4-04**: Proses rendering dan ekspor ke berkas **PDF lokal** (`REVISI Selaras / F4 · 08 Membuat PDF`), penanganan kegagalan (`09 PDF gagal`), dan status berkas siap dibagikan (`10 PDF siap`).

### 6.8 Pengaturan & Manajemen Data Lokal (`168:2351–2354`)
- **FR-SET-01**: Konfigurasi waktu alarm pengingat harian (`REVISI Selaras / Pendukung · 08 Pengingat` / `168:2351` dan dialog jam `173:7716`).
- **FR-SET-02**: Permintaan izin notifikasi sistem Android (`REVISI Selaras / Pendukung · 09 Izin notifikasi` / `168:2352`).
- **FR-SET-03**: Tombol darurat **Hapus Semua Data** (`REVISI Selaras / Pendukung · 10 Hapus semua data` / `168:2353`) yang mengeksekusi penghapusan database lokal secara menyeluruh dengan konfirmasi 2 langkah (`11 Data dihapus`).

---

## 7. Non-Functional Requirements

| Dimensi | Persyaratan Non-Fungsional |
|---|---|
| **Keamanan Data** | Database SQLite wajib dienkripsi penuh menggunakan **SQLCipher** (AES-256). Kunci enkripsi database (DEK) dilindungi oleh KEK hardware **Android Keystore** (AES-GCM 256-bit). |
| **Kinerja & Responsivitas** | Waktu *startup* dingin (*cold launch*) di bawah 1,5 detik. Transisi antar-layar Compose berjalan pada kecepatan minimal 60 fps tanpa *frame drop*. |
| **Aksesibilitas (A11y)** | Mendukung pembaca layar (*TalkBack*). Target sentuh interaktif minimum berukuran **$48 \times 48\text{ dp}$**. Kontras rasio warna teks minimal 4.5:1. |
| **Ketahanan Data** | Transaksi penyimpanan hasil skrining dan rincian jawaban dilakukan secara atomik (*ACID*). |

---

## 8. Business Rules (Aturan Bisnis Pengikat)

- **K01 · Konteks Spesifikasi**: Ketentuan merujuk pada standar UTP BAB I–II dan dirancang untuk batasan beban kerja tim Selaras.
- **K02 · Syarat Minimal F4**: Pembuatan ringkasan konselor lengkap mensyaratkan **minimal 4 hari catatan check-in** dalam periode observasi.
- **K03 & K04 · Kuota Penapisan Berkala**:
  - Kuota penapisan adalah **1 kali penapisan MANUAL** dan **1 kali penapisan ROUTINE** per minggu kalender (Senin–Minggu).
  - Kedua jenis kuota dihitung secara terpisah dan independen.
- **K05 · Tanpa Restriksi Jeda Artifisial**:
  - Tidak ada jeda paksa 48 jam atau larangan pengisian pada hari-hari yang berurutan selama kuota mingguan masih tersedia.
- **K06 · Instrumen Baku & Non-Diagnostik**:
  - Penapisan hanya menggunakan satu instrumen baku (**PHQ-9**).
  - Pemrosesan skor dilakukan murni secara lokal di perangkat.
  - Hasil penapisan **bukan merupakan penetapan kondisi klinis / diagnosis medis**.
- **K07 · Prioritas Bantuan & Ekspor Sadar**:
  - Informasi bantuan darurat (*helpline*) dapat diakses kapan saja dan wajib diprioritaskan sebelum skor atau saran ditampilkan bila terdeteksi indikasi krisis.
  - Pembuatan atau ekspor berkas keluar aplikasi hanya dapat terjadi atas tindakan sadar dan eksplisit dari pengguna.
- **K08 · Standarisasi Skala Penilaian**:
  - Skala **Suasana Hati (*Mood*)** adalah bilangan bulat **1 s.d. 5**.
  - Skala **Tingkat Energi (*Energy*)** adalah bilangan bulat **1 s.d. 10**.
