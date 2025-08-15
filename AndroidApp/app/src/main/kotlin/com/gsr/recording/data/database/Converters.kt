package com.gsr.recording.data.database

import androidx.room.TypeConverter
import com.gsr.recording.data.model.*
import java.util.Date

/**
 * Type converters for Room database.
 * Handles conversion between Kotlin types and database-compatible types.
 */
class Converters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromSessionStatus(status: SessionStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toSessionStatus(status: String): SessionStatus {
        return SessionStatus.valueOf(status)
    }
    
    @TypeConverter
    fun fromDeviceType(type: DeviceType): String {
        return type.name
    }
    
    @TypeConverter
    fun toDeviceType(type: String): DeviceType {
        return DeviceType.valueOf(type)
    }
    
    @TypeConverter
    fun fromConnectionStatus(status: ConnectionStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toConnectionStatus(status: String): ConnectionStatus {
        return ConnectionStatus.valueOf(status)
    }
    
    @TypeConverter
    fun fromReadingQuality(quality: ReadingQuality): String {
        return quality.name
    }
    
    @TypeConverter
    fun toReadingQuality(quality: String): ReadingQuality {
        return ReadingQuality.valueOf(quality)
    }
}