package com.sandeep.silentzone

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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

    private val _message = MutableStateFlow<String?>(null)

    val uiStateFlow: StateFlow<UiState> = combine(
        repo.currentModeFlow,
        _message
    ) { mode, msg ->
        UiState(
            accessGranted = repo.hasPolicyAccess(),
            currentMode = mode,
            message = msg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState(repo.hasPolicyAccess(), repo.getCurrentMode(), null))

    fun refresh() {
        repo.refreshMode()
    }
    fun setSilent() {
        viewModelScope.launch {
            repo.setSilent()
            _message.value = "Silent mode enabled"
            delay(2000)
            _message.value = null
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
            repo.setVibrate()
            _message.value = "Vibrate mode enabled"
            delay(2000)
            _message.value = null
        }
    }

    fun setNormal() {
        viewModelScope.launch {
            repo.setNormal()
            _message.value = "Normal mode enabled"
            delay(2000)
            _message.value = null
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
            _message.value = "Important contact added!"
            delay(2000)
            _message.value = null
        }
    }

    fun removeImportantContact(phoneNumber: String) {
        viewModelScope.launch {
            repo.removeImportantContact(phoneNumber)
            _message.value = "Important contact removed!"
            delay(2000)
            _message.value = null
        }
    }

    fun addCurrentLocationZone(mode: RingerMode, radius: Float = 100f) {
        repo.getCurrentLocation(
            onLocationResult = { lat, lon ->
                addLocationZone(lat, lon, "Zone ${locationZones.value.size + 1}", mode, radius)
                _message.value = "Location Zone Added!"
                viewModelScope.launch {
                    delay(2000)
                    _message.value = null
                }
            },
            onError = {
                Log.e("SilentModeViewModel", "Could not get current location")
                _message.value = "Error: Could not get location"
                viewModelScope.launch {
                    delay(2000)
                    _message.value = null
                }
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