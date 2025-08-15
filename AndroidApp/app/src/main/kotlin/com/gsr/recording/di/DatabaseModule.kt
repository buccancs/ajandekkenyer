package com.gsr.recording.di

import android.content.Context
import androidx.room.Room
import com.gsr.recording.data.database.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideGSRDatabase(@ApplicationContext context: Context): GSRDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            GSRDatabase::class.java,
            "gsr_database"
        )
        .fallbackToDestructiveMigration() // For development - remove in production
        .build()
    }
    
    @Provides
    fun provideGSRReadingDao(database: GSRDatabase): GSRReadingDao {
        return database.gsrReadingDao()
    }
    
    @Provides
    fun provideThermalFrameDao(database: GSRDatabase): ThermalFrameDao {
        return database.thermalFrameDao()
    }
    
    @Provides
    fun provideRecordingSessionDao(database: GSRDatabase): RecordingSessionDao {
        return database.recordingSessionDao()
    }
    
    @Provides
    fun provideDeviceDao(database: GSRDatabase): DeviceDao {
        return database.deviceDao()
    }
}