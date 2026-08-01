# Minum.ku 💧☕ — Manajemen Hidrasi & Kopi

Aplikasi Android *offline-first* modern berbasis **Jetpack Compose** untuk memantau asupan air minum harian dan konsumsi kopi/kafein. Aplikasi dilengkapi dengan penyimpanan lokal **Room Database**, pengingat pintar via **WorkManager**, serta opsi sinkronisasi cloud dengan **Supabase**.

---

## 🌟 Fitur Utama

- 💧 **Pencatatan Asupan Air Harian**: Catat konsumsi air minum (ml) secara akurat dengan indikator kemajuan (progress ring) interaktif dan target harian.
- 📱 **Home Screen AppWidget**: Widget layar utama Android untuk pencatatan cepat (+250ml, +500ml air, & +kopi) secara instan tanpa perlu membuka aplikasi.
- ⚡ **Tombol Akses Cepat & Bottom Sheet**: Floating Action Button (FAB) "+ Tambah Air" yang membuka Bottom Sheet berisi takaran presisi instan (+150ml, +250ml, +330ml, +500ml, +750ml, +1.000ml).
- 👈 **Swipe-to-Delete**: Geser item ke kiri atau ke kanan di daftar riwayat untuk menghapus entri yang salah secara instan.
- ☕ **Pencatatan Kopi & Kafein**: Pantau kadar kafein harian dari berbagai varian kopi (Espresso, Latte, Cappuccino, dll).
- 📶 **Offline-First & Auto Sync**: Seluruh data disimpan secara lokal di SQLite/Room terlebih dahulu. Saat terhubung ke internet & Supabase, data disinkronkan di latar belakang.
- 🟢 **Indikator Koneksi Supabase**: Status koneksi real-time di bagian kanan atas Dashboard menunjukkan apakah aplikasi aktif terhubung ke Supabase.
- 🔔 **Pengingat Minum Air**: Notifikasi berkala otomatis untuk mengingatkan pengguna menjaga hidrasi tubuh.
- 📊 **Riwayat & Statistik**: Tinjau riwayat pencatatan harian dan total konsumsi secara cepat.
- ⚙️ **Pengaturan Fleksibel**: Konfigurasi URL & API Key Supabase serta target air harian sesuai kebutuhan pengguna.

---

## 🗄️ Skema Database (Room / SQLite)

### 1. Tabel `water_logs`
Tabel untuk menyimpan data riwayat konsumsi air minum.

| Kolom | Tipe Data | Keterangan |
|---|---|---|
| `id` | `TEXT` (UUID) | Primary Key (ID unik log) |
| `amount_ml` | `INTEGER` | Jumlah air minum dalam mililiter (ml) |
| `created_at` | `TEXT` | Waktu pembuatan (`yyyy-MM-dd HH:mm:ss`) |
| `date_string` | `TEXT` | Tanggal pencatatan (`yyyy-MM-dd`) |
| `is_synced` | `INTEGER` (Boolean) | Status sinkronisasi ke Supabase (`0` = belum, `1` = sudah) |

### 2. Tabel `coffee_logs`
Tabel untuk menyimpan data riwayat konsumsi kopi dan estimasi kandungan kafein.

| Kolom | Tipe Data | Keterangan |
|---|---|---|
| `id` | `TEXT` (UUID) | Primary Key (ID unik log) |
| `coffee_type` | `TEXT` | Jenis kopi (misal: Espresso, Latte, Americano) |
| `caffeine_mg` | `INTEGER` | Estimasi kadar kafein dalam miligram (mg) |
| `created_at` | `TEXT` | Waktu pembuatan (`yyyy-MM-dd HH:mm:ss`) |
| `date_string` | `TEXT` | Tanggal pencatatan (`yyyy-MM-dd`) |
| `is_synced` | `INTEGER` (Boolean) | Status sinkronisasi ke Supabase (`0` = belum, `1` = sudah) |

---

## 🏗️ Teknologi & Arsitektur

- **Bahasa**: Kotlin
- **UI Framework**: Jetpack Compose dengan Material Design 3 (M3)
- **Database Lokal**: Room Database (KSP, Coroutines Flow)
- **Background Tasks**: AndroidX WorkManager (`SyncWorker`, `ReminderWorker`)
- **Notifikasi**: Android NotificationManager
- **Networking & Cloud**: Supabase REST API via OkHttp / Retrofit
- **Arsitektur**: MVVM (Model-View-ViewModel) + Repository Pattern

---

## 📱 Struktur Direktori Utama

```text
com.example/
├── data/
│   ├── dao/                # Data Access Objects (WaterLogDao, CoffeeLogDao)
│   ├── model/              # Entity Database (WaterLog, CoffeeLog)
│   ├── repository/         # HydrationRepository
│   ├── AppDatabase.kt      # Configuration Room Database
│   └── SupabaseSettingsManager.kt  # Pengelolaan credentials Supabase
├── ui/
│   ├── components/         # Komponen UI Reusable (Dialog, Card, CircularProgress)
│   ├── theme/              # Warna, Tipografi, dan Tema M3
│   ├── DashboardScreen.kt  # Layar utama pencatatan & progress
│   ├── HistoryScreen.kt    # Layar riwayat pencatatan
│   ├── SettingsScreen.kt   # Layar pengaturan Supabase & Notifikasi
│   └── HydrationViewModel.kt # State management aplikasi
├── worker/                 # WorkManager untuk sinkronisasi & pengingat
└── notification/           # Pengelola notifikasi sistem
```
