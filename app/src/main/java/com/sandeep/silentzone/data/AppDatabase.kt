package com.sandeep.silentzone.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sandeep.silentzone.RingerMode

@Database(entities = [LocationZoneEntity::class, ImportantContactEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun silentZoneDao(): SilentZoneDao
}

class Converters {
    @androidx.room.TypeConverter
    fun fromRingerMode(mode: RingerMode): String = mode.name

    @androidx.room.TypeConverter
    fun toRingerMode(name: String): RingerMode = RingerMode.valueOf(name)
}
