package com.sandeep.silentzone.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sandeep.silentzone.RingerMode

@Entity(tableName = "location_zones")
data class LocationZoneEntity(
    @PrimaryKey val id: String,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val radius: Float,
    val mode: RingerMode
)
