package com.gsr.recording.data.database

import androidx.room.*
import com.gsr.recording.data.model.GSRReading
import com.gsr.recording.data.model.ReadingQuality
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for GSR readings.
 * Provides database operations for GSR sensor data.
 */
@Dao
interface GSRReadingDao {
    
    @Query("SELECT * FROM gsr_readings ORDER BY timestamp DESC")
    fun getAllReadings(): Flow<List<GSRReading>>
    
    @Query("SELECT * FROM gsr_readings WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getReadingsForSession(sessionId: String): Flow<List<GSRReading>>
    
    @Query("SELECT * FROM gsr_readings WHERE sessionId = :sessionId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    suspend fun getReadingsInTimeRange(sessionId: String, startTime: Long, endTime: Long): List<GSRReading>
    
    @Query("SELECT * FROM gsr_readings WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentReadingsForDevice(deviceId: String, limit: Int = 100): List<GSRReading>
    
    @Query("SELECT AVG(gsrValue) FROM gsr_readings WHERE sessionId = :sessionId")
    suspend fun getAverageGSRForSession(sessionId: String): Double?
    
    @Query("SELECT MAX(gsrValue) FROM gsr_readings WHERE sessionId = :sessionId")
    suspend fun getMaxGSRForSession(sessionId: String): Double?
    
    @Query("SELECT MIN(gsrValue) FROM gsr_readings WHERE sessionId = :sessionId")
    suspend fun getMinGSRForSession(sessionId: String): Double?
    
    @Query("SELECT COUNT(*) FROM gsr_readings WHERE sessionId = :sessionId")
    suspend fun getReadingCountForSession(sessionId: String): Int
    
    @Query("SELECT * FROM gsr_readings WHERE quality = :quality ORDER BY timestamp DESC")
    suspend fun getReadingsByQuality(quality: ReadingQuality): List<GSRReading>
    
    @Query("DELETE FROM gsr_readings WHERE sessionId = :sessionId")
    suspend fun deleteReadingsForSession(sessionId: String)
    
    @Query("DELETE FROM gsr_readings WHERE timestamp < :cutoffTime")
    suspend fun deleteOldReadings(cutoffTime: Long)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: GSRReading)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadings(readings: List<GSRReading>)
    
    @Update
    suspend fun updateReading(reading: GSRReading)
    
    @Delete
    suspend fun deleteReading(reading: GSRReading)
    
    @Query("SELECT * FROM gsr_readings WHERE id = :id")
    suspend fun getReadingById(id: String): GSRReading?
    
    /**
     * Get the latest reading for a specific device.
     * Useful for monitoring current device status.
     */
    @Query("SELECT * FROM gsr_readings WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestReadingForDevice(deviceId: String): GSRReading?
    
    /**
     * Get reading statistics for a session.
     */
    @Query("""
        SELECT 
            COUNT(*) as readingCount,
            AVG(gsrValue) as avgGSR,
            MAX(gsrValue) as maxGSR,
            MIN(gsrValue) as minGSR,
            MIN(timestamp) as startTime,
            MAX(timestamp) as endTime
        FROM gsr_readings 
        WHERE sessionId = :sessionId
    """)
    suspend fun getSessionStatistics(sessionId: String): GSRSessionStats?
}

/**
 * Data class for session statistics query result.
 */
data class GSRSessionStats(
    val readingCount: Int,
    val avgGSR: Double,
    val maxGSR: Double,
    val minGSR: Double,
    val startTime: Long,
    val endTime: Long
)