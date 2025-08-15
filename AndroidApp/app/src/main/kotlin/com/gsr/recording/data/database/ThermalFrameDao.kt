package com.gsr.recording.data.database

import androidx.room.*
import com.gsr.recording.data.model.ThermalFrame
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for thermal camera frames.
 * Provides database operations for thermal imaging data.
 */
@Dao
interface ThermalFrameDao {
    
    @Query("SELECT * FROM thermal_frames ORDER BY timestamp DESC")
    fun getAllFrames(): Flow<List<ThermalFrame>>
    
    @Query("SELECT * FROM thermal_frames WHERE sessionId = :sessionId ORDER BY frameNumber ASC")
    fun getFramesForSession(sessionId: String): Flow<List<ThermalFrame>>
    
    @Query("SELECT * FROM thermal_frames WHERE sessionId = :sessionId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    suspend fun getFramesInTimeRange(sessionId: String, startTime: Long, endTime: Long): List<ThermalFrame>
    
    @Query("SELECT * FROM thermal_frames WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentFramesForDevice(deviceId: String, limit: Int = 50): List<ThermalFrame>
    
    @Query("SELECT AVG(averageTemperature) FROM thermal_frames WHERE sessionId = :sessionId")
    suspend fun getAverageTemperatureForSession(sessionId: String): Float?
    
    @Query("SELECT MAX(maxTemperature) FROM thermal_frames WHERE sessionId = :sessionId")
    suspend fun getMaxTemperatureForSession(sessionId: String): Float?
    
    @Query("SELECT MIN(minTemperature) FROM thermal_frames WHERE sessionId = :sessionId")
    suspend fun getMinTemperatureForSession(sessionId: String): Float?
    
    @Query("SELECT COUNT(*) FROM thermal_frames WHERE sessionId = :sessionId")
    suspend fun getFrameCountForSession(sessionId: String): Int
    
    @Query("DELETE FROM thermal_frames WHERE sessionId = :sessionId")
    suspend fun deleteFramesForSession(sessionId: String)
    
    @Query("DELETE FROM thermal_frames WHERE timestamp < :cutoffTime")
    suspend fun deleteOldFrames(cutoffTime: Long)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrame(frame: ThermalFrame)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrames(frames: List<ThermalFrame>)
    
    @Update
    suspend fun updateFrame(frame: ThermalFrame)
    
    @Delete
    suspend fun deleteFrame(frame: ThermalFrame)
    
    @Query("SELECT * FROM thermal_frames WHERE id = :id")
    suspend fun getFrameById(id: String): ThermalFrame?
    
    /**
     * Get the latest frame for a specific device.
     * Useful for monitoring current device status.
     */
    @Query("SELECT * FROM thermal_frames WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestFrameForDevice(deviceId: String): ThermalFrame?
    
    /**
     * Get frames with temperature above threshold for analysis.
     */
    @Query("SELECT * FROM thermal_frames WHERE maxTemperature > :threshold ORDER BY timestamp DESC")
    suspend fun getFramesAboveTemperature(threshold: Float): List<ThermalFrame>
    
    /**
     * Get thermal session statistics.
     */
    @Query("""
        SELECT 
            COUNT(*) as frameCount,
            AVG(averageTemperature) as avgTemperature,
            MAX(maxTemperature) as maxTemperature,
            MIN(minTemperature) as minTemperature,
            MIN(timestamp) as startTime,
            MAX(timestamp) as endTime
        FROM thermal_frames 
        WHERE sessionId = :sessionId
    """)
    suspend fun getThermalSessionStats(sessionId: String): ThermalSessionStats?
}

/**
 * Data class for thermal session statistics query result.
 */
data class ThermalSessionStats(
    val frameCount: Int,
    val avgTemperature: Float,
    val maxTemperature: Float,
    val minTemperature: Float,
    val startTime: Long,
    val endTime: Long
)