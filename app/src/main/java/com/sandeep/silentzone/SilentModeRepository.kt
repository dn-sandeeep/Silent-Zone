package com.sandeep.silentzone

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.BatteryManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.sandeep.silentzone.data.ImportantContactEntity
import com.sandeep.silentzone.data.LocationZoneEntity
import com.sandeep.silentzone.data.SilentZoneDao
import com.sandeep.silentzone.data.WifiZoneEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class LocationZone(
        val id: String,
        val latitude: Double,
        val longitude: Double,
        val name: String,
        val radius: Float,
        val mode: RingerMode = RingerMode.SILENT // Default for migration
)

data class WifiZone(
        val ssid: String,
        val mode: RingerMode,
        val latitude: Double? = null,
        val longitude: Double? = null
)

data class ImportantContact(val id: String, val name: String, val phoneNumber: String)

data class AnalyticsEvent(
        val id: Long,
        val zoneName: String,
        val zoneType: String,
        val entryTime: Long,
        val exitTime: Long?,
        val durationMillis: Long,
        val mode: RingerMode
)

data class BatteryUsage(
    val totalImpact: Double,
    val wifiImpact: Double,
    val locationImpact: Double,
    val idleImpact: Double
)

@Singleton
class SilentModeRepository
@Inject
constructor(
        @ApplicationContext private val appContext: Context,
        private val dao: SilentZoneDao,
        private val geofenceManager: SilentZoneGeofenceManager,
        private val analytics: com.sandeep.silentzone.utils.AnalyticsHelper
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val audio: AudioManager =
            appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _currentModeFlow = kotlinx.coroutines.flow.MutableStateFlow(getCurrentMode())
    val currentModeFlow: kotlinx.coroutines.flow.StateFlow<RingerMode> =
            _currentModeFlow.asStateFlow()

    private var isNetworkCallbackRegistered = false

    private val _fallbackEvents = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(replay = 0)
    val fallbackEvents = _fallbackEvents.asSharedFlow()

    fun refreshMode() {
        val current = getCurrentMode()
        _currentModeFlow.value = current

        // If the device is in NORMAL mode, it's not a "peaceful" session.
        // Close any active sessions to prevent calculating time when the mode isn't changed or manually overridden.
        if (current == RingerMode.NORMAL) {
            scope.launch {
                sanitizeAnalytics()
            }
        }
    }

    private val fusedLocationClient =
            com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(
                    appContext
            )
    private val prefs = appContext.getSharedPreferences("silent_zone_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_KEY_SAVED_MODE = "saved_ringer_mode"
        private const val PREF_KEY_ACTIVE_WIFI_SET = "active_wifi_set"
        private const val PREF_KEY_ACTIVE_LOCATION_SET = "active_location_set"
        private const val PREF_KEY_ACTIVE_PROXY_SET = "active_proxy_set"
        private const val PREF_KEY_TOTAL_REPORTED_MILLIS = "total_reported_millis"
        private const val ACTION_NETWORK_CALLBACK = "com.sandeep.silentzone.ACTION_NETWORK_CALLBACK"
    }

    private val wifiMutex = Mutex()
    private var lastProcessedSsid: String? = null
    private var lastWifiProcessTime: Long = 0

    private val networkPendingIntent: PendingIntent by lazy {
        val intent =
                Intent(appContext, NetworkChangeReceiver::class.java).apply {
                    action = ACTION_NETWORK_CALLBACK
                }
        PendingIntent.getBroadcast(
                appContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private fun getActiveWifiSet(): Set<String> =
            prefs.getStringSet(PREF_KEY_ACTIVE_WIFI_SET, emptySet()) ?: emptySet()
    private fun getActiveLocationSet(): Set<String> =
            prefs.getStringSet(PREF_KEY_ACTIVE_LOCATION_SET, emptySet()) ?: emptySet()

    private fun addToWifiSet(ssid: String) {
        val current = getActiveWifiSet().toMutableSet()
        current.add(ssid)
        prefs.edit().putStringSet(PREF_KEY_ACTIVE_WIFI_SET, current).apply()
    }

    private fun removeFromWifiSet(ssid: String) {
        val current = getActiveWifiSet().toMutableSet()
        current.remove(ssid)
        prefs.edit().putStringSet(PREF_KEY_ACTIVE_WIFI_SET, current).apply()
    }

    private fun addToLocationSet(id: String) {
        val current = getActiveLocationSet().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet(PREF_KEY_ACTIVE_LOCATION_SET, current).apply()
    }

    private fun removeFromLocationSet(id: String) {
        val current = getActiveLocationSet().toMutableSet()
        current.remove(id)
        prefs.edit().putStringSet(PREF_KEY_ACTIVE_LOCATION_SET, current).apply()
    }

    private fun getActiveProxySet(): Set<String> =
            prefs.getStringSet(PREF_KEY_ACTIVE_PROXY_SET, emptySet()) ?: emptySet()

    private fun addToProxySet(ssid: String) {
        val current = getActiveProxySet().toMutableSet()
        current.add(ssid)
        prefs.edit().putStringSet(PREF_KEY_ACTIVE_PROXY_SET, current).apply()
    }

    private fun removeFromProxySet(ssid: String) {
        val current = getActiveProxySet().toMutableSet()
        current.remove(ssid)
        prefs.edit().putStringSet(PREF_KEY_ACTIVE_PROXY_SET, current).apply()
    }

    private fun registerWifiCallback() {
        val connectivityManager =
                appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request =
                NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build()
        try {
            // ALWAYS unregister existing ones first to prevent leaks on Samsung
            try {
                connectivityManager.unregisterNetworkCallback(networkPendingIntent)
            } catch (e: Exception) {}

            connectivityManager.registerNetworkCallback(request, networkPendingIntent)
            isNetworkCallbackRegistered = true
            android.util.Log.d("SilentModeRepo", "Background WiFi callback registered.")
        } catch (e: Exception) {
            android.util.Log.e(
                    "SilentModeRepo",
                    "Failed to register background callback: ${e.message}"
            )
        }
    }

    private fun unregisterWifiCallback() {
        val connectivityManager =
                appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            connectivityManager.unregisterNetworkCallback(networkPendingIntent)
            isNetworkCallbackRegistered = false
            android.util.Log.d("SilentModeRepo", "Background WiFi callback unregistered.")
        } catch (e: Exception) {}
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun getCurrentLocation(onLocationResult: (Double, Double) -> Unit, onError: () -> Unit) {
        // High accuracy request instead of just lastLocation
        val priority = com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
        fusedLocationClient
                .getCurrentLocation(priority, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        onLocationResult(location.latitude, location.longitude)
                    } else {
                        // Fallback to lastLocation if fresh one fails
                        fusedLocationClient.lastLocation
                                .addOnSuccessListener { lastLoc ->
                                    if (lastLoc != null) {
                                        onLocationResult(lastLoc.latitude, lastLoc.longitude)
                                    } else {
                                        onError()
                                    }
                                }
                                .addOnFailureListener { onError() }
                    }
                }
                .addOnFailureListener { onError() }
    }

    fun hasPolicyAccess(): Boolean {
        val notif =
                appContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                        android.app.NotificationManager
        return notif.isNotificationPolicyAccessGranted
    }

    fun getCurrentMode(): RingerMode =
            when (audio.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> RingerMode.SILENT
                AudioManager.RINGER_MODE_VIBRATE -> RingerMode.VIBRATE
                AudioManager.RINGER_MODE_NORMAL -> RingerMode.NORMAL
                else -> RingerMode.NORMAL
            }

    fun saveOriginalMode() {
        if (prefs.getInt(PREF_KEY_SAVED_MODE, -1) == -1) {
            val currentMode = getCurrentMode()
            prefs.edit().putInt(PREF_KEY_SAVED_MODE, currentMode.ordinal).apply()
            android.util.Log.d("SilentModeRepo", "Saved original mode: $currentMode")
        }
    }

    fun restoreOriginalMode() {
        val savedModeOrdinal = prefs.getInt(PREF_KEY_SAVED_MODE, -1)
        if (savedModeOrdinal != -1) {
            val savedMode = RingerMode.values()[savedModeOrdinal]
            android.util.Log.d("SilentModeRepo", "Restoring to original mode: $savedMode")
            applyMode(savedMode)
            prefs.edit().remove(PREF_KEY_SAVED_MODE).apply()
        }
    }

    private fun applyMode(mode: RingerMode) {
        val previousMode = getCurrentMode()
        var targetMode = mode
        if (!hasPolicyAccess() && mode == RingerMode.SILENT) {
            targetMode = RingerMode.VIBRATE
            @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            GlobalScope.launch(Dispatchers.Main) { _fallbackEvents.emit(Unit) }
        }

        try {
            when (targetMode) {
                RingerMode.SILENT -> audio.ringerMode = AudioManager.RINGER_MODE_SILENT
                RingerMode.VIBRATE -> audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                RingerMode.NORMAL -> audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
            refreshMode()
            
            val newMode = getCurrentMode()
            if (previousMode != newMode) {
                analytics.logModeTransition(previousMode.name, newMode.name)
            }
        } catch (e: Exception) {
            android.util.Log.e("SilentModeRepo", "Failed to apply mode $targetMode: ${e.message}")
        }
    }

    fun setSilent(): RingerMode {
        applyMode(RingerMode.SILENT)
        return getCurrentMode()
    }

    fun setVibrate(): RingerMode {
        applyMode(RingerMode.VIBRATE)
        return getCurrentMode()
    }

    fun setNormal(): RingerMode {
        applyMode(RingerMode.NORMAL)
        return getCurrentMode()
    }

    private fun startMonitoringService(zoneName: String) {
        val intent =
                Intent(appContext, SilentZoneService::class.java).apply {
                    putExtra(SilentZoneService.EXTRA_ZONE_NAME, zoneName)
                }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e(
                    "SilentModeRepo",
                    "Failed to start monitoring service for $zoneName: ${e.message}"
            )
        }
    }

    private fun stopMonitoringService() {
        val intent = Intent(appContext, SilentZoneService::class.java)
        appContext.stopService(intent)
    }

    suspend fun reRegisterAllTriggers() {
        android.util.Log.d("SilentModeRepo", "Re-registering all triggers...")

        // 1. Re-register Location Zones
        val locationZones = getLocationZones()
        locationZones.forEach { zone ->
            geofenceManager.addGeofence(zone.id, zone.latitude, zone.longitude, zone.radius)
        }

        // 2. Re-register WiFi Proxy Geofences
        val wifiZones = getWifiZonesFlow().first()
        wifiZones.forEach { wifiZone ->
            if (wifiZone.latitude != null && wifiZone.longitude != null) {
                geofenceManager.addGeofence(
                        requestId = "wifi_proxy_${wifiZone.ssid}",
                        latitude = wifiZone.latitude,
                        longitude = wifiZone.longitude,
                        radius = 100f
                )
            }
        }
    }

    suspend fun onWifiChanged(currentSsid: String?) {
        wifiMutex.withLock {
            val now = System.currentTimeMillis()
            // Deduplication: Ignore identical SSID changes within 2 seconds
            if (currentSsid == lastProcessedSsid && (now - lastWifiProcessTime) < 2000) {
                return@withLock
            }
            lastProcessedSsid = currentSsid
            lastWifiProcessTime = now

            val zones = getWifiZonesFlow().first()
            val activeWifiSet = getActiveWifiSet()

            if (currentSsid == android.net.wifi.WifiManager.UNKNOWN_SSID) {
                android.util.Log.w(
                        "SilentModeRepo",
                        "Received UNKNOWN_SSID due to background limits. Deferring disconnect to prevent wrong mode switch."
                )
                return@withLock
            }

            if (currentSsid != null) {
                val zone = zones.find { it.ssid == currentSsid }
                if (zone != null) {
                    val isNewlyDetected = !activeWifiSet.contains(currentSsid)
                    val hasActiveSession = dao.getActiveEventByZone(currentSsid) != null

                    if (isNewlyDetected && !hasActiveSession) {
                        android.util.Log.d("SilentModeRepo", "WiFi Zone detected (New: $isNewlyDetected, ActiveSession: $hasActiveSession). Starting session.")
                        saveOriginalMode()
                        if (isNewlyDetected) addToWifiSet(currentSsid)
                        applyMode(zone.mode)
                        logEntry(currentSsid, "WIFI", zone.mode)
                        unregisterWifiCallback() // Stop background listener, service takes over
                        startMonitoringService(currentSsid)
                    }
                } else if (activeWifiSet.isNotEmpty()) {
                    // Not in a saved zone anymore
                    android.util.Log.d("SilentModeRepo", "Exited WiFi zone: $activeWifiSet")
                    activeWifiSet.forEach {
                        removeFromWifiSet(it)
                        logExit(it)
                    }
                }
            } else if (activeWifiSet.isNotEmpty()) {
                // Disconnected from WiFi (currentSsid is null)
                android.util.Log.d("SilentModeRepo", "WiFi disconnected. Clearing active zones.")
                activeWifiSet.forEach {
                    removeFromWifiSet(it)
                    logExit(it)
                }
            }

            // Always sync state to ensure persistent monitoring notification is active
            checkAndRestore()
        }
    }

    suspend fun onLocationTransition(id: String, transitionType: Int) {
        val isEntering =
                transitionType == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER
        val activeProxySet = getActiveProxySet()

        if (id.startsWith("wifi_proxy_")) {
            val ssid = id.removePrefix("wifi_proxy_")
            if (isEntering) {
                addToProxySet(ssid)
            } else {
                removeFromProxySet(ssid)
                removeFromWifiSet(ssid)
                checkAndRestore()
            }
            return
        }

        // Use direct DB lookup to avoid race conditions with INITIAL_TRIGGER
        val entity =
                dao.getLocationZoneById(id)
                        ?: run {
                            android.util.Log.e("SilentModeRepo", "Zone ID $id not found in DB")
                            return
                        }
        val zone = entity.toDomain()

        val activeLocationSet = getActiveLocationSet()

        if (isEntering) {
            saveOriginalMode()
            addToLocationSet(id)
            applyMode(zone.mode)
            logEntry(zone.name, "LOCATION", zone.mode)
            try {
                startMonitoringService(zone.name)
            } catch (e: Exception) {
                android.util.Log.e(
                        "SilentModeRepo",
                        "Failed to start monitoring service for ${zone.name}: ${e.message}"
                )
            }
        } else if (activeLocationSet.contains(id)) {
            removeFromLocationSet(id)
            logExit(zone.name)
            checkAndRestore()
        }
    }

    suspend fun syncCurrentState(currentSsid: String?) {
        android.util.Log.d("SilentModeRepo", "Syncing current state (SSID: $currentSsid)...")

        // 1. WiFi Sync
        onWifiChanged(currentSsid)

        // 2. Location Sync (check if we are in any saved geofence zone)
        // Note: Play Services handles Geofencing, but we can do a manual backup check
        // against last known location to be safe.
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                scope.launch {
                    val zones = getLocationZones()
                    val activeLocationSet = getActiveLocationSet().toMutableSet()

                    zones.forEach { zone ->
                        val results = FloatArray(1)
                        android.location.Location.distanceBetween(
                                location.latitude,
                                location.longitude,
                                zone.latitude,
                                zone.longitude,
                                results
                        )
                        val isInside = results[0] <= zone.radius
                        val isAlreadyInSet = activeLocationSet.contains(zone.id)
                        val hasActiveSession = dao.getActiveEventByZone(zone.name) != null

                        if (isInside && (!isAlreadyInSet && !hasActiveSession)) {
                            android.util.Log.d(
                                    "SilentModeRepo",
                                    "Sync: Found inside zone ${zone.name} (InSet: $isAlreadyInSet, ActiveSession: $hasActiveSession)"
                            )
                            saveOriginalMode()
                            if (!isAlreadyInSet) addToLocationSet(zone.id)
                            applyMode(zone.mode)
                            logEntry(zone.name, "LOCATION", zone.mode)
                            try {
                                startMonitoringService(zone.name)
                            } catch (e: Exception) {}
                        }
                    }
                }
            }
        }
    }

    private suspend fun checkAndRestore() {
        val activeWifiSet = getActiveWifiSet()
        val activeLocationSet = getActiveLocationSet()
        val activeProxySet = getActiveProxySet()

        android.util.Log.d(
                "SilentModeRepo",
                "checkAndRestore: Wifi=${activeWifiSet.size}, Loc=${activeLocationSet.size}, Proxy=${activeProxySet.size}"
        )

        // Priority 1: If any SILENT zone is truly ACTIVE
        if (activeWifiSet.isNotEmpty() || activeLocationSet.isNotEmpty()) {
            val wifiZone =
                    if (activeWifiSet.isNotEmpty()) {
                        val ssid = activeWifiSet.first()
                        getWifiZonesFlow().first().find { it.ssid == ssid }
                    } else null

            val locZone =
                    if (activeLocationSet.isNotEmpty()) {
                        val id = activeLocationSet.first()
                        getLocationZones().find { it.id == id }
                    } else null

            val targetMode = locZone?.mode ?: wifiZone?.mode ?: RingerMode.SILENT
            val zoneName = locZone?.name ?: wifiZone?.ssid ?: "Active Zone"

            saveOriginalMode()
            applyMode(targetMode)

            // Clean up background listener when service is active
            unregisterWifiCallback()
            startMonitoringService(zoneName)
        }
        // Priority 2: If we are ONLY searching (in proximity)
        else if (activeProxySet.isNotEmpty()) {
            android.util.Log.d(
                    "SilentModeRepo",
                    "In Proxy-only area. Restoring original mode while searching..."
            )
            restoreOriginalMode()
            registerWifiCallback()
            startMonitoringService("Monitoring")
        }
        // Priority 3: Nothing active
        else {
            android.util.Log.d(
                    "SilentModeRepo",
                    "Nothing active. Maintaining persistent monitoring."
            )
            restoreOriginalMode()
            unregisterWifiCallback()
            startMonitoringService("Monitoring")
        }
    }

    // Location Zone Management
    fun getLocationZonesFlow(): Flow<List<LocationZone>> {
        return dao.getAllLocationZones().map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun getLocationZones(): List<LocationZone> {
        return getLocationZonesFlow().first()
    }

    suspend fun addLocationZone(zone: LocationZone) {
        dao.insertLocationZone(zone.toEntity())
        geofenceManager.addGeofence(zone.id, zone.latitude, zone.longitude, zone.radius)
    }

    suspend fun removeLocationZone(id: String) {
        val zone = dao.getLocationZoneById(id)?.toDomain()
        dao.deleteLocationZoneById(id)
        geofenceManager.removeGeofence(id)
        if (getActiveLocationSet().contains(id)) {
            removeFromLocationSet(id)
            if (zone != null) logExit(zone.name)
            checkAndRestore()
        }
    }

    // WiFi Zone Management
    fun getWifiZonesFlow(): Flow<List<WifiZone>> {
        return dao.getAllWifiZones().map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun addWifiZone(wifiZone: WifiZone) {
        dao.insertWifiZone(wifiZone.toEntity())
        // Add a 150m proxy geofence if coordinates are available
        if (wifiZone.latitude != null && wifiZone.longitude != null) {
            geofenceManager.addGeofence(
                    requestId = "wifi_proxy_${wifiZone.ssid}",
                    latitude = wifiZone.latitude,
                    longitude = wifiZone.longitude,
                    radius = 100f
            )
        }
    }

    suspend fun removeWifiZone(ssid: String) {
        dao.deleteWifiZoneBySsid(ssid)
        geofenceManager.removeGeofence("wifi_proxy_$ssid")
        if (getActiveWifiSet().contains(ssid)) {
            removeFromWifiSet(ssid)
            logExit(ssid)
            checkAndRestore()
        }
    }

    // Important Contact Management
    fun getImportantContactsFlow(): Flow<List<ImportantContact>> {
        return dao.getAllImportantContacts().map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun getImportantContacts(): List<ImportantContact> {
        return getImportantContactsFlow().first()
    }

    suspend fun addImportantContact(contact: ImportantContact) {
        dao.insertImportantContact(contact.toEntity())
    }

    suspend fun removeImportantContact(phoneNumber: String) {
        dao.deleteImportantContactByNumber(phoneNumber)
    }

    suspend fun isImportantContact(phoneNumber: String): Boolean {
        val normalizedIncoming = normalizePhoneNumber(phoneNumber)
        return getImportantContacts().any {
            normalizePhoneNumber(it.phoneNumber) == normalizedIncoming
        }
    }

    private fun normalizePhoneNumber(phone: String): String {
        return phone.replace(Regex("[^0-9+]"), "")
    }

    // Analytics Helpers
    private suspend fun logEntry(zoneName: String, zoneType: String, mode: RingerMode) {
        val now = System.currentTimeMillis()
        // Ensure only one active session at a time
        dao.closeAllActiveEvents(now)
        
        val event =
                com.sandeep.silentzone.data.AnalyticsEventEntity(
                        zoneName = zoneName,
                        zoneType = zoneType,
                        entryTime = now,
                        mode = mode
                )
        dao.insertAnalyticsEvent(event)
    }

    suspend fun sanitizeAnalytics() {
        // Close any "Active" sessions that are stale (e.g., from a previous run)
        val now = System.currentTimeMillis()
        dao.closeAllActiveEvents(now)
    }

    private suspend fun logExit(zoneName: String) {
        val activeEvent = dao.getActiveEventByZone(zoneName)
        if (activeEvent != null) {
            val exitTime = System.currentTimeMillis()
            val durationMillis = exitTime - activeEvent.entryTime
            val durationMinutes = durationMillis / 60000

            dao.updateAnalyticsEvent(
                    activeEvent.copy(exitTime = exitTime, durationMillis = durationMillis)
            )
            val bundle =
                    android.os.Bundle().apply {
                        putString("zone_name", zoneName)
                        putString("zone_type", activeEvent.zoneType)
                        putLong("duration_millis", durationMillis)
                        putLong("duration_minutes", durationMinutes)
                    }
            com.google.firebase.analytics.FirebaseAnalytics.getInstance(appContext)
                    .logEvent("silent_zone_session", bundle)
        }
    }

    suspend fun reportServiceUsage() {
        val allEvents = dao.getEventsSinceList(0)
        val cumulativeTotalMillis = allEvents.sumOf {
            it.durationMillis + (if (it.exitTime == null) System.currentTimeMillis() - it.entryTime else 0)
        }

        val lastReported = prefs.getLong(PREF_KEY_TOTAL_REPORTED_MILLIS, 0L)
        val deltaMillis = cumulativeTotalMillis - lastReported

        // Report if we have at least 1 new minute of usage
        if (deltaMillis >= 60000) {
            val deltaMinutes = deltaMillis / 60000
            val bundle = android.os.Bundle().apply {
                putLong("minutes", deltaMinutes)
                putLong("cumulative_minutes", cumulativeTotalMillis / 60000)
            }
            
            com.google.firebase.analytics.FirebaseAnalytics.getInstance(appContext)
                .logEvent("background_service_usage_heartbeat", bundle)

            // Log estimated battery impact and total zones
            val batteryUsage = getBatteryUsageFlow().first()
            analytics.logEstimatedBatteryImpact(
                batteryUsage.totalImpact,
                batteryUsage.wifiImpact,
                batteryUsage.locationImpact
            )

            val wifiCount = dao.getWifiZonesCount()
            val locationCount = dao.getLocationZonesCount()
            analytics.logTotalZones(wifiCount, locationCount)

            // Update baseline (keeping it minute-aligned to avoid rounding drift)
            prefs.edit().putLong(PREF_KEY_TOTAL_REPORTED_MILLIS, lastReported + (deltaMinutes * 60000)).apply()

            // Update user property for segmentation (Total Hours)
            com.google.firebase.analytics.FirebaseAnalytics.getInstance(appContext)
                .setUserProperty("total_service_hours", (cumulativeTotalMillis / 3600000).toString())
            
            android.util.Log.d("SilentModeRepo", "Reported $deltaMinutes mins of background usage. Total: ${cumulativeTotalMillis/60000} mins.")
        }
    }

    suspend fun reportServiceUptimeHeartbeat() {
        val servicePrefs = appContext.getSharedPreferences("service_prefs", Context.MODE_PRIVATE)
        val startTime = servicePrefs.getLong("service_start_time", 0L)

        if (startTime == 0L) return

        val currentTime = System.currentTimeMillis()
        val deltaMillis = currentTime - startTime

        // Report if we have at least 5 minutes of new usage
        if (deltaMillis >= 5 * 60 * 1000) {
            val deltaMinutes = deltaMillis / 60000

            val bundle = android.os.Bundle().apply {
                putLong("uptime_minutes_delta", deltaMinutes)
            }
            com.google.firebase.analytics.FirebaseAnalytics.getInstance(appContext)
                .logEvent("service_uptime_heartbeat", bundle)

            val cumulativeTotalMillis = servicePrefs.getLong("cumulative_service_uptime_millis", 0L) + deltaMillis
            servicePrefs.edit().putLong("cumulative_service_uptime_millis", cumulativeTotalMillis).apply()

            com.google.firebase.analytics.FirebaseAnalytics.getInstance(appContext)
                .setUserProperty("total_service_uptime_hours", (cumulativeTotalMillis / 3600000).toString())

            servicePrefs.edit().putLong("service_start_time", currentTime).apply()
            android.util.Log.d("SilentModeRepo", "Reported $deltaMinutes mins of service uptime. Total: ${cumulativeTotalMillis/3600000} hours.")
        }
    }

    fun getRecentAnalyticsFlow(): Flow<List<AnalyticsEvent>> {
        return dao.getRecentEvents().map { entities -> entities.map { it.toDomain() } }
    }

    fun getDailyAnalyticsFlow(): Flow<Long> {
        val startOfDay = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }.timeInMillis

        return dao.getEventsSince(startOfDay).map { events ->
            events.sumOf { event ->
                if (event.exitTime != null) {
                    event.durationMillis
                } else {
                    System.currentTimeMillis() - event.entryTime
                }
            }
        }
    }

    fun getActiveSessionFlow(): Flow<AnalyticsEvent?> {
        return dao.getRecentEvents().map { events ->
            events.find { it.exitTime == null }?.toDomain()
        }
    }

    fun getLifetimePeacefulTimeFlow(): Flow<Long> {
        return dao.getEventsSince(0).map { events ->
            events.sumOf { event ->
                if (event.exitTime != null) {
                    event.durationMillis
                } else {
                    System.currentTimeMillis() - event.entryTime
                }
            }
        }
    }

    fun getBatteryUsageFlow(): Flow<BatteryUsage> {
        val startOfDay = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }.timeInMillis

        return dao.getEventsSince(startOfDay).map { events ->
            var wifiMillis = 0L
            var locationMillis = 0L
            
            events.forEach { event ->
                val duration = event.durationMillis + (if (event.exitTime == null) System.currentTimeMillis() - event.entryTime else 0)
                if (event.zoneType == "WIFI") {
                    wifiMillis += duration
                } else {
                    locationMillis += duration
                }
            }

            // Realistic Weights (estimated % per hour)
            val wifiWeight = 0.015 // 0.015% per hour
            val locationWeight = 0.08 // 0.08% per hour
            val idleWeight = 0.005 // 0.005% per hour

            val wifiImpact = (wifiMillis / 3600000.0) * wifiWeight
            val locationImpact = (locationMillis / 3600000.0) * locationWeight
            
            // Total time since start of day
            val totalTimeMillis = System.currentTimeMillis() - startOfDay
            val idleMillis = (totalTimeMillis - wifiMillis - locationMillis).coerceAtLeast(0L)
            val idleImpact = (idleMillis / 3600000.0) * idleWeight

            val totalImpact = wifiImpact + locationImpact + idleImpact

            BatteryUsage(
                totalImpact = (totalImpact * 100).round(2) / 100.0,
                wifiImpact = (wifiImpact * 100).round(2) / 100.0,
                locationImpact = (locationImpact * 100).round(2) / 100.0,
                idleImpact = (idleImpact * 100).round(2) / 100.0
            )
        }
    }

    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun getCurrentSsid(): String? {
        return try {
            val wifiManager =
                    appContext.getSystemService(Context.WIFI_SERVICE) as
                            android.net.wifi.WifiManager
            var isConnectedButUnknown = false

            // Priority 1: ConnectionInfo
            @Suppress("DEPRECATION") val info = wifiManager.connectionInfo
            if (info != null && info.networkId != -1) {
                @Suppress("DEPRECATION") val ssid = info.ssid
                if (ssid != null && ssid != android.net.wifi.WifiManager.UNKNOWN_SSID) {
                    return ssid.trim('"')
                } else {
                    isConnectedButUnknown = true
                }
            }

            // Priority 2: NetworkCapabilities
            val cm =
                    appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as
                            android.net.ConnectivityManager
            val network = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(network)
            val wifiInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                capabilities?.transportInfo as? android.net.wifi.WifiInfo
            } else {
                null
            }
            if (wifiInfo != null) {
                @Suppress("DEPRECATION") val ssid = wifiInfo.ssid
                if (ssid != null && ssid != android.net.wifi.WifiManager.UNKNOWN_SSID) {
                    return ssid.trim('"')
                } else {
                    isConnectedButUnknown = true
                }
            }

            if (isConnectedButUnknown) android.net.wifi.WifiManager.UNKNOWN_SSID else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun syncCurrentState() {
        syncCurrentState(getCurrentSsid())
    }

    /**
     * Called when the user turns GPS/Location OFF from system settings. We clear all active
     * location-based zone activations and restore the original mode, because we can no longer
     * reliably detect geofence exits.
     */
    suspend fun onLocationProviderDisabled() {
        val activeLocationSet = getActiveLocationSet()
        if (activeLocationSet.isNotEmpty()) {
            android.util.Log.d(
                    "SilentModeRepo",
                    "Location provider disabled. Clearing ${activeLocationSet.size} active location zones."
            )
            activeLocationSet.forEach { removeFromLocationSet(it) }
            checkAndRestore()
        }
    }

    // Mappers
    private fun LocationZoneEntity.toDomain() =
            LocationZone(id, latitude, longitude, name, radius, mode)
    private fun LocationZone.toEntity() =
            LocationZoneEntity(id, latitude, longitude, name, radius, mode)
    private fun WifiZoneEntity.toDomain() = WifiZone(ssid, mode, latitude, longitude)
    private fun WifiZone.toEntity() = WifiZoneEntity(ssid, mode, latitude, longitude)
    private fun ImportantContactEntity.toDomain() = ImportantContact(id, name, phoneNumber)
    private fun ImportantContact.toEntity() = ImportantContactEntity(id, name, phoneNumber)
    private fun com.sandeep.silentzone.data.AnalyticsEventEntity.toDomain() =
            AnalyticsEvent(id, zoneName, zoneType, entryTime, exitTime, durationMillis, mode)
}
