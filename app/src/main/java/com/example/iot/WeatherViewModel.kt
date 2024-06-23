package com.example.iot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iot.firebase.Repository
import com.example.iot.firebase.model.CurrentData
import com.example.iot.firebase.model.WeatherData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {

    private val repository = Repository()

    private val _weatherData = MutableStateFlow<List<WeatherData>>(emptyList())
    val weatherData: StateFlow<List<WeatherData>> get() = _weatherData

    private val _currentData = MutableStateFlow(CurrentData())
    val currentData: StateFlow<CurrentData> get() = _currentData

    private val _forecastingData = MutableStateFlow<List<WeatherData>>(emptyList())
    val forecastingData: StateFlow<List<WeatherData>> get() = _forecastingData

    init {
        observeWeatherData()
        observeCurrentData()
        observeForecastingData()
    }

    private fun observeWeatherData() {
        viewModelScope.launch {
            repository.getWeatherDataUpdates().collect { data ->
                _weatherData.value = data
            }
        }
    }

    private fun observeCurrentData() {
        repository.getCurrentData { data ->
            _currentData.value = data
        }
    }

    private fun observeForecastingData() {
        repository.getForecastingData { data ->
            _forecastingData.value = data
        }
    }

    fun toggleLED() {
        viewModelScope.launch {
            repository.getAktuatorState { ledState ->
                val newLedState = !ledState
                repository.updateAktuatorValues(newLedState, newLedState)
            }
        }
    }

    fun fetchInitialLedState(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.getAktuatorState { ledState ->
                callback(ledState)
            }
        }
    }

    fun updateLedState(ledState: Boolean) {
        viewModelScope.launch {
            repository.updateAktuatorValues(ledState, ledState)
        }
    }
}
