package com.sandeep.silentzone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.content.IntentCompat

/**
 * Handles network changes triggered by the system even when the app is in the background.
 * Registered via ConnectivityManager.registerNetworkCallback(request, pendingIntent).
 */
@AndroidEntryPoint
class NetworkChangeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: SilentModeRepository

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onReceive(context: Context, intent: Intent) {
        val network = IntentCompat.getParcelableExtra(intent, ConnectivityManager.EXTRA_NETWORK, Network::class.java)
        Log.d("NetworkChangeReceiver", "System network signal received (Network: $network)")

        if (network == null) {
            Log.w("NetworkChangeReceiver", "Received null network extra from system.")
            return
        }

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        CoroutineScope(Dispatchers.IO).launch {
            var attempts = 0
            var detectedSsid: String? = null
            
            // Background Retry Loop (Similar to Service)
            while (attempts < 3) {
                val delayTime = if (attempts == 0) 1000L else 3000L
                kotlinx.coroutines.delay(delayTime)
                
                var ssid: String? = null
                
                // Method 1: Modern API
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val wifiInfo = capabilities?.transportInfo as? WifiInfo
                if (wifiInfo != null) {
                    @Suppress("DEPRECATION")
                    val currentSsid = wifiInfo.ssid

                    if (currentSsid != null && currentSsid != android.net.wifi.WifiManager.UNKNOWN_SSID) {
                        ssid = currentSsid.trim('"')
                    }
                }
                
                // Method 2: Legacy Fallback (Often more reliable in background)
                if (ssid == null || ssid == android.net.wifi.WifiManager.UNKNOWN_SSID) {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                    @Suppress("DEPRECATION")
                    val info = wifiManager.connectionInfo
                    if (info != null && info.networkId != -1) {
                        @Suppress("DEPRECATION")
                        val legacySsid = info.ssid

                        if (legacySsid != null && legacySsid != android.net.wifi.WifiManager.UNKNOWN_SSID) {
                            ssid = legacySsid.trim('"')
                        }
                    }
                }
                
                if (ssid != null && ssid != android.net.wifi.WifiManager.UNKNOWN_SSID) {
                    detectedSsid = ssid
                    Log.d("NetworkChangeReceiver", "Background SSID detected on attempt ${attempts + 1}: $ssid")
                    break
                }
                
                attempts++
                Log.d("NetworkChangeReceiver", "SSID unknown in background, retrying... (${attempts}/3)")
            }

            if (detectedSsid != null) {
                repository.onWifiChanged(detectedSsid)
            } else {
                // If we still have capabilities but no SSID, it might be a valid network without SSID perm
                // Or if capabilities null, it's a disconnect.
                val finalCaps = connectivityManager.getNetworkCapabilities(network)
                if (finalCaps == null) {
                    Log.d("NetworkChangeReceiver", "Network lost in background. Syncing state...")
                    repository.onWifiChanged(null)
                }
            }
        }
    }
}
