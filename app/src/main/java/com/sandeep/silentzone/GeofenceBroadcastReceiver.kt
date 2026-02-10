package com.sandeep.silentzone

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        
        // Create notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "geofence_channel"
            val channelName = "Geofence Notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            val channel = NotificationChannel(channelId, channelName, importance)
            channel.setShowBadge(false)
            channel.setSound(null, null)
            channel.enableVibration(true)
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            
            // Create persistent notification
            val persistentNotification = createPersistentNotification(context, "SilentZone is monitoring location zones")
            notificationManager.notify(NOTIFICATION_ID, persistentNotification)
        }
        
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            return
        }

        if (geofencingEvent.hasError()) {
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            if (triggeringGeofences != null) {
                for (geofence in triggeringGeofences) {
                    val requestId = geofence.requestId

                    // Handle transition
                    handleTransition(context, geofenceTransition, requestId)
                }
            }
        }
    }
    
    private fun createPersistentNotification(context: Context, content: String): Notification {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("from_notification", true)
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, "geofence_channel")
            .setContentTitle("SilentZone Active")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
    }

    private fun handleTransition(context: Context, transition: Int, requestId: String) {
        val repository = SilentModeRepository(context)
        val zones = repository.getLocationZones()
        val zone = zones.find { it.id == requestId }
        
        if (zone == null) {
             return
        }

        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Toast.makeText(context, "Entering ${zone.name} -> ${zone.mode}", Toast.LENGTH_SHORT).show()
             when (zone.mode) {
                RingerMode.SILENT -> repository.setSilent()
                RingerMode.VIBRATE -> repository.setVibrate()
                else -> repository.setSilent()
            }
        } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Toast.makeText(context, "Exiting ${zone.name} -> Normal", Toast.LENGTH_SHORT).show()
            repository.setNormal()
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
        const val NOTIFICATION_ID = 1234
    }
}
