package com.sandeep.silentzone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WifiBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: SilentModeRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
            val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
            val ssid = if (networkInfo?.isConnected == true) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val info = wifiManager.connectionInfo
                if (info.ssid != null && info.ssid != WifiManager.UNKNOWN_SSID) {
                    info.ssid.trim('"')
                } else {
                    null
                }
            } else {
                null
            }

            val pendingResult = goAsync()
            scope.launch {
                try {
                    repository.onWifiChanged(ssid)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
