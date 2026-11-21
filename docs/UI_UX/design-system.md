# Design System Specification — Selaras

| Metadata | Keterangan |
|---|---|
| **Sumber Desain Figma** | `https://www.figma.com/design/RYBsm8MUhzHTJdAGgLz5jT/Serasi---Android-UI---Design-System` |
| **Node Acuan Utama** | `5:154` (*Handoff/Specification/Board*), `12:178` (*V2 Components*), `45:1059` (*F2 Components*), `168:2320` (*F3 Energi 1–10*), `173:2307` (*PIN Pad & Dots*), `168:2348` (*Recovery Code*) |
| **Kerangka Target** | Android Jetpack Compose dengan Material Design 3 (M3) |
| **Tema Aplikasi** | **Tema Tunggal Standar (*Single Unified Theme*)** — Sesuai ADR-013, mode gelap (*dark theme*) tidak diimplementasikan |
| **Status Dokumen** | **Verified (Diekstrak Langsung via Figma MCP & Terkonfirmasi ERD)** |
| **Versi Dokumen** | 1.1.0 |
| **Terakhir Diperbarui** | 2026-11-20 |

---

## 1. Filosofi Desain (*Design Philosophy*)

Sistem desain **Selaras** dirancang secara khusus untuk aplikasi kesehatan emosional mandiri dan penapisan gejala dengan prinsip-prinsip utama:

1. **Menenangkan & Tidak Mengintimidasi (*Calming & Non-threatening*)**: Menggunakan palet warna netral hangat (*warm neutrals*) yang dipadukan dengan aksen hijau pinus (*pine green*) `#356859` dan latar hangat *off-white* `#F7F7F2`. Menghindari kontras tajam yang dapat memicu ketegangan atau kecemasan pengguna.
2. **Tema Tunggal Konsisten (*Single Unified Theme*)**: Sesuai keputusan produk (ADR-013), Selaras dirancang khusus dengan satu tema tunggal yang lembut dan konsisten. Peniadaan mode gelap (*dark theme*) bertujuan menjaga kontras terapeutik yang terukur, keseragaman pembacaan laporan konselor, serta memangkas kompleksitas state visual.
3. **Kejelasan Status Eksplisit (*Explicit States*)**: Setiap komponen interaktif memiliki 4 kondisi nyata: `Default`, `Disabled`, `Error`, dan `Loading`. Desain melarang penggunaan warna semata untuk menandai error (wajib disertai teks pembantu penjelas).
4. **Skala Penilaian Selaras ERD & PRD**: Skala evaluasi suasana hati (*Mood*) distandarisasi pada rentang **1–5**, dan tingkat energi distandarisasi pada rentang **1–10** (`168:2320`), selaras dengan batasan database Room (`check_ins.energy BETWEEN 1 AND 10`) dan model domain Kotlin.
5. **Aksesibilitas Tanpa Kompromi (*Accessibility First*)**: Memenuhi standar target sentuh minimum $\ge 48\text{ dp}$, kontras teks rasio tinggi terhadap latar belakang, serta integrasi ramah pembaca layar (*TalkBack*).
6. **Pemisahan Jalur Bantuan (*Priority Help Path*)**: Elemen bantuan darurat (*Help/Crisis*) dirancang dengan status independen; tombol bantuan **tidak boleh terkunci** oleh kondisi `Loading` atau `Disabled` pada fitur lain.

---

## 2. Palet Warna (*Color Palette*)

Diekstrak langsung dari definisi token CSS variables Figma (`var(--color/...)`), board spesifikasi handoff (`5:154`), dan sinkronisasi model data ERD:

| Nama Token Figma | Nilai Hex | Peran Semantik & Penggunaan | Pratinjau |
|---|---|---|:---:|
| `color/primary` | `#356859` | Warna aksen utama; tombol *Filled*, ikon aktif, border terpilih, PIN filled dots. | ![#356859](https://via.placeholder.com/15/356859/000000?text=+) `#356859` |
| `color/onprimary` | `#FFFFFF` | Teks dan ikon di atas latar belakang warna *Primary*. | ![#FFFFFF](https://via.placeholder.com/15/FFFFFF/000000?text=+) `#FFFFFF` |
| `color/primarycontainer` | `#DCECE3` | Latar belakang indikator aktif, chip aktif, card terpilih, tombol mood & energi aktif. | ![#DCECE3](https://via.placeholder.com/15/DCECE3/000000?text=+) `#DCECE3` |
| `color/surface` | `#FFFFFF` | Latar belakang kartu, input field, dialog, pin keypad pad, dan bottom navigation bar. | ![#FFFFFF](https://via.placeholder.com/15/FFFFFF/000000?text=+) `#FFFFFF` |
| `color/background` | `#F7F7F2` | Latar belakang kanvas layar utama (nuansa *off-white/cream* hangat menenangkan). | ![#F7F7F2](https://via.placeholder.com/15/F7F7F2/000000?text=+) `#F7F7F2` |
| `color/text` | `#243B35` / `#20282C` | Warna teks utama (*Headline*, *Title*, teks isi kartu, angka keypad PIN). | ![#243B35](https://via.placeholder.com/15/243B35/000000?text=+) `#243B35` |
| `color/muted` | `#52675F` | Teks sekunder, label pembantu (*helper*), ikon navigasi tidak aktif, indikator netral. | ![#52675F](https://via.placeholder.com/15/52675F/000000?text=+) `#52675F` |
| `color/outline` | `#7B9187` | Garis tepi (*border*) tombol *Outlined*, TextField, pembatas kartu, PIN empty dots. | ![#7B9187](https://via.placeholder.com/15/7B9187/000000?text=+) `#7B9187` |
| `color/disabled` | `#E2E8E3` | Latar belakang tombol atau kontainer dalam kondisi nonaktif (*disabled*). | ![#E2E8E3](https://via.placeholder.com/15/E2E8E3/000000?text=+) `#E2E8E3` |
| `color/disabledtext` | `#596B62` | Teks pada elemen nonaktif (tetap memenuhi rasio keterbacaan WCAG). | ![#596B62](https://via.placeholder.com/15/596B62/000000?text=+) `#596B62` |
| `color/error` | `#B3261E` | Warna status kegagalan, pesan validasi, PIN salah (`173:2367`), peringatan krisis. | ![#B3261E](https://via.placeholder.com/15/B3261E/000000?text=+) `#B3261E` |
| `color/errorcontainer` | `#F9DEDC` | Latar belakang kartu notifikasi kesalahan (*Error Card/Banner*). | ![#F9DEDC](https://via.placeholder.com/15/F9DEDC/000000?text=+) `#F9DEDC` |

### Warna Aksen Pendukung Tambahan (Spesifikasi Handoff `5:158`)
- **Sage**: `#E0EDE4` (Digunakan pada kontainer latihan relaksasi dan penanda status ringan).
- **Lavender**: `#EEE5F5` (Digunakan pada penanda status malam / durasi tidur).
- **Support / Warm Tint**: `#FFF0E5` (Digunakan pada banner bantuan darurat krisis).

> [!NOTE]
> **Kebijakan Tema Tunggal (*Single Theme Policy*)**: Sesuai arahan teknis dan produk (ADR-013), aplikasi **tidak mengimplementasikan mode gelap (*Dark Theme*)**. Seluruh komponen UI pada aplikasi Android hanya menggunakan palet tema standar di atas untuk menjaga konsistensi suasana menenangkan (*calming atmosphere*) dan memastikan legibilitas dokumen pratinjau konselor.

---

## 3. Tipografi (*Typography Scale*)

Keluarga font utama yang digunakan adalah **Roboto**. Seluruh ukuran, bobot (*font weight*), dan tinggi baris (*line height*) terdefinisi sebagai berikut:

| Style Name | Font Family | Weight | Size | Line Height | Penggunaan Umum |
|---|---|---|---|---|---|
| **Display** | Roboto | Regular (400) | `36px` (`36sp`) | `44px` | Judul bab besar, angka skor akhir skrining PHQ-9. |
| **Headline** | Roboto | Regular (400) | `28px` (`28sp`) | `36px` | Judul utama layar (*TopAppBar*, judul fitur). |
| **Title** | Roboto | Medium (500) | `22px` (`22sp`) | `28px` | Judul sub-fitur, judul dialog, judul seksi formulir. |
| **Subtitle** | Roboto | Medium (500) | `18px` (`18sp`) | `24px` | Judul kartu (*Card Title*), sub-seksi pengaturan. |
| **Body** | Roboto | Regular (400) | `16px` (`16sp`) | `24px` | Teks paragraf, butir pertanyaan PHQ-9, input jurnal bebas. |
| **Small** | Roboto | Regular (400) | `14px` (`14sp`) | `20px` | Teks pembantu (*helper*), deskripsi kartu, catatan kaki. |
| **Label** | Roboto | Medium (500) | `14px` (`14sp`) | `20px` | Teks tombol (*Button*), label input, tab navigasi, angka keypad. |

---

## 4. Spacing, Radius, & Ukuran Target Sentuh

### 4.1 Skala Spacing
Diekstrak dari token `var(--space/...)`:
- `space/0`: `0dp` (`0px`)
- `space/4`: `4dp` (`4px`) — Jarak antar teks label dan input, padding mikro.
- `space/8`: `8dp` (`8px`) — Jarak antar ikon dan teks dalam tombol, jarak antar titik PIN dots.
- `space/12`: `12dp` (`12px`) — Jarak antar tombol opsi, padding chip.
- `space/16`: `16dp` (`16px`) — Padding horizontal standar layar, padding internal kartu, margin keypad PIN.
- `space/24`: `24dp` (`24px`) — Padding board handoff, jarak vertikal antar grup formulir.
- `space/32`: `32dp` (`32px`) — Margin pemisah seksi utama layar.

### 4.2 Skala Border Radius
Diekstrak dari token `var(--radius/...)`:
- `radius/12`: `12dp` (`12px`) — Sudut melengkung untuk `TextField`, `Chip`, tombol angka energi.
- `radius/16`: `16dp` (`16px`) — Sudut melengkung untuk `Card`, indikator tab, modal konfirmasi, dialog PIN recovery.
- `radius/24`: `24dp` (`24px`) — Sudut melengkung penuh untuk `Button` dan `PatternCard`.
- `radius/full`: `999dp` — Lingkaran penuh untuk PIN dots indikator (`16dp` x `16dp`) dan tombol navigasi bulat.

### 4.3 Target Sentuh & Interaksi
- **Target Sentuh Minimum**: **$\ge 48\text{ dp}$** (Tinggi tombol standar adalah `48dp` hingga `52dp`; setiap tombol angka keypad PIN berukuran minimal `56 × 56 dp`).
- **State Feedback Layer**:
  - **Hover**: State layer transparansi `8%`.
  - **Focus**: Garis luar (*outline*) selebar `2dp`.
  - **Pressed**: State layer transparansi `12%`.

---

## 5. Ikonografi (*Iconography*)

Selaras menggunakan set ikon **Material Symbols Rounded** dengan ukuran standar `24 × 24 dp` (`FILL = 0`, `GRAD = 0`):

| Nama Ikon | Simbol / Karakter | Konteks Penggunaan |
|---|---|---|
| `home` | Beranda | Tab navigasi Beranda / Dashboard. |
| `edit_note` | Catatan Check-in | Tab navigasi Daily Check-in harian. |
| `fact_check` | Pemeriksaan | Tab navigasi Skrining PHQ-9. |
| `history` | Riwayat | Tab navigasi Riwayat catatan & skrining. |
| `support_agent` | Agen Bantuan | Tab navigasi Bantuan & Kontak Krisis. |
| `air` | Udara / Napas | Ikon kartu latihan relaksasi pernapasan F2. |
| `lock` | Gembok | Ikon layar kunci (*App Lock*) dan status keamanan PIN. |
| `key` | Kunci | Ikon pembuatan PIN dan kode pemulihan (*Recovery Code*). |
| `notifications` | Lonceng Pengingat | Ikon preferensi jadwal pengingat check-in. |
| `delete_forever` | Hapus Permanen | Ikon tombol tindakan berbahaya "Hapus Semua Data". |
| `content_copy` | Salin Berkas | Ikon tindakan salin kode pemulihan (*Copy Recovery Code*). |
| `check_circle` | Sukses | Indikator keberhasilan penyimpanan data dan PIN cocok. |
| `error` | Kesalahan | Indikator kegagalan operasi dan PIN salah. |
| `backspace` | Hapus Karakter | Tombol hapus digit pada keypad numerik PIN. |
| `progress_activity` | Indikator Putar | Indikator memuat (*loading spinner*) pada tombol dan layar proses. |
| `radio_button_checked` | Radio Terpilih | Status opsi pola pernapasan dan butir PHQ-9 terpilih. |
| `radio_button_unchecked`| Radio Kosong | Status opsi tidak terpilih. |

---

## 6. Inventaris Komponen UI Figma (*Component Inventory*)

Seluruh komponen lokal dibangun sebagai *Component Sets* mandiri:

### 6.1 `V2/Button` (Node ID: `12:178`)
- **Dimensi**: Lebar `296dp` – `361dp`, Tinggi `48dp` – `52dp`, Radius `24dp`.
- **Properties**:
  - `Style`: `Filled`, `Outlined`, `Text`
  - `State`: `Default`, `Disabled`, `Error`, `Loading`
- **Total Varian**: **12 Varian**.
- **Karakteristik**:
  - Varian `Loading` menampilkan teks label bersama ikon `progress_activity`.
  - Varian `Disabled` menggunakan latar `#E2E8E3` dan teks `#596B62`.
  - Varian `Error` menggunakan latar/border `#B3261E`.

### 6.2 `V2/Card` (Node ID: `12:206`)
- **Dimensi**: Lebar `328dp` – `361dp`, Tinggi fleksibel, Radius `16dp`, Padding `16dp`.
- **Properties**:
  - `State`: `Default`, `Disabled`, `Error`, `Loading`
- **Total Varian**: **4 Varian**.
- **Karakteristik**:
  - Memuat elemen `title` (Roboto Medium 18sp) dan `body` (Roboto Regular 14sp).
  - Varian `Error` mengubah latar belakang menjadi `#F9DEDC` (*errorcontainer*).

### 6.3 `V2/TextField` (Node ID: `12:219`)
- **Dimensi**: Lebar `328dp` – `361dp`, Tinggi `106dp`, Radius `12dp`, Padding `16dp`.
- **Properties**:
  - `State`: `Default`, `Disabled`, `Error`, `Loading`
- **Total Varian**: **4 Varian**.
- **Karakteristik**:
  - Memuat 3 baris: `Label` (14sp Muted), `Value` (16sp Text), dan `Helper Text` (14sp Muted/Error).
  - Border 1px solid (`#7B9187` pada kondisi normal, `#B3261E` pada kondisi error).

### 6.4 `V2/Chip` (Node ID: `12:236`)
- **Dimensi**: Lebar `296dp`, Tinggi `44dp`, Radius `12dp`, Padding `12dp`.
- **Properties**:
  - `State`: `Default`, `Disabled`, `Error`, `Loading`
- **Total Varian**: **4 Varian**.
- **Penggunaan**: Indikator status tahapan, kategori skor PHQ-9, atau filter riwayat.

### 6.5 `V2/BottomNav` (Node ID: `12:245`)
- **Dimensi**: Lebar `360dp` – `393dp`, Tinggi `80dp`, Padding Vertikal `8dp`.
- **Properties**:
  - `Active`: `Beranda`, `Check-in`, `Skrining`, `Riwayat`, `Bantuan`
  - `State`: `Default`, `Disabled`, `Error`, `Loading`
- **Total Varian**: **20 Varian**.
- **Karakteristik**:
  - Setiap tab berukuran lebar `72dp` dan tinggi `64dp`.
  - Tab aktif menampilkan indikator latar pil melengkung (`#DCECE3`) dengan teks dan ikon berwarna `#356859`.

### 6.6 `F2/PatternCard` (Node ID: `45:1059`)
- **Dimensi**: Lebar `361dp`, Tinggi `88dp`, Radius `24dp`, Padding `16dp`.
- **Properties**:
  - `Selected`: `true`, `false`
- **Total Varian**: **2 Varian**.
- **Karakteristik**:
  - Memuat ikon `air`, judul pola pernapasan, durasi ("Tarik X dtk · Embus Y dtk"), dan radio button.
  - Varian `Selected=true` menggunakan latar `#DCECE3` dan border `#356859`.

### 6.7 Komponen F3: Selektor Energi 1–10 (Node ID: `168:2320`)
- **Dimensi & Tata Letak**: Grid 10 tombol angka tersusun dalam 2 baris (5 kolom per baris: angka 1–5 di baris atas, angka 6–10 di baris bawah).
- **Target Sentuh**: Setiap tombol memiliki ukuran minimum $56 \times 48\text{ dp}$ dengan radius `12dp`.
- **Properties & States**:
  - `Default (Unselected)`: Latar `Surface` (`#FFFFFF`), border outline 1px (`#7B9187`), teks angka `#243B35` (Roboto Medium 16sp).
  - `Selected`: Latar `PrimaryContainer` (`#DCECE3`), border solid 2px `Primary` (`#356859`), teks angka `#243B35` (Roboto Medium 16sp).
- **Keterangan Semantik**:
  - Angka **1** = Sangat lelah / Tanpa energi (*Exhausted*).
  - Angka **10** = Sangat berenergi / Penuh semangat (*Full of Energy*).
- **Kesesuaian**: Selaras 1:1 dengan batasan database `check_ins.energy BETWEEN 1 AND 10` dan model Kotlin `require(energy in 1..10)`.

### 6.8 Komponen F3: Selektor Mood 1–5 (Node ID: `168:2319`)
- **Tata Letak**: 5 kartu opsi suasana hati tersusun vertikal/horizontal:
  - 1 = Sangat Kurang Baik / Buruk
  - 2 = Kurang Baik
  - 3 = Netral / Cukup
  - 4 = Baik
  - 5 = Sangat Baik
- **States**:
  - `Default`: Latar `#FFFFFF`, border `#7B9187`.
  - `Selected`: Latar `#DCECE3`, border `#356859`, indikator centang hijau.

### 6.9 Komponen App Lock: PIN Input Dots & Numeric Keypad (Node ID: `173:2307`, `173:2313`, `173:2337`)
- **PIN Indicator Dots (6 Digit)**:
  - 6 lingkaran terpisah selebar `16dp × 16dp`, jarak antar titik `12dp`.
  - `Empty Dot`: Lingkaran outline border 2px `#7B9187`.
  - `Filled Dot`: Lingkaran solid penuh berwarna `#356859`.
  - `Error State` (`173:2361`, `173:2367`): Seluruh 6 titik berubah warna menjadi `#B3261E` dengan efek animasi getar (*shake feedback*).
- **Numeric Keypad**:
  - Grid 3 kolom × 4 baris memuat angka 1 sampai 9, baris terbawah berisi placeholder kosong, angka 0, dan tombol ikon `backspace`.
  - Ukuran target sentuh tiap tombol angka: minimal `64 × 64 dp`, bentuk lingkaran/rounded.
  - Teks angka: Roboto Regular 24sp `#243B35`.

### 6.10 Komponen Recovery Code Card (Node ID: `168:2348`)
- **Dimensi**: Lebar `361dp`, Radius `16dp`, Padding `16dp`, Latar `Surface` (`#FFFFFF`).
- **Elemen Teks**: Kode 16 karakter alfanumerik diformat menjadi 4 segmen (`XXXX-XXXX-XXXX-XXXX`) dengan font monospace/Roboto Medium berjarak antar-huruf (*letter-spacing*).
- **Aksi Cepat**: Tombol *Outlined* "Salin Kode" dengan ikon `content_copy` yang memicu penyalinan ke clipboard Android dan menampilkan snackbar konfirmasi.

### 6.11 Komponen Settings: Reminder Switch & Two-step Destruction (Node ID: `168:2351`, `168:2353`)
- **Reminder Preference Card**:
  - Kartu berisi Switch M3 on/off untuk aktivasi pengingat harian.
  - Saat aktif, menampilkan baris pemilih jam (contoh: `21.00`, frame `173:7716`) dengan dialog TimePicker standar M3.
- **Two-step Data Destruction Modal**:
  - Langkah 1: Tombol berwarna merah peringatan `Error` (`#B3261E`) "Hapus Semua Data".
  - Langkah 2: Dialog konfirmasi bahaya dengan peringatan eksplisit bahwa data tidak dapat dipulihkan, serta tombol konfirmasi pengetikan kata sandi/PIN sebelum eksekusi `clearAllTables()`.

---

## 7. Pemetaan Design Tokens ke Jetpack Compose Material 3

Implementasi tema tunggal pada file `core/ui/theme/Theme.kt`:

```kotlin
package id.selaras.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Palet Tema Tunggal Selaras (Single Unified Theme — ADR-013)
private val SelarasColorScheme = lightColorScheme(
    primary = Color(0xFF356859),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCECE3),
    onPrimaryContainer = Color(0xFF243B35),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF243B35),
    surfaceVariant = Color(0xFFE2E8E3),
    onSurfaceVariant = Color(0xFF52675F),
    background = Color(0xFFF7F7F2),
    onBackground = Color(0xFF20282C),
    outline = Color(0xFF7B9187),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFFB3261E)
)

// Tipografi Selaras
val SelarasTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)

@Composable
fun SelarasTheme(
    content: @Composable () -> Unit
) {
    // Mode gelap dinonaktifkan: aplikasi selalu menggunakan SelarasColorScheme tunggal
    MaterialTheme(
        colorScheme = SelarasColorScheme,
        typography = SelarasTypography,
        content = content
    )
}
```
