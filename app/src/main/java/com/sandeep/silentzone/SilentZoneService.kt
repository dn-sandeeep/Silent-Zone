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
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SilentZoneService : Service() {

    @Inject lateinit var repository: SilentModeRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var connectivityManager: ConnectivityManager

    private val networkCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    checkWifiAndApplyMode(network)
                }

                override fun onCapabilitiesChanged(
                        network: Network,
                        capabilities: NetworkCapabilities
                ) {
                    checkWifiAndApplyMode(network)
                }

                override fun onLost(network: Network) {
                    checkWifiAndApplyMode(null)
                }
            }

    private val ringerModeReceiver =
            object : BroadcastReceiver() {
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
        val networkRequest =
                NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun checkWifiAndApplyMode(network: Network?) {
        serviceScope.launch {
            if (network == null) {
                // Immediate disconnect processing
                repository.onWifiChanged(null)
                return@launch
            }

            var attempts = 0
            var finalSsid: String? = null

            // Background retry loop: Android 14 can take a few seconds to "permit"
            // the background service to read the real SSID.
            while (attempts < 3) {
                // 1s first wait, then 3s between retries
                val delayTime = if (attempts == 0) 1000L else 3000L
                kotlinx.coroutines.delay(delayTime)

                finalSsid = getCurrentSsid(network)

                if (finalSsid != WifiManager.UNKNOWN_SSID) {
                    android.util.Log.d(
                            "SilentZoneService",
                            "Successfully identified SSID: $finalSsid"
                    )
                    break
                }

                attempts++
                android.util.Log.d(
                        "SilentZoneService",
                        "SSID is Unknown (background limit), retrying... ($attempts/3)"
                )
            }

            repository.onWifiChanged(finalSsid)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentSsid(network: Network? = null): String? {
        return try {
            val wifiManager =
                    applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            var isConnectedButUnknown = false

            // Priority 1: Legacy API (often more reliable for SSID even on modern Android)
            @Suppress("DEPRECATION") val info = wifiManager.connectionInfo
            if (info != null && info.networkId != -1) {
                @Suppress("DEPRECATION")
                val ssid = info.ssid

                if (ssid != null && ssid != WifiManager.UNKNOWN_SSID) {
                    return ssid.trim('"')
                } else {
                    isConnectedButUnknown = true
                }
            }

            // Priority 2: Modern API (NetworkCapabilities)
            val targetNetwork = network ?: connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(targetNetwork)
            val wifiInfo = capabilities?.transportInfo as? android.net.wifi.WifiInfo
            if (wifiInfo != null) {
                @Suppress("DEPRECATION")
                val ssid = wifiInfo.ssid

                if (ssid != null && ssid != WifiManager.UNKNOWN_SSID) {
                    return ssid.trim('"')
                } else {
                    isConnectedButUnknown = true
                }
            }

            if (isConnectedButUnknown) WifiManager.UNKNOWN_SSID else null
        } catch (e: Exception) {
            null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESTORE_MODE) {
            android.util.Log.d("SilentZoneService", "Restore action received")
            serviceScope.launch {
                repository.restoreOriginalMode()
                stopSelf()
            }
            return START_NOT_STICKY
        }

        val zoneName = intent?.getStringExtra(EXTRA_ZONE_NAME) ?: "Monitoring"
        val notification = createNotification(zoneName)
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        val restartServicePendingIntent =
                PendingIntent.getService(
                        applicationContext,
                        1,
                        restartServiceIntent,
                        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
        val alarmService =
                applicationContext.getSystemService(Context.ALARM_SERVICE) as
                        android.app.AlarmManager
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
            val channel =
                    NotificationChannel(
                                    CHANNEL_ID,
                                    "SilentZone Service",
                                    NotificationManager.IMPORTANCE_LOW
                            )
                            .apply {
                                description = "Keeps SilentZone active in the background"
                                setShowBadge(false)
                            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(zoneName: String): Notification {
        val intent =
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
        val pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        val restoreIntent =
                Intent(this, SilentZoneService::class.java).apply { action = ACTION_RESTORE_MODE }
        val restorePendingIntent =
                PendingIntent.getService(
                        this,
                        0,
                        restoreIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        val isSearching = zoneName.startsWith("WiFi: ")
        val isMonitoring = zoneName == "Monitoring"
        
        val displayTitle = when {
            isSearching -> "Searching for WiFi: ${zoneName.removePrefix("WiFi: ")}"
            isMonitoring -> "SilentZone: Monitoring"
            else -> "SilentZone: $zoneName"
        }
        
        val contentText = when {
            isSearching -> "Waiting to connect and protect your silence"
            isMonitoring -> "Active and guarding your silence"
            else -> "Protecting your silence in this area"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(displayTitle)
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .addAction(android.R.drawable.ic_menu_revert, "Restore Now", restorePendingIntent)
                .build()
    }

    companion object {
        const val CHANNEL_ID = "silent_zone_service_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_ZONE_NAME = "extra_zone_name"
        const val ACTION_RESTORE_MODE = "com.sandeep.silentzone.ACTION_RESTORE_MODE"
    }
}
