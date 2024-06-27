package com.example.iot.firebase.model

data class WeatherData(
    val humidity: Int = 0,
    val light: Boolean = false,
    val rainfall: Int = 0,
    val temperature: Double = 0.0,
    val timestamp: String = ""
)

data class CurrentData(
    val humidity: Int = 0,
    val light: Boolean = false,
    val rainfall: Int = 0,
    val temperature: Double = 0.0,
    val timestamp: String = "",
    val weather_name: String = ""
)