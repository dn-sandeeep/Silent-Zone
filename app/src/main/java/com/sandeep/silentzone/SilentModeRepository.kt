package com.sandeep.silentzone

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import com.sandeep.silentzone.SilentZoneGeofenceManager // Assuming in same package
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class LocationZone(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val radius: Float,
    val mode: RingerMode = RingerMode.SILENT // Default for migration
)

class SilentModeRepository(
    private val appContext: Context
) {
    private val audio: AudioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notif: NotificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    private val geofenceManager = SilentZoneGeofenceManager(appContext)
    private val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(appContext)
    private val prefs: SharedPreferences = appContext.getSharedPreferences("location_zones", Context.MODE_PRIVATE)
    private val gson = Gson()

    @android.annotation.SuppressLint("MissingPermission")
    fun getCurrentLocation(onLocationResult: (Double, Double) -> Unit, onError: () -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
             if (location != null) {
                 onLocationResult(location.latitude, location.longitude)
             } else {
                 // Request fresh location or just error out for simplicity in this step
                 // Ideally we should request location updates, but lastLocation is often sufficient if recent
                 onError()
             }
        }.addOnFailureListener {
            onError()
        }
    }

    fun hasPolicyAccess(): Boolean {
        // Check both policy access and audio Manager mode
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
        } else {
            android.util.Log.e("SilentModeRepo", "Cannot set SILENT: No Policy Access")
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
        } else {
             android.util.Log.e("SilentModeRepo", "Cannot set VIBRATE: No Policy Access")
        }
        return getCurrentMode()
    }

    fun setNormal(): RingerMode {
        // Normal mode usually doesn't need policy access unless we are currently in DND
        // But to be safe we check, or we catch the security exception
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
    fun getLocationZones(): List<LocationZone> {
        val json = prefs.getString("zones", null) ?: return emptyList()
        val type = object : TypeToken<List<LocationZone>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun addLocationZone(zone: LocationZone) {
        val current = getLocationZones().toMutableList()
        current.add(zone)
        saveLocationZones(current)
        
        // Add to Geofence Manager
        geofenceManager.addGeofence(zone.id, zone.latitude, zone.longitude, zone.radius)
    }

    fun removeLocationZone(id: String) {
        val current = getLocationZones().toMutableList()
        current.removeAll { it.id == id }
        saveLocationZones(current)
        
        // Remove from Geofence Manager
        geofenceManager.removeGeofence(id)
    }

    private fun saveLocationZones(zones: List<LocationZone>) {
        val json = gson.toJson(zones)
        prefs.edit().putString("zones", json).apply()
    }
}