package com.raceup.app

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// 1. The Data Structure (What comes back from the API)
data class WeatherResponse(
    val main: MainData,
    val weather: List<WeatherDescription>,
    val wind: WindData
)

data class MainData(
    val temp: Float,     // Temperature
    val humidity: Int    // Humidity
)

data class WeatherDescription(
    val main: String,    // e.g. "Rain", "Clear"
    val description: String
)

data class WindData(
    val speed: Float
)

// 2. The API Definition
interface WeatherApi {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String
    ): WeatherResponse
}

// 3. The Object to use in your Activity
object WeatherNetwork {
    private const val BASE_URL = "https://api.openweathermap.org/"

    val api: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }
}