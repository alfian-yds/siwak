package com.mosque.prayerclock.data.network

import com.mosque.prayerclock.data.model.WeatherInfo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherMapApi {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
    ): Response<OpenWeatherMapResponse>

    @GET("forecast")
    suspend fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("cnt") count: Int = 5,
    ): Response<OpenWeatherMapForecastResponse>
}

data class OpenWeatherMapResponse(
    val coord: Coordinates,
    val weather: List<WeatherDescription>,
    val base: String,
    val main: MainWeatherData,
    val visibility: Int,
    val wind: WindData,
    val clouds: CloudData,
    val dt: Long,
    val sys: SystemData,
    val timezone: Int,
    val id: Long,
    val name: String,
    val cod: Int,
)

data class OpenWeatherMapForecastResponse(
    val cod: String,
    val message: Int,
    val cnt: Int,
    val list: List<ForecastItem>,
    val city: CityInfo,
)

data class Coordinates(
    val lon: Double,
    val lat: Double,
)

data class WeatherDescription(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String,
)

data class MainWeatherData(
    val temp: Double,
    val feels_like: Double,
    val temp_min: Double,
    val temp_max: Double,
    val pressure: Int,
    val humidity: Int,
    val sea_level: Int? = null,
    val grnd_level: Int? = null,
)

data class WindData(
    val speed: Double,
    val deg: Int,
    val gust: Double? = null,
)

data class CloudData(
    val all: Int,
)

data class SystemData(
    val type: Int? = null,
    val id: Long? = null,
    val country: String,
    val sunrise: Long,
    val sunset: Long,
)

data class ForecastItem(
    val dt: Long,
    val main: MainWeatherData,
    val weather: List<WeatherDescription>,
    val clouds: CloudData,
    val wind: WindData,
    val visibility: Int,
    val pop: Double,
    val sys: ForecastSystemData,
    val dt_txt: String,
)

data class ForecastSystemData(
    val pod: String,
)

data class CityInfo(
    val id: Long,
    val name: String,
    val coord: Coordinates,
    val country: String,
    val population: Long,
    val timezone: Int,
    val sunrise: Long,
    val sunset: Long,
)

fun OpenWeatherMapResponse.toWeatherInfo(): WeatherInfo {
    val weatherDesc = weather.firstOrNull()
    return WeatherInfo(
        temperature = main.temp,
        description = weatherDesc?.description?.replaceFirstChar { it.uppercase() } ?: "Unknown",
        icon = "https://openweathermap.org/img/wn/${weatherDesc?.icon ?: "01d"}@2x.png",
        humidity = main.humidity,
        feelsLike = main.feels_like,
        visibility = visibility / 1000.0, // Convert meters to kilometers
        // SIWAK: endpoint /weather gratis TIDAK menyediakan UV → diestimasi dari posisi matahari
        // (waktu terhadap sunrise/sunset) dan tutupan awan. 0 di malam hari, puncak di tengah hari.
        uvIndex = estimateUvIndex(dt, sys.sunrise, sys.sunset, clouds.all),
        windSpeed = wind.speed * 3.6, // Convert m/s to km/h
    )
}

/**
 * Estimasi indeks UV tanpa endpoint berbayar.
 * Model sederhana: kurva harian berpuncak di tengah hari (antara sunrise & sunset),
 * dikalikan faktor tutupan awan. Cocok untuk wilayah tropis (Indonesia), puncak cerah ~11.
 * Semua waktu dalam detik epoch (UTC), jadi tak bergantung zona waktu.
 */
internal fun estimateUvIndex(
    dtSec: Long,
    sunriseSec: Long,
    sunsetSec: Long,
    cloudsPercent: Int,
): Double {
    if (sunsetSec <= sunriseSec) return 0.0
    if (dtSec <= sunriseSec || dtSec >= sunsetSec) return 0.0 // malam → UV 0
    val noon = (sunriseSec + sunsetSec) / 2.0
    val halfDay = (sunsetSec - sunriseSec) / 2.0
    val x = ((dtSec - noon) / halfDay).coerceIn(-1.0, 1.0) // -1 (sunrise) .. 0 (siang) .. 1 (sunset)
    val elevation = kotlin.math.cos(x * Math.PI / 2.0).coerceIn(0.0, 1.0) // 1 di tengah hari
    val peakClearSky = 11.0 // puncak langit cerah wilayah tropis
    val cloudFactor = 1.0 - 0.7 * (cloudsPercent.coerceIn(0, 100) / 100.0) // awan mengurangi s/d 70%
    return peakClearSky * elevation * cloudFactor
}
