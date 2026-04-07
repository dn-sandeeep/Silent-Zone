package com.sandeep.silentzone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: SilentModeRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: run {
            android.util.Log.e("GeofenceReceiver", "Received intent with no geofencing event")
            return
        }

        if (geofencingEvent.hasError()) {
            android.util.Log.e("GeofenceReceiver", "GeofencingEvent error: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        android.util.Log.d("GeofenceReceiver", "Transition detected: $geofenceTransition")

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: run {
                android.util.Log.e("GeofenceReceiver", "No triggering geofences found")
                return
            }
            
            val pendingResult = goAsync()
            scope.launch {
                try {
                    for (geofence in triggeringGeofences) {
                        repository.onLocationTransition(
                            geofence.requestId, 
                            geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GeofenceReceiver", "Error processing geofences: ${e.message}")
                } finally {
                    android.util.Log.d("GeofenceReceiver", "Geofence processing complete")
                    pendingResult.finish()
                }
            }
        }
    }
}
