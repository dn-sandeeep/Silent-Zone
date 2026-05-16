package com.sandeep.silentzone

enum class RingerMode {
    SILENT,
    NORMAL,
    VIBRATE
}

data class UiState(
        val accessGranted: Boolean = false,
        val currentMode: RingerMode = RingerMode.NORMAL,
        val isFallback: Boolean = false,
        val message: String? = null,
        val hasForegroundLocation: Boolean = true,
        val hasBackgroundLocation: Boolean = true,
        val hasWifiAutomationPermission: Boolean = true,
        val isIgnoringBatteryOptimizations: Boolean = true,
        val batteryUsage: BatteryUsage = BatteryUsage(0.0, 0.0, 0.0, 0.0)
)
