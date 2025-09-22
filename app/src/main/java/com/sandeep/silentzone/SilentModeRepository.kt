package com.sandeep.silentzone

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices

class SilentModeRepository(
    private val appContext: Context
) {
    private val audio: AudioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notif: NotificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(appContext)

    fun hasPolicyAccess(): Boolean = notif.isNotificationPolicyAccessGranted

    fun getCurrentMode(): RingerMode = when (audio.ringerMode) {
        AudioManager.RINGER_MODE_SILENT -> RingerMode.SILENT
        AudioManager.RINGER_MODE_NORMAL -> RingerMode.NORMAL
        else -> RingerMode.NORMAL
    }

    fun setSilent(): RingerMode {
        require(hasPolicyAccess()) {
            "Notification policy access not granted"
        }
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        return getCurrentMode()
    }

    fun setNormal(): RingerMode {
        require(hasPolicyAccess()) {
            "Notification policy access not granted"
        }
        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        return getCurrentMode()
    }

//    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
//    fun addGeofence(
//        lat: Double,
//        long: Double,
//        radius: Float,
//        onResult: (Boolean, String) -> Unit
//    ) {
//        val geofence = Geofence.Builder()
//            .setRequestId("silent_zone")
//            .setCircularRegion(lat, long, radius)
//            .setExpirationDuration(Geofence.NEVER_EXPIRE)
//            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
//            .build()
//
//        val request = GeofencingRequest.Builder()
//            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
//            .addGeofence(geofence)
//            .build()
//        val intent = Intent(appContext, GeofenceBroadcastReceiver::class.java)
//        val pendingIntent = PendingIntent.getBroadcast(
//            appContext,
//            0,
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//        geofencingClient.addGeofences(request, pendingIntent)
//            .addOnSuccessListener {
//                Log.d("Geofence", "Geofence added successfully")
//            }
//            .addOnFailureListener { e ->
//                Log.e("Geofence", "Failed to add geofence", e)
//
//            }
//    }
//
//}
}