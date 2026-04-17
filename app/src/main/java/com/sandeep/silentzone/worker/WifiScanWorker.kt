package com.sandeep.silentzone.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sandeep.silentzone.SilentModeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

@HiltWorker
class WifiScanWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val repository: SilentModeRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("WifiScanWorker", "Starting background SSID detection...")

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        var attempts = 0
        var detectedSsid: String? = null

        // WorkManager allows us to keep the process alive for long enough to catch the SSID
        while (attempts < 4) {
            val delayTime = if (attempts == 0) 1000L else 3000L
            delay(delayTime)

            var ssid: String? = null

            // Method 1: Modern API
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                val wifiInfo = capabilities?.transportInfo as? WifiInfo
                if (wifiInfo != null && wifiInfo.ssid != null && wifiInfo.ssid != WifiManager.UNKNOWN_SSID) {
                    ssid = wifiInfo.ssid.trim('"')
                }
            }

            // Method 2: Legacy Fallback
            if (ssid == null || ssid == WifiManager.UNKNOWN_SSID) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                val info = wifiManager.connectionInfo
                if (info != null && info.networkId != -1 && info.ssid != null && info.ssid != WifiManager.UNKNOWN_SSID) {
                    ssid = info.ssid.trim('"')
                }
            }

            if (ssid != null && ssid != WifiManager.UNKNOWN_SSID) {
                detectedSsid = ssid
                Log.d("WifiScanWorker", "SSID found in background: $ssid")
                break
            }

            attempts++
            Log.d("WifiScanWorker", "SSID still unknown, retry $attempts/4...")
        }

        if (detectedSsid != null) {
            repository.onWifiChanged(detectedSsid)
        } else {
            // Check if we are disconnected
            if (connectivityManager.activeNetwork == null) {
                Log.d("WifiScanWorker", "Network lost. Normalizing...")
                repository.onWifiChanged(null)
            }
        }

        return Result.success()
    }
}
