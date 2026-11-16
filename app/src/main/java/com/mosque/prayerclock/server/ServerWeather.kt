package com.mosque.prayerclock.server

import com.google.gson.Gson
import com.mosque.prayerclock.data.model.WeatherInfo
import com.mosque.prayerclock.data.network.estimateUvIndex

/** Cuaca dari server (Backend Schema §5.4) ke model tampilan TV. UV diperkirakan di TV (TRD §8). */
object ServerWeather {
    private val gson = Gson()

    fun toWeatherInfo(json: String): WeatherInfo? {
        val w = runCatching { gson.fromJson(json, WeatherDto::class.java) }.getOrNull() ?: return null
        val temp = w.temperatureC ?: return null
        val uv =
            if (w.observedAt != null && w.sunrise != null && w.sunset != null && w.sunset > 0) {
                estimateUvIndex(w.observedAt, w.sunrise, w.sunset, w.cloudsPercent ?: 0)
            } else {
                null
            }
        return WeatherInfo(
            temperature = temp,
            description = w.description.orEmpty().replaceFirstChar { it.uppercase() },
            // Ikon digambar di TV dari kodenya; tidak ada gambar yang diunduh dari OpenWeather.
            icon = w.iconCode ?: "01d",
            humidity = w.humidity ?: 0,
            feelsLike = w.feelsLikeC ?: temp,
            visibility = w.visibilityKm,
            uvIndex = uv,
            windSpeed = w.windKmh,
        )
    }
}
