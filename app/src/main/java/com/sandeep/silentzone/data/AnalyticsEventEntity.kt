package com.sandeep.silentzone.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sandeep.silentzone.RingerMode

@Entity(tableName = "analytics_events")
data class AnalyticsEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val zoneName: String,
    val zoneType: String, // "WIFI" or "LOCATION"
    val entryTime: Long,
    val exitTime: Long? = null,
    val durationMillis: Long = 0,
    val mode: RingerMode
)
