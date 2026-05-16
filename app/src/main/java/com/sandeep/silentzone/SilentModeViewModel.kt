package com.sandeep.silentzone

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val repo: SilentModeRepository,
    private val analytics: com.sandeep.silentzone.utils.AnalyticsHelper,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    private val _isFallback = MutableStateFlow(false)
    private val _permissionRefreshTick = MutableStateFlow(0)
    private var lastForegroundLocationGranted: Boolean? = null
    private var lastBackgroundLocationGranted: Boolean? = null
    private var lastWifiAutomationPermissionGranted: Boolean? = null

    private val _availableSsidList = MutableStateFlow<List<String>>(emptyList())
    val availableSsidList: StateFlow<List<String>> = _availableSsidList.asStateFlow()

    private val prefs = appContext.getSharedPreferences("analytics_prefs", Context.MODE_PRIVATE)

    init {
        // Initial DND permission check
        val isGranted = repo.hasPolicyAccess()
        analytics.logDndPermissionStatus(isGranted)
        prefs.edit().putBoolean("last_dnd_status", isGranted).apply()

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
        _isFallback,
        repo.getBatteryUsageFlow(),
        _permissionRefreshTick
    ) { mode, msg, fallback, battery, _ ->
        UiState(
            accessGranted = repo.hasPolicyAccess(),
            currentMode = mode,
            isFallback = fallback && !repo.hasPolicyAccess(),
            message = msg,
            hasForegroundLocation = hasForegroundLocationPermission(),
            hasBackgroundLocation = hasBackgroundLocationPermission(),
            hasWifiAutomationPermission = hasWifiAutomationPermission(),
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(),
            batteryUsage = battery
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UiState(
            accessGranted = repo.hasPolicyAccess(),
            currentMode = repo.getCurrentMode(),
            isFallback = false,
            message = null,
            hasForegroundLocation = hasForegroundLocationPermission(),
            hasBackgroundLocation = hasBackgroundLocationPermission(),
            hasWifiAutomationPermission = hasWifiAutomationPermission(),
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations()
        )
    )

    private fun hasForegroundLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun hasWifiAutomationPermission(): Boolean {
        if (!hasForegroundLocationPermission()) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.NEARBY_WIFI_DEVICES
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            return pm.isIgnoringBatteryOptimizations(appContext.packageName)
        }
        return true
    }

    fun refresh() {
        repo.refreshMode()
        
        // Track DND permission status change
        val currentStatus = repo.hasPolicyAccess()
        val lastStatus = prefs.getBoolean("last_dnd_status", false)
        if (currentStatus != lastStatus) {
            analytics.logDndPermissionStatus(currentStatus)
            prefs.edit().putBoolean("last_dnd_status", currentStatus).apply()
        }
    }

    fun logNavigateToZones() {
        analytics.logNavigateToZones()
    }

    fun logClickCreateZone() {
        analytics.logClickCreateZone()
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

    fun addWifiZone(ssid: String, mode: RingerMode, latitude: Double? = null, longitude: Double? = null) {
        launchOperation("WiFi Zone Added!") {
            repo.addWifiZone(WifiZone(ssid, mode, latitude, longitude))
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

    fun refreshAutomationPermissionHealth(currentSsid: String?) {
        val foregroundGranted = hasForegroundLocationPermission()
        val backgroundGranted = hasBackgroundLocationPermission()
        val wifiGranted = hasWifiAutomationPermission()
        val previousForeground = lastForegroundLocationGranted
        val previousBackground = lastBackgroundLocationGranted
        val previousWifi = lastWifiAutomationPermissionGranted

        lastForegroundLocationGranted = foregroundGranted
        lastBackgroundLocationGranted = backgroundGranted
        lastWifiAutomationPermissionGranted = wifiGranted
        _permissionRefreshTick.value += 1

        viewModelScope.launch {
            if (!foregroundGranted || !backgroundGranted) {
                repo.pauseLocationAutomation()
            }
            if (!wifiGranted) {
                repo.pauseWifiAutomation()
            }

            val locationRestored =
                previousForeground != null &&
                    previousBackground != null &&
                    (!previousForeground || !previousBackground) &&
                    foregroundGranted &&
                    backgroundGranted
            val wifiRestored = previousWifi != null && !previousWifi && wifiGranted

            if (locationRestored) {
                repo.resumeAutomation(if (wifiGranted) currentSsid else null)
            } else if (wifiRestored) {
                repo.onWifiChanged(currentSsid)
            }
        }
    }

    fun updateWifiZoneMode(ssid: String, mode: RingerMode) {
        launchOperation("WiFi Zone Updated") {
            repo.updateWifiZoneMode(ssid, mode)
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

    fun updateLocationZone(zone: LocationZone) {
        launchOperation("Location Zone Updated") {
            repo.updateLocationZone(zone)
        }
    }

    val recentAnalytics: StateFlow<List<AnalyticsEvent>> = repo.getRecentAnalyticsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyPeacefulTime: StateFlow<Long> = repo.getDailyAnalyticsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val lifetimePeacefulTime: StateFlow<Long> = repo.getLifetimePeacefulTimeFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val activeSession: StateFlow<AnalyticsEvent?> = repo.getActiveSessionFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _updateReadyToInstall = MutableStateFlow(false)
    val updateReadyToInstall: StateFlow<Boolean> = _updateReadyToInstall.asStateFlow()

    fun setUpdateReadyToInstall(ready: Boolean) {
        _updateReadyToInstall.value = ready
    }
}
