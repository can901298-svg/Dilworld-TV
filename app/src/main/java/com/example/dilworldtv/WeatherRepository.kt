package com.example.dilworldtv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object WeatherConfig {
    // OpenWeather API key (gömülü)
    const val API_KEY = "ccfdf9c77269b832c1881921bd4d69c5"

    // Bayrampaşa
    const val LAT = 41.035689
    const val LON = 28.9119
}

class WeatherRepository(
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun fetchCurrent(): WeatherUi = withContext(Dispatchers.IO) {
        val url =
            "https://api.openweathermap.org/data/2.5/weather" +
                "?lat=${WeatherConfig.LAT}&lon=${WeatherConfig.LON}" +
                "&units=metric&lang=tr&appid=${WeatherConfig.API_KEY}"

        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("Weather HTTP ${resp.code}")
            val body = resp.body?.string() ?: error("Empty body")

            val json = JSONObject(body)
            val temp = json.getJSONObject("main").getDouble("temp").toInt()
            val desc = json.getJSONArray("weather").getJSONObject(0).getString("description")

            WeatherUi(
                tempC = temp,
                descriptionTr = desc.replaceFirstChar { it.uppercase() }
            )
        }
    }
}
