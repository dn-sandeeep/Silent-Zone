package com.sandeep.silentzone

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SilentZoneService : Service() {

    @Inject
    lateinit var repository: SilentModeRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var connectivityManager: ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            checkWifiAndApplyMode()
        }

        override fun onLost(network: Network) {
            checkWifiAndApplyMode()
        }
    }

    private val ringerModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
                repository.refreshMode()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        registerNetworkCallback()
        registerReceiver(ringerModeReceiver, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun checkWifiAndApplyMode() {
        serviceScope.launch {
            val ssid = getCurrentSsid()
            repository.onWifiChanged(ssid)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentSsid(): String? {
        return try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val wifiInfo = capabilities?.transportInfo as? android.net.wifi.WifiInfo
                wifiInfo?.ssid?.trim('"')?.takeIf { it != WifiManager.UNKNOWN_SSID }
            } else {
                @Suppress("DEPRECATION")
                val info = wifiManager.connectionInfo
                if (info.ssid != null && info.ssid != WifiManager.UNKNOWN_SSID) {
                    info.ssid.trim('"')
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext, 1, restartServiceIntent, 
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmService.set(
            android.app.AlarmManager.RTC,
            System.currentTimeMillis() + 1000,
            restartServicePendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {}
        try {
            unregisterReceiver(ringerModeReceiver)
        } catch (e: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SilentZone Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps SilentZone active in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SilentZone is Active")
            .setContentText("Monitoring your zones in the background")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "silent_zone_service_channel"
        const val NOTIFICATION_ID = 1
    }
}
