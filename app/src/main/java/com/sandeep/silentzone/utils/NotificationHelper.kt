package com.sandeep.silentzone.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sandeep.silentzone.MainActivity
import com.sandeep.silentzone.R

object NotificationHelper {
    private const val INACTIVITY_CHANNEL_ID = "inactivity_channel"
    private const val INACTIVITY_NOTIFICATION_ID = 1001

    fun showInactivityNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                INACTIVITY_CHANNEL_ID,
                "App Inactivity Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to keep SilentZone active"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, INACTIVITY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use appropriate icon
            .setContentTitle("SilentZone is waiting")
            .setContentText("You haven't checked your zones in a while. Tap to stay protected!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(INACTIVITY_NOTIFICATION_ID, builder.build())
    }
}
