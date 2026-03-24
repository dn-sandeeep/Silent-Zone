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
}
