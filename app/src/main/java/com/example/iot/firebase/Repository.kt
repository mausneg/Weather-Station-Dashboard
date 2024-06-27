package com.example.iot.firebase

import android.util.Log
import com.example.iot.firebase.model.CurrentData
import com.example.iot.firebase.model.WeatherData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class Repository {

    private val database = Firebase.database
    private val weatherDataRef = database.getReference("weather_data")
    private val aktuatorRef = database.getReference("aktuator")
    private val currentDataRef = database.getReference("current_data")
    private val forecastingDataRef = database.getReference("forecasting_data")

    fun getWeatherDataUpdates(): Flow<List<WeatherData>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val weatherDataList = mutableListOf<WeatherData>()
                for (dataSnapshot in snapshot.children) {
                    val weatherData = dataSnapshot.getValue(WeatherData::class.java)
                    if (weatherData != null) {
                        weatherDataList.add(weatherData)
                    }
                }
                Log.d("Repository", "Weather Data: $weatherDataList")
                trySend(weatherDataList).isSuccess
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Repository", "Error fetching data", error.toException())
                close(error.toException())
            }
        }
        weatherDataRef.addValueEventListener(listener)
        awaitClose { weatherDataRef.removeEventListener(listener) }
    }

    fun getCurrentData(callback: (CurrentData) -> Unit) {
        currentDataRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentData = snapshot.getValue(CurrentData::class.java) ?: CurrentData()
                callback(currentData)
                Log.d("Repository", "Current Data: $currentData")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Repository", "Error fetching current data", error.toException())
            }
        })
    }

    fun getForecastingData(callback: (List<WeatherData>) -> Unit) {
        forecastingDataRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val forecastingDataList = mutableListOf<WeatherData>()
                for (dataSnapshot in snapshot.children) {
                    val weatherData = dataSnapshot.getValue(WeatherData::class.java)
                    if (weatherData != null) {
                        forecastingDataList.add(weatherData)
                    }
                }
                callback(forecastingDataList)
                Log.d("Repository", "Forecasting Data: $forecastingDataList")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Repository", "Error fetching forecasting data", error.toException())
            }
        })
    }



    fun getAktuatorState(callback: (Boolean) -> Unit) {
        aktuatorRef.child("led").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ledState = snapshot.getValue(Boolean::class.java) ?: false
                callback(ledState)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Repository", "Error fetching aktuator state", error.toException())
            }
        })
    }

    fun updateAktuatorValues(led: Boolean, motor: Boolean) {
        val updates = mapOf(
            "led" to led,
            "motor" to motor
        )
        aktuatorRef.updateChildren(updates)
            .addOnSuccessListener {
                Log.d("Repository", "Aktuator values updated successfully.")
            }
            .addOnFailureListener { error ->
                Log.e("Repository", "Failed to update aktuator values", error)
            }
    }
}

