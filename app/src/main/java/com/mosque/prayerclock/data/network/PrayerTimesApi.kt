package com.mosque.prayerclock.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Default method 20 = Kementerian Agama Republik Indonesia (Kemenag); school 0 = Syafi'i
private const val METHOD_KEMENAG = 20
private const val SCHOOL_SHAFI = 0

interface PrayerTimesApi {
    // Today, by coordinates
    @GET("v1/timings")
    suspend fun getPrayerTimes(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = METHOD_KEMENAG,
        @Query("school") school: Int = SCHOOL_SHAFI,
    ): Response<AlAdhanResponse>

    // Specific date, by coordinates (date path format: dd-MM-yyyy)
    @GET("v1/timings/{date}")
    suspend fun getPrayerTimesByLocationAndDate(
        @Path("date") date: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = METHOD_KEMENAG,
        @Query("school") school: Int = SCHOOL_SHAFI,
    ): Response<AlAdhanResponse>

    @GET("v1/timingsByCity")
    suspend fun getPrayerTimesByCity(
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("method") method: Int = METHOD_KEMENAG,
        @Query("school") school: Int = SCHOOL_SHAFI,
    ): Response<AlAdhanResponse>

    @GET("v1/timingsByCity")
    suspend fun getPrayerTimesByCity(
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("date") date: String, // Format: dd-MM-yyyy
        @Query("method") method: Int = METHOD_KEMENAG,
        @Query("school") school: Int = SCHOOL_SHAFI,
    ): Response<AlAdhanResponse>
}

data class AlAdhanResponse(
    val code: Int,
    val status: String,
    val data: AlAdhanData,
)

data class AlAdhanData(
    val timings: AlAdhanTimings,
    val date: AlAdhanDate,
    val meta: AlAdhanMeta,
)

data class AlAdhanTimings(
    @SerializedName("Fajr")
    val fajr: String,
    @SerializedName("Sunrise")
    val sunrise: String,
    @SerializedName("Dhuhr")
    val dhuhr: String,
    @SerializedName("Asr")
    val asr: String,
    @SerializedName("Sunset")
    val sunset: String,
    @SerializedName("Maghrib")
    val maghrib: String,
    @SerializedName("Isha")
    val isha: String,
    @SerializedName("Imsak")
    val imsak: String,
    @SerializedName("Midnight")
    val midnight: String,
)

data class AlAdhanDate(
    val readable: String,
    val timestamp: String,
    val hijri: AlAdhanHijri,
    val gregorian: AlAdhanGregorian,
)

data class AlAdhanHijri(
    val date: String,
    val format: String,
    val day: String,
    val weekday: AlAdhanWeekday,
    val month: AlAdhanMonth,
    val year: String,
    val designation: AlAdhanDesignation,
)

data class AlAdhanGregorian(
    val date: String,
    val format: String,
    val day: String,
    val weekday: AlAdhanWeekday,
    val month: AlAdhanMonth,
    val year: String,
    val designation: AlAdhanDesignation,
)

data class AlAdhanWeekday(
    val en: String,
)

data class AlAdhanMonth(
    val number: Int,
    val en: String,
)

data class AlAdhanDesignation(
    val abbreviated: String,
    val expanded: String,
)

data class AlAdhanMeta(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val method: AlAdhanMethod,
    val latitudeAdjustmentMethod: String,
    val midnightMode: String,
    val school: String,
    val offset: Map<String, Int>,
)

data class AlAdhanMethod(
    val id: Int,
    val name: String,
    val params: AlAdhanParams,
)

data class AlAdhanParams(
    @SerializedName("Fajr")
    val fajr: Double,
    @SerializedName("Isha")
    val isha: Double,
)
