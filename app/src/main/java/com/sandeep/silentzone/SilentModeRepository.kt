package com.sandeep.silentzone

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import com.sandeep.silentzone.data.ImportantContactEntity
import com.sandeep.silentzone.data.LocationZoneEntity
import com.sandeep.silentzone.data.SilentZoneDao
import com.sandeep.silentzone.data.WifiZoneEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

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
    val mode: RingerMode
)

data class ImportantContact(
    val id: String,
    val name: String,
    val phoneNumber: String
)

@Singleton
class SilentModeRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val dao: SilentZoneDao,
    private val geofenceManager: SilentZoneGeofenceManager
) {
    private val audio: AudioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notif: NotificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    private val _currentModeFlow = kotlinx.coroutines.flow.MutableStateFlow(getCurrentMode())
    val currentModeFlow: kotlinx.coroutines.flow.StateFlow<RingerMode> = _currentModeFlow.asStateFlow()

    fun refreshMode() {
        _currentModeFlow.value = getCurrentMode()
    }
    
    private val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(appContext)
    private val prefs = appContext.getSharedPreferences("silent_zone_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_KEY_SAVED_MODE = "saved_ringer_mode"
        private const val PREF_KEY_ACTIVE_WIFI_SSID = "active_wifi_ssid"
        private const val PREF_KEY_ACTIVE_LOCATION_ID = "active_location_id"
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun getCurrentLocation(onLocationResult: (Double, Double) -> Unit, onError: () -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
             if (location != null) {
                 onLocationResult(location.latitude, location.longitude)
             } else {
                 onError()
             }
        }.addOnFailureListener {
            onError()
        }
    }

    fun hasPolicyAccess(): Boolean {
        return notif.isNotificationPolicyAccessGranted
    }

    fun getCurrentMode(): RingerMode = when (audio.ringerMode) {
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
        if (!hasPolicyAccess() && mode == RingerMode.SILENT) return
        try {
            when (mode) {
                RingerMode.SILENT -> audio.ringerMode = AudioManager.RINGER_MODE_SILENT
                RingerMode.VIBRATE -> audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                RingerMode.NORMAL -> audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
            refreshMode()
        } catch (e: Exception) {
            android.util.Log.e("SilentModeRepo", "Failed to apply mode $mode: ${e.message}")
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

    suspend fun onWifiChanged(currentSsid: String?) {
        val zones = getWifiZonesFlow().first()
        val activeWifiSsid = prefs.getString(PREF_KEY_ACTIVE_WIFI_SSID, null)

        if (currentSsid != null) {
            val zone = zones.find { it.ssid == currentSsid }
            if (zone != null) {
                if (activeWifiSsid != currentSsid) {
                    saveOriginalMode()
                    prefs.edit().putString(PREF_KEY_ACTIVE_WIFI_SSID, currentSsid).apply()
                    applyMode(zone.mode)
                }
            } else if (activeWifiSsid != null) {
                // Left a WiFi zone
                prefs.edit().remove(PREF_KEY_ACTIVE_WIFI_SSID).apply()
                checkAndRestore()
            }
        } else if (activeWifiSsid != null) {
            // Disconnected from WiFi
            prefs.edit().remove(PREF_KEY_ACTIVE_WIFI_SSID).apply()
            checkAndRestore()
        }
    }

    suspend fun onLocationTransition(id: String, isEntering: Boolean) {
        val zones = getLocationZones()
        val zone = zones.find { it.id == id } ?: return
        val activeLocationId = prefs.getString(PREF_KEY_ACTIVE_LOCATION_ID, null)

        if (isEntering) {
            saveOriginalMode()
            prefs.edit().putString(PREF_KEY_ACTIVE_LOCATION_ID, id).apply()
            applyMode(zone.mode)
        } else if (activeLocationId == id) {
            prefs.edit().remove(PREF_KEY_ACTIVE_LOCATION_ID).apply()
            checkAndRestore()
        }
    }

    private suspend fun checkAndRestore() {
        val activeWifi = prefs.getString(PREF_KEY_ACTIVE_WIFI_SSID, null)
        val activeLoc = prefs.getString(PREF_KEY_ACTIVE_LOCATION_ID, null)

        if (activeWifi == null && activeLoc == null) {
            restoreOriginalMode()
            // If still no mode applied (e.g. no original mode saved), default to NORMAL
            if (getCurrentMode() != RingerMode.NORMAL && prefs.getInt(PREF_KEY_SAVED_MODE, -1) == -1) {
                applyMode(RingerMode.NORMAL)
            }
        } else {
            // Still in another zone, re-apply that zone's mode
            if (activeWifi != null) {
                val zone = getWifiZonesFlow().first().find { it.ssid == activeWifi }
                zone?.let { applyMode(it.mode) }
            } else if (activeLoc != null) {
                val zone = getLocationZones().find { it.id == activeLoc }
                zone?.let { applyMode(it.mode) }
            }
        }
    }
    
    // Location Zone Management
    fun getLocationZonesFlow(): Flow<List<LocationZone>> {
        return dao.getAllLocationZones().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getLocationZones(): List<LocationZone> {
        return getLocationZonesFlow().first()
    }

    suspend fun addLocationZone(zone: LocationZone) {
        dao.insertLocationZone(zone.toEntity())
        geofenceManager.addGeofence(zone.id, zone.latitude, zone.longitude, zone.radius)
    }

    suspend fun removeLocationZone(id: String) {
        dao.deleteLocationZoneById(id)
        geofenceManager.removeGeofence(id)
        if (prefs.getString(PREF_KEY_ACTIVE_LOCATION_ID, null) == id) {
            prefs.edit().remove(PREF_KEY_ACTIVE_LOCATION_ID).apply()
            checkAndRestore()
        }
    }

    // WiFi Zone Management
    fun getWifiZonesFlow(): Flow<List<WifiZone>> {
        return dao.getAllWifiZones().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun addWifiZone(wifiZone: WifiZone) {
        dao.insertWifiZone(wifiZone.toEntity())
    }

    suspend fun removeWifiZone(ssid: String) {
        dao.deleteWifiZoneBySsid(ssid)
        if (prefs.getString(PREF_KEY_ACTIVE_WIFI_SSID, null) == ssid) {
            prefs.edit().remove(PREF_KEY_ACTIVE_WIFI_SSID).apply()
            checkAndRestore()
        }
    }

    // Important Contact Management
    fun getImportantContactsFlow(): Flow<List<ImportantContact>> {
        return dao.getAllImportantContacts().map { entities ->
            entities.map { it.toDomain() }
        }
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

    // Mappers
    private fun LocationZoneEntity.toDomain() = LocationZone(id, latitude, longitude, name, radius, mode)
    private fun LocationZone.toEntity() = LocationZoneEntity(id, latitude, longitude, name, radius, mode)
    private fun WifiZoneEntity.toDomain() = WifiZone(ssid, mode)
    private fun WifiZone.toEntity() = WifiZoneEntity(ssid, mode)
    private fun ImportantContactEntity.toDomain() = ImportantContact(id, name, phoneNumber)
    private fun ImportantContact.toEntity() = ImportantContactEntity(id, name, phoneNumber)
}
