package com.sandeep.silentzone.di

import android.content.Context
import androidx.room.Room
import com.sandeep.silentzone.data.AppDatabase
import com.sandeep.silentzone.data.SilentZoneDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "silent_zone_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideSilentZoneDao(database: AppDatabase): SilentZoneDao {
        return database.silentZoneDao()
    }
}
