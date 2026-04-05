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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
            val pendingResult = goAsync()
            scope.launch {
                try {
                    for (geofence in triggeringGeofences) {
                        repository.onLocationTransition(
                            geofence.requestId, 
                            geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
                        )
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
