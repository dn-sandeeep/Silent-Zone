package com.sandeep.silentzone

enum class RingerMode { SILENT, NORMAL }


data class UiState(
val accessGranted: Boolean = false,
val currentMode: RingerMode = RingerMode.NORMAL,
val message: String? = null
)