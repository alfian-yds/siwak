package com.mosque.prayerclock.data.model

data class AppSettings(
    val language: Language = Language.INDONESIAN,
    val city: String = "Yogyakarta",
    val country: String = "Indonesia",
    val latitude: Double = -7.7956, // Yogyakarta
    val longitude: Double = 110.3695,
    val mosqueName: String = "Masjid",
    val clockType: ClockType = ClockType.DIGITAL,
    val theme: AppTheme = AppTheme.DARK,
    val fontSize: FontSize = FontSize.MEDIUM,
    val showSeconds: Boolean = true,
    val show24HourFormat: Boolean = false,
    val colorTheme: String = "classic_mosque", // Color theme ID
    val manualImsak: String = "04:20",
    val manualFajrAzan: String = "05:30",
    val manualSunrise: String = "06:15",
    val manualDhuhrAzan: String = "12:15",
    val manualAsrAzan: String = "15:30",
    val manualMaghribAzan: String = "18:30",
    val manualIshaAzan: String = "19:45",
    val fajrIqamahGap: Int = 20,
    val dhuhrIqamahGap: Int = 10,
    val asrIqamahGap: Int = 10,
    val maghribIqamahGap: Int = 10,
    val ishaIqamahGap: Int = 10,
    // SIWAK: koreksi menit per-waktu (offset ±) untuk mencocokkan jadwal Kemenag setempat
    // SIWAK: aktifkan koreksi menit; jika mati, semua offset diabaikan (0)
    val prayerOffsetEnabled: Boolean = false,
    val imsakOffset: Int = 0,
    val fajrOffset: Int = 0,
    val sunriseOffset: Int = 0,
    val dhuhrOffset: Int = 0,
    val asrOffset: Int = 0,
    val maghribOffset: Int = 0,
    val ishaOffset: Int = 0,
    // SIWAK: sakelar animasi pergantian Adzan<->Iqamah pada kartu waktu
    val showIqamahCycle: Boolean = true,
    val refreshInterval: Long = 24 * 60 * 60 * 1000L, // 24 hours in milliseconds
    val showWeather: Boolean = false,
    val weatherCity: String = "",
    val weatherCountry: String = "Indonesia",
    val weatherProvider: WeatherProvider = WeatherProvider.WEATHER_API,
    val hijriProvider: HijriProvider = HijriProvider.AL_ADHAN_API,
    val manualHijriDay: Int = 7,
    val manualHijriMonth: Int = 2, // Safar
    val manualHijriYear: Int = 1447,
    val lastUpdatedGregorianDate: String = "2025-08-01", // Track when Hijri date was last set
    val prayerServiceType: PrayerServiceType = PrayerServiceType.AL_ADHAN_API,
    val selectedZone: Int = 1, // (legacy) MosqueClock backend zones
    val selectedRegion: String = "Yogyakarta", // For third-party prayer time services
    val soundEnabled: Boolean = true, // Enable/disable beep countdown sound (master toggle)
    // Enable/disable full-screen countdown before Azan/Iqamah
    val fullScreenCountdownEnabled: Boolean = true,
    // SIWAK: berapa menit sebelum Adzan countdown layar penuh mulai tampil (bisa diatur)
    val fullScreenCountdownMinutes: Int = 10,
    // SIWAK: opsi sembunyikan waktu Terbit & Imsak di layar utama
    val showSunrise: Boolean = true,
    val showImsak: Boolean = false,
    // SIWAK: waktu sinkronisasi terakhir (epoch millis; 0 = belum pernah)
    val prayerLastSyncMillis: Long = 0L,
    val weatherLastSyncMillis: Long = 0L,
    // SIWAK: cache data cuaca terakhir (JSON) agar tetap tampil saat offline / awal buka
    val cachedWeatherJson: String = "",
    // SIWAK: ukuran jam (persen; 100 = normal, 115 = besar, 130 = sangat besar)
    val clockSizePercent: Int = 100,
    // SIWAK: penyesuaian tanggal Hijriah dalam hari (mis. -1, 0, +1) untuk cocok Kemenag setempat
    val hijriDayOffset: Int = 0,
    // SIWAK: Mode Istirahat Malam (layar redup pada rentang jam malam)
    val nightModeEnabled: Boolean = false,
    val nightStartHour: Int = 22, // mulai redup (jam 0-23)
    val nightEndHour: Int = 4, // berhenti redup (jam 0-23)
    val nightDimLevel: Int = 85, // persen kegelapan overlay (0-100)
    // SIWAK: logo & background kustom (URI; kosong = default)
    val mosqueLogoUri: String = "",
    val backgroundMainUri: String = "",
    val backgroundPrayerUri: String = "", // layar "masuk waktu shalat" (silent HP)
    val backgroundIqamahUri: String = "", // layar hitung mundur iqamah
    // SIWAK: running text banyak pesan dipisah "||" (diganti instan)
    val runningTextMessages: String = "",
    val weatherApiKey: String = "", // WeatherAPI.com API key
    val openWeatherMapApiKey: String = "3c2ad345d6952077f51037b1c0f8de88", // SIWAK: OpenWeatherMap default
    val mosqueClockBackendUrl: String = "", // MosqueClock backend URL
    val mosqueClockBackendApiKey: String = "", // MosqueClock backend API key
    // Duration for Jummah (Friday) prayer in minutes (default: 60 minutes = 1 hour)
    val jummahDurationMinutes: Int = 60,
    // Duration to display Dua for Joining Saff after Iqamah in minutes (default: 5 minutes)
    val duaDisplayDurationMinutes: Int = 5,
    val showJummahScreen: Boolean = true, // Show full-screen Jummah in progress display during Jummah prayer time
    // Enhanced notification settings
    val azanSoundEnabled: Boolean = true, // Enable/disable Azan sound
    val iqamahSoundEnabled: Boolean = true, // Enable/disable Iqamah sound
    val azanSoundType: SoundType = SoundType.COUNTDOWN_TICKING, // Sound type for Azan
    val iqamahSoundType: SoundType = SoundType.COUNTDOWN_TICKING, // Sound type for Iqamah
    val azanSoundUri: String = "", // URI to custom audio file for Azan (empty = use built-in)
    val iqamahSoundUri: String = "", // URI to custom audio file for Iqamah (empty = use built-in)
    // Jumma Night Bayan settings - applies to Thursday night Isha prayer
    val jummaNightBayanEnabled: Boolean = false, // Enable/disable Jumma Night Bayan feature
    val jummaNightBayanMinutes: Int = 30, // Duration in minutes for Jumma Night Bayan (default: 30 minutes)
    // Auto-update settings
    val autoUpdateCheckEnabled: Boolean = true, // Enable/disable automatic update check on app startup
    val skippedUpdateVersion: String = "", // Version that user chose to skip (won't show dialog for this version)
)

enum class Language(
    val code: String,
    val displayName: String,
) {
    // Android resource qualifier for Indonesian is "in" (Java legacy code); folder = values-in
    INDONESIAN("in", "Bahasa Indonesia"),
    ENGLISH("en", "English"),
}

enum class WeatherProvider(
    val displayName: String,
) {
    WEATHER_API("WeatherAPI.com"),
    OPEN_WEATHER_MAP("OpenWeatherMap"),
}

enum class HijriProvider(
    val displayName: String,
) {
    // SIWAK: hanya AL_ADHAN_API (hisab otomatis) & MANUAL yang ditampilkan
    ACJU_DIRECT("ACJU Direct Scraping"),
    MOSQUE_CLOCK_API("MosqueClock API"),
    AL_ADHAN_API("Otomatis (Hisab)"),
    MANUAL("Manual"),
}

enum class ClockType {
    DIGITAL,
    ANALOG,
    BOTH, // Cycles between Digital and Analog
}

enum class AppTheme {
    DEFAULT,
    DARK,
    LIGHT,
    MOSQUE_GREEN,
    BLUE,
}

enum class FontSize {
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE,
}

enum class PrayerServiceType(
    val displayName: String,
) {
    // SIWAK: hanya AL_ADHAN_API (Kemenag) & MANUAL yang ditampilkan; dua lainnya (Sri Lanka) disembunyikan
    ACJU_DIRECT("ACJU Direct Scraping"),
    MOSQUE_CLOCK_API("MosqueClock API"),
    AL_ADHAN_API("Otomatis (Kemenag RI)"),
    MANUAL("Jadwal Manual"),
}

enum class SoundType(
    val displayName: String,
) {
    COUNTDOWN_TICKING("Tik-tik hitung mundur (5 detik)"),
    TRADITIONAL_BEEP("Bip / pip (nyaring)"),
    CUSTOM("Audio kustom (mis. kumandang azan)"),
}

data class LocationInfo(
    val region: String,
    val city: String,
    val displayNameEn: String,
    val displayNameTa: String,
)

data class WeatherInfo(
    val temperature: Double,
    val description: String,
    val icon: String,
    val humidity: Int,
    val feelsLike: Double,
    val visibility: Double? = null,
    val uvIndex: Double? = null,
    val windSpeed: Double? = null, // in km/h
)

data class PrayerZone(
    val id: Int,
    val name: String,
    val description: String,
)

data class CityCoordinates(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String = "Sri Lanka",
)

// City coordinates mapping for accurate weather data
object CityCoordinatesMap {
    val cityCoordinates =
        mapOf(
            "Dickwella" to CityCoordinates("Dickwella", 5.9769, 80.6986),
            "Colombo" to CityCoordinates("Colombo", 6.9271, 79.8612),
            "Kandy" to CityCoordinates("Kandy", 7.2906, 80.6337),
            "Galle" to CityCoordinates("Galle", 6.0535, 80.2210),
            "Jaffna" to CityCoordinates("Jaffna", 9.6615, 80.0255),
            "Negombo" to CityCoordinates("Negombo", 7.2083, 79.8358),
            "Trincomalee" to CityCoordinates("Trincomalee", 8.5874, 81.2152),
            "Batticaloa" to CityCoordinates("Batticaloa", 7.7102, 81.6924),
            "Anuradhapura" to CityCoordinates("Anuradhapura", 8.3114, 80.4037),
            "Ratnapura" to CityCoordinates("Ratnapura", 6.6828, 80.3992),
            "Kurunegala" to CityCoordinates("Kurunegala", 7.4818, 80.3609),
            "Badulla" to CityCoordinates("Badulla", 6.9934, 81.0550),
            "Matara" to CityCoordinates("Matara", 5.9549, 80.5550),
            "Hambantota" to CityCoordinates("Hambantota", 6.1241, 81.1185),
        )

    fun getCoordinates(cityName: String): CityCoordinates? = cityCoordinates[cityName]
}

// Predefined zones for MosqueClock backend (all 13 Sri Lankan zones as per ACJU)
object PrayerZones {
    val zones =
        listOf(
            PrayerZone(
                1,
                "Zone 1",
                "Colombo District, Gampaha District, Kalutara District",
            ),
            PrayerZone(2, "Zone 2", "Jaffna District, Nallur"),
            PrayerZone(
                3,
                "Zone 3",
                "Mullaitivu District (except Nallur), Kilinochchi District, Vavuniya District",
            ),
            PrayerZone(4, "Zone 4", "Mannar District, Puttalam District"),
            PrayerZone(5, "Zone 5", "Anuradhapura District, Polonnaruwa District"),
            PrayerZone(6, "Zone 6", "Kurunegala District"),
            PrayerZone(
                7,
                "Zone 7",
                "Kandy District, Matale District, Nuwara Eliya District",
            ),
            PrayerZone(8, "Zone 8", "Batticaloa District, Ampara District"),
            PrayerZone(9, "Zone 9", "Trincomalee District"),
            PrayerZone(
                10,
                "Zone 10",
                "Badulla District, Monaragala District, Padiyatalawa, Dehiaththakandiya",
            ),
            PrayerZone(11, "Zone 11", "Ratnapura District, Kegalle District"),
            PrayerZone(12, "Zone 12", "Galle District, Matara District"),
            PrayerZone(13, "Zone 13", "Hambantota District"),
        )
}

// SIWAK: daftar Kota/Kabupaten Indonesia -> koordinat (fokus DIY & Jawa Tengah + kota besar).
// Dipakai untuk jadwal shalat berbasis koordinat (metode Kemenag). Bisa ditambah sesuai kebutuhan.
data class IdCity(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

object IndonesianCities {
    val list: List<IdCity> =
        listOf(
            // DI Yogyakarta
            IdCity("Kota Yogyakarta", -7.8014, 110.3647),
            IdCity("Kab. Sleman", -7.7161, 110.3550),
            IdCity("Kab. Bantul", -7.8880, 110.3300),
            IdCity("Kab. Kulon Progo", -7.8664, 110.1486),
            IdCity("Kab. Gunungkidul", -7.9656, 110.6039),
            // Jawa Tengah (sekitar)
            IdCity("Kota Magelang", -7.4706, 110.2178),
            IdCity("Kab. Magelang", -7.6000, 110.2200),
            IdCity("Kab. Klaten", -7.7059, 110.6065),
            IdCity("Kota Surakarta (Solo)", -7.5755, 110.8243),
            IdCity("Kab. Boyolali", -7.5333, 110.5833),
            IdCity("Kab. Sukoharjo", -7.6822, 110.8405),
            IdCity("Kab. Karanganyar", -7.5966, 110.9530),
            IdCity("Kab. Sragen", -7.4269, 111.0198),
            IdCity("Kab. Wonogiri", -7.8167, 110.9250),
            IdCity("Kota Salatiga", -7.3305, 110.5084),
            IdCity("Kota Semarang", -6.9932, 110.4203),
            IdCity("Kab. Purworejo", -7.7139, 109.9989),
            IdCity("Kab. Temanggung", -7.3167, 110.1750),
            IdCity("Kab. Wonosobo", -7.3583, 109.9008),
            IdCity("Kab. Kebumen", -7.6689, 109.6522),
            IdCity("Kab. Banyumas (Purwokerto)", -7.4250, 109.2340),
            IdCity("Kota Pekalongan", -6.8886, 109.6753),
            IdCity("Kota Tegal", -6.8694, 109.1402),
            IdCity("Kab. Cilacap", -7.7269, 109.0100),
            IdCity("Kab. Kudus", -6.8048, 110.8405),
            // Kota besar nasional
            IdCity("DKI Jakarta", -6.2088, 106.8456),
            IdCity("Kota Bogor", -6.5971, 106.8060),
            IdCity("Kota Depok", -6.4025, 106.7942),
            IdCity("Kota Tangerang", -6.1783, 106.6319),
            IdCity("Kota Bekasi", -6.2383, 106.9756),
            IdCity("Kota Bandung", -6.9175, 107.6191),
            IdCity("Kota Surabaya", -7.2575, 112.7521),
            IdCity("Kota Malang", -7.9666, 112.6326),
            IdCity("Kota Medan", 3.5952, 98.6722),
            IdCity("Kota Palembang", -2.9761, 104.7754),
            IdCity("Kota Makassar", -5.1477, 119.4327),
            IdCity("Kota Denpasar", -8.6705, 115.2126),
            IdCity("Kota Balikpapan", -1.2379, 116.8529),
        )

    fun find(name: String): IdCity? = list.firstOrNull { it.name == name }
}
