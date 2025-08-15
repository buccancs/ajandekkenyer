package com.gsr.recording.data.database

import androidx.room.*
import com.gsr.recording.data.model.RecordingSession
import com.gsr.recording.data.model.SessionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for recording sessions.
 * Provides database operations for session management.
 */
@Dao
interface RecordingSessionDao {
    
    @Query("SELECT * FROM recording_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<RecordingSession>>
    
    @Query("SELECT * FROM recording_sessions WHERE status = :status ORDER BY startTime DESC")
    fun getSessionsByStatus(status: SessionStatus): Flow<List<RecordingSession>>
    
    @Query("SELECT * FROM recording_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): RecordingSession?
    
    @Query("SELECT * FROM recording_sessions WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveSession(): RecordingSession?
    
    @Query("SELECT * FROM recording_sessions ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int = 10): List<RecordingSession>
    
    @Query("SELECT * FROM recording_sessions WHERE startTime BETWEEN :startTime AND :endTime ORDER BY startTime DESC")
    suspend fun getSessionsInDateRange(startTime: Long, endTime: Long): List<RecordingSession>
    
    @Query("SELECT * FROM recording_sessions WHERE participantId = :participantId ORDER BY startTime DESC")
    suspend fun getSessionsForParticipant(participantId: String): List<RecordingSession>
    
    @Query("UPDATE recording_sessions SET status = :status, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun updateSessionStatus(sessionId: String, status: SessionStatus, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE recording_sessions SET endTime = :endTime, recordingDuration = :duration, status = :status, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun finishSession(sessionId: String, endTime: Long, duration: Long, status: SessionStatus, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE recording_sessions SET gsrReadingCount = :count, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun updateGSRReadingCount(sessionId: String, count: Int, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE recording_sessions SET thermalFrameCount = :count, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun updateThermalFrameCount(sessionId: String, count: Int, updatedAt: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM recording_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)
    
    @Query("DELETE FROM recording_sessions WHERE startTime < :cutoffTime AND status IN ('COMPLETED', 'FAILED', 'CANCELLED')")
    suspend fun deleteOldCompletedSessions(cutoffTime: Long)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: RecordingSession)
    
    @Update
    suspend fun updateSession(session: RecordingSession)
    
    @Delete
    suspend fun deleteSession(session: RecordingSession)
    
    /**
     * Get session statistics including data counts.
     */
    @Query("""
        SELECT 
            s.*,
            COALESCE(g.gsrCount, 0) as actualGSRCount,
            COALESCE(t.thermalCount, 0) as actualThermalCount
        FROM recording_sessions s
        LEFT JOIN (SELECT sessionId, COUNT(*) as gsrCount FROM gsr_readings GROUP BY sessionId) g ON s.sessionId = g.sessionId
        LEFT JOIN (SELECT sessionId, COUNT(*) as thermalCount FROM thermal_frames GROUP BY sessionId) t ON s.sessionId = t.sessionId
        WHERE s.sessionId = :sessionId
    """)
    suspend fun getSessionWithDataCounts(sessionId: String): SessionWithDataCounts?
    
    /**
     * Count sessions by status for dashboard.
     */
    @Query("SELECT status, COUNT(*) as count FROM recording_sessions GROUP BY status")
    suspend fun getSessionCountsByStatus(): List<StatusCount>
}

/**
 * Data class for session with actual data counts.
 */
data class SessionWithDataCounts(
    val sessionId: String,
    val title: String?,
    val participantId: String?,
    val startTime: Long,
    val endTime: Long?,
    val status: SessionStatus,
    val gsrDeviceId: String?,
    val thermalDeviceId: String?,
    val pcConnectionId: String?,
    val dataFilePath: String?,
    val sampleRate: Int,
    val recordingDuration: Long?,
    val gsrReadingCount: Int,
    val thermalFrameCount: Int,
    val notes: String?,
    val actualGSRCount: Int,
    val actualThermalCount: Int
)

/**
 * Data class for status count query result.
 */
data class StatusCount(
    val status: SessionStatus,
    val count: Int
)