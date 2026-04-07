package com.sandeep.silentzone.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sandeep.silentzone.RingerMode

@Entity(tableName = "wifi_zones")
data class WifiZoneEntity(
    @PrimaryKey val ssid: String,
    val mode: RingerMode,
    val latitude: Double? = null,
    val longitude: Double? = null
)
