package com.sandeep.silentzone.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SilentZoneDao {
    // Location Zones
    @Query("SELECT * FROM location_zones")
    fun getAllLocationZones(): Flow<List<LocationZoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationZone(zone: LocationZoneEntity)

    @Query("DELETE FROM location_zones WHERE id = :id")
    suspend fun deleteLocationZoneById(id: String)

    @Query("SELECT * FROM location_zones WHERE id = :id")
    suspend fun getLocationZoneById(id: String): LocationZoneEntity?

    // Important Contacts
    @Query("SELECT * FROM important_contacts")
    fun getAllImportantContacts(): Flow<List<ImportantContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportantContact(contact: ImportantContactEntity)

    @Query("DELETE FROM important_contacts WHERE phoneNumber = :phoneNumber")
    suspend fun deleteImportantContactByNumber(phoneNumber: String)

    // WiFi Zones
    @Query("SELECT * FROM wifi_zones")
    fun getAllWifiZones(): Flow<List<WifiZoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWifiZone(zone: WifiZoneEntity)

    @Query("DELETE FROM wifi_zones WHERE ssid = :ssid")
    suspend fun deleteWifiZoneBySsid(ssid: String)

    @Query("SELECT COUNT(*) FROM location_zones")
    suspend fun getLocationZonesCount(): Int

    @Query("SELECT COUNT(*) FROM wifi_zones")
    suspend fun getWifiZonesCount(): Int

    // Analytics
    @Insert
    suspend fun insertAnalyticsEvent(event: AnalyticsEventEntity): Long

    @Update
    suspend fun updateAnalyticsEvent(event: AnalyticsEventEntity)

    @Query("SELECT * FROM analytics_events WHERE zoneName = :zoneName AND exitTime IS NULL ORDER BY entryTime DESC LIMIT 1")
    suspend fun getActiveEventByZone(zoneName: String): AnalyticsEventEntity?

    @Query("SELECT * FROM analytics_events ORDER BY entryTime DESC LIMIT 50")
    fun getRecentEvents(): Flow<List<AnalyticsEventEntity>>

    @Query("SELECT * FROM analytics_events WHERE entryTime >= :startTime")
    fun getEventsSince(startTime: Long): Flow<List<AnalyticsEventEntity>>

    @Query("SELECT * FROM analytics_events WHERE entryTime >= :startTime")
    suspend fun getEventsSinceList(startTime: Long): List<AnalyticsEventEntity>
}
