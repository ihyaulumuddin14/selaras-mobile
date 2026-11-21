# Architecture & Product Decision Records (ADR) — Selaras

Dokumen ini mencatat seluruh rekaman keputusan teknis, arsitektur, dan arah produk aplikasi **Selaras**. Setiap keputusan memuat konteks, alternatif yang dipertimbangkan, konsekuensi, dan status persetujuannya.

---

## 1. Ringkasan Status Keputusan

| ID | Judul Keputusan | Status | Kategori |
|---|---|---|---|
| **ADR-001** | Enkripsi Database Lokal Menggunakan SQLCipher & Keystore | **Accepted** | Keamanan / Data |
| **ADR-002** | Pemisahan Kunci Database (DEK) dari PIN Pengguna | **Accepted** | Keamanan / Auth |
| **ADR-003** | Mekanisme Pemulihan Akses via Recovery Code 16 Karakter | **Accepted** | Keamanan / UX |
| **ADR-004** | Penerapan Clean Architecture dengan Use Case Granular | **Accepted** | Arsitektur Kode |
| **ADR-005** | Pola Singleton Row untuk Tabel `app_settings` | **Accepted** | Basis Data |
| **ADR-006** | Indeks Komposit Unik untuk Pembatasan Kuota Skrining Mingguan | **Accepted** | Basis Data / Bisnis |
| **ADR-007** | Transaksi Atomik dan Cascade Delete pada Skrining PHQ-9 | **Accepted** | Basis Data / Integritas |
| **ADR-008** | Snapshot JSON Statis untuk Berkas Ringkasan Konselor | **Accepted** | Bisnis / Data |
| **ADR-009** | Pendekatan Offline-First Tanpa Kewajiban Registrasi Cloud | **Amended** | Produk / Privasi |
| **ADR-010** | Standarisasi Skala Energi (1–10) dan Skala Mood (1–5) | **Accepted** | Produk / UI/UX |
| **ADR-011** | Strategi Dependency Injection dengan Manual DI (AppContainer) | **Accepted** | Arsitektur / DI |
| **ADR-012** | Implementasi Type-Safe Navigation Compose 2.8+ | **Accepted** | Antarmuka / Navigasi |
| **ADR-013** | Penetapan Tema Tunggal (*Single Theme*) Tanpa Mode Gelap | **Accepted** | Antarmuka / UI |
| **ADR-014** | Desain Skema dan Arsitektur Offline-First Ready (UUID, Soft-Delete, Sync Engine) | **Accepted** | Arsitektur / Sinkronisasi |

---

## 2. Keputusan yang Telah Dikonfirmasi (*Accepted Decisions*)

### ADR-001: Enkripsi Database Lokal Menggunakan SQLCipher & Android Keystore
- **Status**: **Accepted**
- **Konteks**: Aplikasi Selaras menyimpan data yang sangat privat, mencakup catatan suasana hati harian, durasi tidur, dan jawaban penapisan depresi PHQ-9. Pada perangkat Android yang telah di-root atau melalui analisis backup ADB fisik, file SQLite standar dapat dibaca tanpa hambatan jika tidak dienkripsi.
- **Keputusan**:
  Mengintegrasikan pustaka native **SQLCipher for Android** (`net.zetetic:sqlcipher-android:4.20.0`) melalui `SupportOpenHelperFactory` pada Room Database. Kunci Database Encryption Key (DEK) berupa 256-bit acak dilindungi oleh Key Encryption Key (KEK) yang tersimpan di dalam perangkat keras **Android Keystore** (`AES/GCM/NoPadding`).
- **Alternatif yang Dipertimbangkan**:
  1. *Room Database Standar tanpa Enkripsi*: Ditolak karena melanggar prinsip perlindungan data medis/kesehatan mental.
  2. *Enkripsi Kolom Per Kolom (Field-level encryption)*: Ditolak karena rumit, memperlambat kueri agregasi, dan tetap mengekspos metadata tabel serta indeks.
- **Konsekuensi**:
  - *Positif*: Keamanan data setara perbankan; file database `selaras.db` yang disalin paksa dari disk tidak dapat dibaca tanpa DEK.
  - *Negatif*: Penambahan ukuran binary aplikasi sekitar 4–8 MB karena pustaka shared library (`.so`) SQLCipher; sedikit overhead performa I/O baca-tulis database.

---

### ADR-002: Pemisahan Kunci Database (DEK) dari PIN Pengguna
- **Status**: **Accepted**
- **Konteks**: Pada banyak aplikasi keamanan rendah, passphrase pengguna/PIN langsung dijadikan passphrase database SQLCipher. Jika pengguna lupa PIN atau ingin mengganti PIN, seluruh basis data harus didekripsi dan dienkripsi ulang (*re-keying*) yang berisiko merusak data jika proses terputus di tengah jalan.
- **Keputusan**:
  Memisahkan peran kunci:
  - **DEK (Database Encryption Key)**: Kunci acak kriptografis 256-bit independen yang dibuat sekali saat aplikasi pertama kali diinisialisasi dan dienkripsi oleh KEK Keystore.
  - **PIN Pengguna**: Hanya bertindak sebagai kunci gerbang antarmuka (*UI App Lock*). PIN di-hash menggunakan **PBKDF2WithHmacSHA256** (10.000 iterasi dengan salt 16-byte acak) melalui `PinManager`.
- **Alternatif yang Dipertimbangkan**:
  - Menurunkan kunci DEK dari PIN pengguna via PBKDF2: Ditolak karena jika pengguna lupa PIN, basis data akan hilang selamanya tanpa kemungkinan pemulihan.
- **Konsekuensi**:
  - *Positif*: Pengguna dapat mengganti PIN sewaktu-waktu tanpa perlu melakukan re-key database; basis data tetap aman dari akses fisik.
  - *Negatif*: Membutuhkan pengelolaan dua komponen keamanan terpisah (`KeyManager` dan `PinManager`).

---

### ADR-003: Mekanisme Pemulihan Akses via Recovery Code 16 Karakter
- **Status**: **Accepted**
- **Konteks**: Karena aplikasi beroperasi tanpa kebergantungan server otentikasi eksternal, pengguna yang lupa PIN membutuhkan cara mandiri untuk memulihkan akses tanpa mereset database.
- **Keputusan**:
  Menyediakan kode pemulihan (*Recovery Code*) 16-karakter alfanumerik yang dihasilkan saat onboarding (dikelompokkan 4x4, contoh: `ABCD-EFGH-JKLM-NPQR`).
  - Karakter ambigu seperti `0`, `O`, `1`, `I`, `L` dieliminasi untuk mencegah kesalahan baca/tulis.
  - Kode di-hash dengan PBKDF2 dan salt untuk verifikasi.
  - Salinan cadangan DEK dienkripsi menggunakan kunci yang diturunkan dari kode pemulihan tersebut melalui AES-GCM (`RecoveryManager`).
- **Konsekuensi**:
  - *Positif*: Pengguna memiliki jalur darurat pemulihan data lokal tanpa keterlibatan pihak ketiga.
  - *Negatif*: Pengguna wajib mencatat dan menyimpan kode pemulihan dengan cermat di luar perangkat.

---

### ADR-004: Penerapan Clean Architecture dengan Use Case Granular
- **Status**: **Accepted**
- **Konteks**: Logika bisnis Selaras memiliki aturan medis dan agregasi yang ketat (seperti batas kuota skrining mingguan, aturan kelayakan minimal 4 hari check-in, dan penanganan skor depresi). Menggabungkan logika ini ke dalam ViewModel atau Repository akan membuat kode sulit diuji dan dipelihara.
- **Keputusan**:
  Menerapkan Clean Architecture dengan 4 layer jelas:
  1. **Presentation Layer**: UI Jetpack Compose dan ViewModel.
  2. **Domain Layer**: Model bisnis murni dan Use Case berprinsip tanggung jawab tunggal (*Single Responsibility Principle*).
  3. **Data Layer**: Repositori konkret, Data Source, Entity, dan DAO.
  4. **Core Layer**: Utilitas basis data, kriptografi, tema, dan navigasi.
- **Konsekuensi**:
  - *Positif*: Setiap aturan bisnis dapat diuji unit secara terisolasi tanpa framework Android; kode modular dan mudah diperluas.
  - *Negatif*: Memerlukan lebih banyak berkas kelas (*boilerplate*) dan pemetaan objek (*mappers*).

---

### ADR-005: Pola Singleton Row untuk Tabel `app_settings`
- **Status**: **Accepted**
- **Konteks**: Pengaturan aplikasi seperti status onboarding, status persetujuan (*consent*), dan jam pengingat harian hanya membutuhkan satu baris status aktif untuk perangkat tersebut.
- **Keputusan**:
  Mengunci Primary Key tabel `app_settings` pada konstanta tunggal `id = 1L` (`SINGLETON_ID`). Kueri pembaruan menggunakan strategi `OnConflictStrategy.REPLACE` via fungsi `upsertSettings()`.
- **Konsekuensi**:
  - *Positif*: Menjamin tidak ada duplikasi entri preferensi; seluruh status aplikasi terpusat di dalam database terenkripsi.
  - *Negatif*: Sedikit overhead kueri database untuk membaca status pengingat sederhana.

---

### ADR-006: Indeks Komposit Unik untuk Pembatasan Kuota Skrining Mingguan
- **Status**: **Accepted**
- **Konteks**: Aturan bisnis K03–K04 menetapkan bahwa pengguna hanya dapat melakukan maksimal 1 kali penapisan Manual dan 1 kali penapisan Rutin dalam satu rentang minggu pengamatan (14 hari cakupan).
- **Keputusan**:
  Memasang indeks komposit unik pada tabel `screening_results`:
  `Index(value = ["screening_type", "period_start_date", "period_end_date"], unique = true)`.
- **Konsekuensi**:
  - *Positif*: Penegakan aturan bisnis dijamin secara mutlak di tingkat mesin basis data (SQLite), mencegah celah *race condition* jika pengguna menekan tombol selesai berkali-kali.
  - *Negatif*: Repository dan UI wajib menangani `SQLiteConstraintException` secara elegan dan menginformasikan status kuota kepada pengguna.

---

### ADR-007: Transaksi Atomik dan Cascade Delete pada Skrining PHQ-9
- **Status**: **Accepted**
- **Konteks**: Skrining PHQ-9 terdiri dari satu data induk ringkasan skor (`screening_results`) dan tepat sembilan data anak butir jawaban (`screening_answers`). Kegagalan salah satu penyimpanan akan merusak integritas data klinis.
- **Keputusan**:
  1. Proses penyimpanan kedua entitas wajib dibungkus dalam blok atomik `database.withTransaction { ... }` pada `ScreeningRepositoryImpl`.
  2. Relasi *foreign key* `screening_answers` dan `counselor_summaries` terhadap `screening_results` dikonfigurasikan dengan `onDelete = ForeignKey.CASCADE`.
- **Konsekuensi**:
  - *Positif*: Jika terjadi *force close* atau kegagalan sistem, seluruh transaksi dibatalkan (*rollback*); penghapusan hasil skrining otomatis membersihkan seluruh rincian jawaban anak.
  - *Negatif*: Operasi penulisan membutuhkan penguncian sementara (*write lock*) pada database.

---

### ADR-008: Snapshot JSON Statis untuk Berkas Ringkasan Konselor
- **Status**: **Accepted**
- **Konteks**: Ringkasan Konselor (F4) dibuat pada suatu titik waktu berdasarkan data check-in dan skrining saat itu. Jika data masa lalu tersebut di kemudian hari diedit atau dihapus oleh pengguna, ringkasan masa lalu yang sudah pernah dibagikan kepada konselor tidak boleh berubah nilainya.
- **Keputusan**:
  Menyimpan seluruh data agregasi ringkasan sebagai dokumen statis string JSON pada kolom `counselor_summaries.snapshot_json`.
- **Konsekuensi**:
  - *Positif*: Integritas riwayat rekam medis mandiri terjamin (*immutable historical record*).
  - *Negatif*: Redundansi data tersimpan dalam format teks JSON di dalam basis data.

---

### ADR-009: Pendekatan Offline-First Tanpa Kewajiban Registrasi Cloud
- **Status**: **Amended oleh ADR-014**
- **Konteks**: Sesuai arahan handoff `5:164` (*Privasi & batas MVP*), aplikasi tidak boleh mewajibkan akun cloud atau memblokir operasional saat offline demi kenyamanan psikologis pengguna.
- **Keputusan (Awal)**:
  Seluruh alur aplikasi berjalan 100% secara lokal tanpa server eksternal.
- **Amandemen**:
  Aplikasi tetap dapat digunakan 100% secara offline tanpa registrasi (*fully offline functional*), namun arsitektur data ditingkatkan menjadi **Offline-First Ready** (ADR-014). Ini memungkinkan penambahan sinkronisasi cloud opsional ke backend Node.js + Express.js + PostgreSQL tanpa perlu merombak model domain atau antarmuka Compose.
- **Konsekuensi**:
  - *Positif*: Privasi pengguna terjamin saat offline; pengguna di masa depan dapat menikmati pencadangan data multi-device jika memilih terhubung ke server.

---

### ADR-010: Standarisasi Skala Penilaian Energi (1–10) dan Suasana Hati (1–5)
- **Status**: **Accepted (Dikonfirmasi)**
- **Konteks**: Penyelarasan skala input antara UI Figma revisi (`168:2320`), domain model `CheckIn.kt`, dan Room Entity `CheckInEntity.kt`.
- **Keputusan**:
  1. Skala **Suasana Hati (*Mood*)** dibakukan pada rentang **1–5** (1 Sangat Kurang Baik s.d. 5 Sangat Baik).
  2. Skala **Tingkat Energi (*Energy*)** dibakukan pada rentang **1–10** (1 Sangat Rendah s.d. 10 Sangat Tinggi).
- **Konsekuensi**:
  - *Positif*: Integritas data dari UI hingga database konsisten penuh tanpa konversi matematis perantara.

---

### ADR-011: Strategi Dependency Injection dengan Manual DI (AppContainer)
- **Status**: **Accepted**
- **Keputusan**:
  Menggunakan pola **Manual Dependency Injection** berbasis antarmuka terpusat **`AppContainer`** (dan implementasi `DefaultAppContainer`) yang diinisialisasi pada tingkat `SelarasApplication`.
- **Alasan & Rasional**:
  - Menghindari penambahan pustaka pihak ketiga dan kerumitan konfigurasi KSP/Gradle (seperti Hilt/Dagger).
  - Transparansi penuh atas siklus hidup objek dependensi.
- **Konsekuensi**:
  - *Positif*: Waktu kompilasi cepat, eksplisit, dan minim boilerplate runtime.
  - *Kewajiban*: Setiap penambahan repositori/manager baru wajib didaftarkan di `AppContainer`.

---

### ADR-012: Implementasi Type-Safe Navigation Compose 2.8+
- **Status**: **Accepted**
- **Keputusan**:
  Menggunakan pustaka resmi **AndroidX Navigation Compose 2.8+** dengan rute bertipe aman (*Type-Safe Routes*) berbasis **Kotlin Serialization** (`@Serializable`) yang terpusat di `core/navigation/AppNavigation.kt`.
- **Konsekuensi**:
  - *Positif*: Keamanan tipe rute saat compile-time; navigasi bebas typo string.

---

### ADR-013: Penetapan Tema Tunggal (*Single Theme*) Tanpa Mode Gelap
- **Status**: **Accepted**
- **Keputusan**:
  Menggunakan satu tema terstandar bernuansa alam (*warm neutrals*, *pine green*, dan *sage*) pada `core/ui/theme/Theme.kt` tanpa opsi mode gelap.
- **Konsekuensi**:
  - *Positif*: Memangkas kompleksitas pemeliharaan visual dan menjaga kontras terapeutik yang konsisten.

---

### ADR-014: Desain Skema dan Arsitektur Offline-First Ready (UUID, Soft-Delete, Sync Engine)
- **Status**: **Accepted**
- **Konteks**:
  Aplikasi Selaras dirancang untuk dapat beralih dari aplikasi lokal mandiri menuju aplikasi terhubung (*connected app*) dengan backend server (Node.js + Express.js + PostgreSQL) secara mulus. Jika skema database lokal hanya menggunakan autoincrement integer dan penghapusan data secara keras (*hard delete*), proses sinkronisasi masa depan akan mengalami benturan identitas (*ID collisions*) dan kehilangan riwayat penghapusan (*deleted records replication failure*).
- **Keputusan**:
  1. **Universal Unique Identifier (UUID v4)**: Setiap entitas sinkronisasi (`check_ins`, `custom_breathing_patterns`) wajib memiliki kolom `uuid: String` berindeks unik di samping `id: Long` lokal.
  2. **Identitas Pengguna Opsional (`user_id`)**: Menambahkan kolom `user_id: String? = null` untuk penautan akun saat autentikasi cloud diaktifkan.
  3. **Kolom Status Sinkronisasi (`sync_status`)**: Setiap entri memiliki status `SYNCED`, `PENDING_CREATE`, `PENDING_UPDATE`, atau `PENDING_DELETE`.
  4. **Pola Soft-Delete**: Operasi hapus data menandai `is_deleted = 1` dan `sync_status = 'PENDING_DELETE'`. Kueri aktif memfilter `WHERE is_deleted = 0`.
  5. **Pembersihan Tombstone (*Tombstone Purge*)**: Data yang telah dihapus di server akan di-hard delete dari SQLite lokal oleh SyncManager untuk menjaga efisiensi penyimpanan lokal.
  6. **Pemisahan Sumber Data di Data Layer**:
     - `LocalDataSource`: Pembungkus Room DAO terenkripsi (Single Source of Truth).
     - `RemoteDataSource`: Kontrak antarmuka ke REST API backend.
     - `SyncManager`: Orkestrator sinkronisasi dua arah dengan resolusi konflik *Last-Write-Wins (LWW)* berbasis `updated_at`.
- **Konsekuensi**:
  - *Positif*: Kesiapan 100% integrasi backend masa depan tanpa perlu migrasi destruktif, refactoring domain model, maupun modifikasi UI Compose.
  - *Kewajiban*: Versi skema Room dinaikkan ke versi 3; pengujian unit wajib mencakup skenario sinkronisasi push, pull, dan offline resilience.

---

## 3. Status Rekomendasi Terbuka

Seluruh **14 Architecture Decision Records (ADR-001 s.d. ADR-014)** telah disepakati dan diterapkan secara konsisten pada basis kode Selaras.
