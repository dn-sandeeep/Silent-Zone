package com.sandeep.silentzone

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SilentModeViewModel(
    private val repo: SilentModeRepository
) : ViewModel() {
    private val _availableSsidList = MutableStateFlow<List<String>>(emptyList())
    val availableSsidList: StateFlow<List<String>> = _availableSsidList.asStateFlow()
    fun updateSsidList(ssids: List<String>) {
        _availableSsidList.value = ssids
    }

    fun clearSsidList() {
        _availableSsidList.value = emptyList()
    }

    private val _autoDetectionEnabled = MutableStateFlow(true)
    val autoDetectionEnabled: StateFlow<Boolean> = _autoDetectionEnabled.asStateFlow()

    fun setAutoDetectionEnabled(enabled: Boolean) {
        _autoDetectionEnabled.value = enabled
    }
    private val uiState = MutableStateFlow(
        UiState(
            accessGranted = repo.hasPolicyAccess(),
            currentMode = repo.getCurrentMode(),
              null
        ))
    val uiStateFlow: StateFlow<UiState> = uiState.asStateFlow()
    fun refresh() {
        uiState.value = uiState.value.copy(
            accessGranted = repo.hasPolicyAccess(),
            currentMode = repo.getCurrentMode()
        )
    }
    fun setSilent() {
        viewModelScope.launch {
            val mode = repo.setSilent()
            uiState.value = uiState.value.copy(currentMode = mode, message = "Silent mode enabled")
        }
    }
    private val _savedSilentSsids = MutableStateFlow<Set<String>>(emptySet())
    val savedSilentSsids: StateFlow<Set<String>> = _savedSilentSsids.asStateFlow()

    private val _savedVibrateSsids = MutableStateFlow<Set<String>>(emptySet())
    val savedVibrateSsids: StateFlow<Set<String>> = _savedVibrateSsids.asStateFlow()

    fun updateSavedSilentSsids(ssids: Set<String>) {
        _savedSilentSsids.value = ssids
    }

    fun updateSavedVibrateSsids(ssids: Set<String>) {
        _savedVibrateSsids.value = ssids
    }

    fun addSilentSsid(ssid: String) {
        val current = _savedSilentSsids.value.toMutableSet()
        current.add(ssid)
        // Ensure it's not in vibrate list
        val vibrate = _savedVibrateSsids.value.toMutableSet()
        vibrate.remove(ssid)

        _savedSilentSsids.value = current
        _savedVibrateSsids.value = vibrate
    }

    fun addVibrateSsid(ssid: String) {
        val current = _savedVibrateSsids.value.toMutableSet()
        current.add(ssid)
        // Ensure it's not in silent list
        val silent = _savedSilentSsids.value.toMutableSet()
        silent.remove(ssid)

        _savedVibrateSsids.value = current
        _savedSilentSsids.value = silent
    }

    fun removeSsid(ssid: String) {
        val silent = _savedSilentSsids.value.toMutableSet()
        if (silent.remove(ssid)) {
            _savedSilentSsids.value = silent
            return // Optimization
        }

        val vibrate = _savedVibrateSsids.value.toMutableSet()
        if (vibrate.remove(ssid)) {
            _savedVibrateSsids.value = vibrate
        }
    }

    fun setVibrate() {
        viewModelScope.launch {
            val mode = repo.setVibrate()
            uiState.value = uiState.value.copy(currentMode = mode, message = "Vibrate mode enabled")
        }
    }

    fun setNormal() {
        viewModelScope.launch {
            val mode = repo.setNormal()
            uiState.value = uiState.value.copy(currentMode = mode, message = "Normal mode enabled")
        }
    }

    private val _locationZones = MutableStateFlow<List<LocationZone>>(emptyList())
    val locationZones: StateFlow<List<LocationZone>> = _locationZones.asStateFlow()

    init {
        refreshLocationZones()
    }

    fun refreshLocationZones() {
        _locationZones.value = repo.getLocationZones()
    }

    fun addCurrentLocationZone(mode: RingerMode) {
        repo.getCurrentLocation(
            onLocationResult = { lat, lon ->
                addLocationZone(lat, lon, "Zone ${locationZones.value.size + 1}", mode)
                uiState.value = uiState.value.copy(message = "Location Zone Added!")
            },
            onError = {
                // Handle error (maybe expose an error state)
                Log.e("SilentModeViewModel", "Could not get current location")
                uiState.value = uiState.value.copy(message = "Error: Could not get location")
            }
        )
    }

    fun addLocationZone(latitude: Double, longitude: Double, name: String, mode: RingerMode) {
        val zone = LocationZone(
            id = java.util.UUID.randomUUID().toString(),
            latitude = latitude,
            longitude = longitude,
            name = name,
            radius = 100f,
            mode = mode
        )
        Log.d("LocationDebug", "Adding Location Zone in ViewModel: ${zone.id}, Lat: $latitude, Lon: $longitude")
        repo.addLocationZone(zone)
        refreshLocationZones()
    }

    fun removeLocationZone(id: String) {
        repo.removeLocationZone(id)
        refreshLocationZones()
        setNormal()
        // Optional: Update message specific to removal if setNormal doesn't cover it well enough
        // setNormal already sets "Normal mode enabled"
    }
}