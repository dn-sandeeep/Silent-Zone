package com.sandeep.silentzone

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OperationState {
    object Idle : OperationState()
    object Loading : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}

@HiltViewModel
class SilentModeViewModel @Inject constructor(
    private val repo: SilentModeRepository
) : ViewModel() {
    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    private val _isFallback = MutableStateFlow(false)

    private val _availableSsidList = MutableStateFlow<List<String>>(emptyList())
    val availableSsidList: StateFlow<List<String>> = _availableSsidList.asStateFlow()

    init {
        // Handle DND Fallback events
        viewModelScope.launch {
            repo.fallbackEvents.collect {
                _isFallback.value = true
                _operationState.value = OperationState.Error("DND access required for SILENT mode. Using Vibrate.")
                delay(4000)
                _operationState.value = OperationState.Idle
            }
        }
    }

    fun updateSsidList(ssids: List<String>) {
        _availableSsidList.value = ssids
    }

    fun clearSsidList() {
        _availableSsidList.value = emptyList()
    }

    private val _message = MutableStateFlow<String?>(null)

    val uiStateFlow: StateFlow<UiState> = combine(
        repo.currentModeFlow,
        _message,
        _isFallback
    ) { mode, msg, fallback ->
        UiState(
            accessGranted = repo.hasPolicyAccess(),
            currentMode = mode,
            isFallback = fallback && !repo.hasPolicyAccess(),
            message = msg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState(repo.hasPolicyAccess(), repo.getCurrentMode(), false, null))

    fun refresh() {
        repo.refreshMode()
    }

    private fun launchOperation(message: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                action()
                _operationState.value = OperationState.Success(message)
                delay(2000)
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(e.message ?: "Operation failed")
                delay(3000)
                _operationState.value = OperationState.Idle
            }
        }
    }

    fun setSilent() {
        launchOperation("Silent mode enabled") {
            repo.setSilent()
        }
    }

    fun setVibrate() {
        launchOperation("Vibrate mode enabled") {
            repo.setVibrate()
        }
    }

    fun setNormal() {
        launchOperation("Normal mode enabled") {
            repo.setNormal()
        }
    }

    val wifiZones: StateFlow<List<WifiZone>> = repo.getWifiZonesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addWifiZone(ssid: String, mode: RingerMode) {
        launchOperation("WiFi Zone Added!") {
            repo.addWifiZone(WifiZone(ssid, mode))
        }
    }

    fun checkWifiConnection(ssid: String?) {
        viewModelScope.launch {
            repo.onWifiChanged(ssid)
        }
    }

    fun removeWifiZone(ssid: String) {
        launchOperation("WiFi Zone Removed") {
            repo.removeWifiZone(ssid)
        }
    }

    val locationZones: StateFlow<List<LocationZone>> = repo.getLocationZonesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val importantContacts: StateFlow<List<ImportantContact>> = repo.getImportantContactsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addImportantContact(name: String, phoneNumber: String) {
        launchOperation("Important contact added!") {
            val contact = ImportantContact(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                phoneNumber = phoneNumber
            )
            repo.addImportantContact(contact)
        }
    }

    fun removeImportantContact(phoneNumber: String) {
        launchOperation("Important contact removed!") {
            repo.removeImportantContact(phoneNumber)
        }
    }

    fun addCurrentLocationZone(mode: RingerMode, radius: Float = 50f) {
        _operationState.value = OperationState.Loading
        repo.getCurrentLocation(
            onLocationResult = { lat, lon ->
                addLocationZone(lat, lon, "Zone ${locationZones.value.size + 1}", mode, radius)
                // Note: success state is handled inside addLocationZone now to ensure proper sequencing
            },
            onError = {
                Log.e("SilentModeViewModel", "Could not get current location")
                _operationState.value = OperationState.Error("Could not get location")
                viewModelScope.launch {
                    delay(3000)
                    _operationState.value = OperationState.Idle
                }
            }
        )
    }

    fun addLocationZone(latitude: Double, longitude: Double, name: String, mode: RingerMode, radius: Float = 50f) {
        viewModelScope.launch {
            try {
                val zone = LocationZone(
                    id = java.util.UUID.randomUUID().toString(),
                    latitude = latitude,
                    longitude = longitude,
                    name = name,
                    radius = radius,
                    mode = mode
                )
                repo.addLocationZone(zone)
                _operationState.value = OperationState.Success("Location Zone Added!")
                delay(2000)
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("Failed to add zone: ${e.message}")
                delay(3000)
                _operationState.value = OperationState.Idle
            }
        }
    }

    fun removeLocationZone(id: String) {
        launchOperation("Location Zone Removed") {
            repo.removeLocationZone(id)
        }
    }
}