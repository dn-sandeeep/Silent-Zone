package com.sandeep.silentzone

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class SilentZoneGeofenceManager(private val context: Context) {

    private val geofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that if we add new geofences, they use the same pending intent
        // FLAG_MUTABLE is required for Android 12+ (S)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun addGeofence(
        requestId: String,
        latitude: Double,
        longitude: Double,
        radius: Float = 100f // Default 100 meters
    ) {
        val geofence = Geofence.Builder()
            .setRequestId(requestId)
            .setCircularRegion(latitude, longitude, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
            
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                android.util.Log.d("LocationDebug", "Geofence added successfully: $requestId")
            }
            addOnFailureListener {
                android.util.Log.e("LocationDebug", "Geofence failed to add: $requestId, Error: ${it.message}")
            }
        }
    }

    fun removeGeofence(requestId: String) {
        geofencingClient.removeGeofences(listOf(requestId)).run {
             addOnSuccessListener {
             }
             addOnFailureListener {
                 android.util.Log.e("GeofenceManager", "Failed to remove geofence: $requestId")
             }
        }
    }
}
