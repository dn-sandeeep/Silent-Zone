package com.sandeep.silentzone.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticsHelper(private val context: Context) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    fun logEstimatedBatteryImpact(totalImpact: Double, wifiImpact: Double, locationImpact: Double) {
        val bundle = Bundle().apply {
            putDouble("total_impact_percent", totalImpact)
            putDouble("wifi_impact_percent", wifiImpact)
            putDouble("location_impact_percent", locationImpact)
        }
        firebaseAnalytics.logEvent("estimated_battery_impact", bundle)
    }

    fun logTotalZones(wifiCount: Int, locationCount: Int) {
        val total = wifiCount + locationCount
        val bundle = Bundle().apply {
            putInt("wifi_zones", wifiCount)
            putInt("location_zones", locationCount)
            putInt("total_zones", total)
        }
        firebaseAnalytics.logEvent("zones_summary", bundle)
        firebaseAnalytics.setUserProperty("total_zones_count", total.toString())
    }

    fun logNavigateToZones() {
        firebaseAnalytics.logEvent("navigate_to_zones_screen", null)
    }

    fun logClickCreateZone() {
        firebaseAnalytics.logEvent("click_create_zone", null)
    }

    fun logModeTransition(fromMode: String, toMode: String) {
        val bundle = Bundle().apply {
            putString("from_mode", fromMode)
            putString("to_mode", toMode)
            putLong("timestamp_millis", System.currentTimeMillis())
        }
        firebaseAnalytics.logEvent("mode_transition_by_app", bundle)
    }

    fun logDndPermissionStatus(granted: Boolean) {
        val status = if (granted) "granted" else "denied"
        val bundle = Bundle().apply {
            putString("status", status)
        }
        firebaseAnalytics.logEvent("dnd_permission_status_changed", bundle)
        firebaseAnalytics.setUserProperty("dnd_permission", status)
    }
}
