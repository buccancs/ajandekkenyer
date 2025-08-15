package com.gsr.recording.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.gsr.recording.data.model.*

/**
 * Room database for the GSR Recording System.
 * Central database containing all sensor data, sessions, and device information.
 */
@Database(
    entities = [
        GSRReading::class,
        ThermalFrame::class,
        RecordingSession::class,
        Device::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GSRDatabase : RoomDatabase() {
    
    abstract fun gsrReadingDao(): GSRReadingDao
    abstract fun thermalFrameDao(): ThermalFrameDao
    abstract fun recordingSessionDao(): RecordingSessionDao
    abstract fun deviceDao(): DeviceDao
    
    companion object {
        @Volatile
        private var INSTANCE: GSRDatabase? = null
        
        fun getDatabase(context: Context): GSRDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GSRDatabase::class.java,
                    "gsr_database"
                )
                .fallbackToDestructiveMigration() // For development - remove in production
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}