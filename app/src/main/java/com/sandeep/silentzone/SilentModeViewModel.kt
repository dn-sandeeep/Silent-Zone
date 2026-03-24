package com.sandeep.silentzone

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SilentModeViewModel @Inject constructor(
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

    val wifiZones: StateFlow<List<WifiZone>> = repo.getWifiZonesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addWifiZone(ssid: String, mode: RingerMode) {
        viewModelScope.launch {
            repo.addWifiZone(WifiZone(ssid, mode))
        }
    }

    fun checkWifiConnection(ssid: String?) {
        viewModelScope.launch {
            repo.onWifiChanged(ssid)
        }
    }

    fun removeWifiZone(ssid: String) {
        viewModelScope.launch {
            repo.removeWifiZone(ssid)
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

    val locationZones: StateFlow<List<LocationZone>> = repo.getLocationZonesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val importantContacts: StateFlow<List<ImportantContact>> = repo.getImportantContactsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addImportantContact(name: String, phoneNumber: String) {
        viewModelScope.launch {
            val contact = ImportantContact(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                phoneNumber = phoneNumber
            )
            repo.addImportantContact(contact)
            uiState.value = uiState.value.copy(message = "Important contact added!")
        }
    }

    fun removeImportantContact(phoneNumber: String) {
        viewModelScope.launch {
            repo.removeImportantContact(phoneNumber)
            uiState.value = uiState.value.copy(message = "Important contact removed!")
        }
    }

    fun addCurrentLocationZone(mode: RingerMode, radius: Float = 100f) {
        repo.getCurrentLocation(
            onLocationResult = { lat, lon ->
                addLocationZone(lat, lon, "Zone ${locationZones.value.size + 1}", mode, radius)
                uiState.value = uiState.value.copy(message = "Location Zone Added!")
            },
            onError = {
                Log.e("SilentModeViewModel", "Could not get current location")
                uiState.value = uiState.value.copy(message = "Error: Could not get location")
            }
        )
    }

    fun addLocationZone(latitude: Double, longitude: Double, name: String, mode: RingerMode, radius: Float = 100f) {
        viewModelScope.launch {
            val zone = LocationZone(
                id = java.util.UUID.randomUUID().toString(),
                latitude = latitude,
                longitude = longitude,
                name = name,
                radius = radius,
                mode = mode
            )
            repo.addLocationZone(zone)
        }
    }

    fun removeLocationZone(id: String) {
        viewModelScope.launch {
            repo.removeLocationZone(id)
        }
    }
}