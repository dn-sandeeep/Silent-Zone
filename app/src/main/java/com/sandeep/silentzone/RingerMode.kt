package com.sandeep.silentzone

enum class RingerMode { SILENT, NORMAL, VIBRATE }


data class UiState(
val accessGranted: Boolean = false,
val currentMode: RingerMode = RingerMode.NORMAL,
val message: String? = null
)