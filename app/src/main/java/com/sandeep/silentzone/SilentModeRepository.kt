package com.sandeep.silentzone

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import com.sandeep.silentzone.data.ImportantContactEntity
import com.sandeep.silentzone.data.LocationZoneEntity
import com.sandeep.silentzone.data.SilentZoneDao
import dagger.hilt.android.qualifiers.ApplicationContext
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
    
    private val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(appContext)

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

    fun setSilent(): RingerMode {
        if (hasPolicyAccess()) {
            try {
                audio.ringerMode = AudioManager.RINGER_MODE_SILENT
                android.util.Log.d("SilentModeRepo", "Set Ringer Mode to SILENT")
            } catch (e: Exception) {
                android.util.Log.e("SilentModeRepo", "Failed to set SILENT: ${e.message}")
            }
        }
        return getCurrentMode()
    }

    fun setVibrate(): RingerMode {
         if (hasPolicyAccess()) {
            try {
                audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                android.util.Log.d("SilentModeRepo", "Set Ringer Mode to VIBRATE")
            } catch (e: Exception) {
                android.util.Log.e("SilentModeRepo", "Failed to set VIBRATE: ${e.message}")
            }
        }
        return getCurrentMode()
    }

    fun setNormal(): RingerMode {
        try {
            if (hasPolicyAccess() || audio.ringerMode != AudioManager.RINGER_MODE_SILENT) {
                 audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
                 android.util.Log.d("SilentModeRepo", "Set Ringer Mode to NORMAL")
            }
        } catch (e: Exception) {
             android.util.Log.e("SilentModeRepo", "Failed to set NORMAL: ${e.message}")
        }
        return getCurrentMode()
    }
    
    // Location Zone Management
    fun getLocationZonesFlow(): Flow<List<LocationZone>> {
        return dao.getAllLocationZones().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // Keep synchronous for GeofenceReceiver for now, but use Room
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
    private fun ImportantContactEntity.toDomain() = ImportantContact(id, name, phoneNumber)
    private fun ImportantContact.toEntity() = ImportantContactEntity(id, name, phoneNumber)
}
