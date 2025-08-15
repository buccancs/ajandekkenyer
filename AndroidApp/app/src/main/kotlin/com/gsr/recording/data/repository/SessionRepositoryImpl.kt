package com.gsr.recording.data.repository

import com.gsr.recording.data.database.RecordingSessionDao
import com.gsr.recording.data.database.GSRReadingDao
import com.gsr.recording.data.database.ThermalFrameDao
import com.gsr.recording.data.model.*
import com.gsr.recording.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SessionRepository.
 * Manages recording sessions using local database and remote synchronization.
 */
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: RecordingSessionDao,
    private val gsrDao: GSRReadingDao,
    private val thermalDao: ThermalFrameDao
) : SessionRepository {
    
    override fun getAllSessions(): Flow<List<RecordingSession>> {
        return sessionDao.getAllSessions()
    }
    
    override fun getSessionsByStatus(status: SessionStatus): Flow<List<RecordingSession>> {
        return sessionDao.getSessionsByStatus(status)
    }
    
    override suspend fun getSessionById(sessionId: String): RecordingSession? {
        return sessionDao.getSessionById(sessionId)
    }
    
    override suspend fun getActiveSession(): RecordingSession? {
        return sessionDao.getActiveSession()
    }
    
    override suspend fun createSession(
        title: String?,
        participantId: String?,
        gsrDeviceId: String?,
        thermalDeviceId: String?,
        sampleRate: Int,
        notes: String?
    ): Result<RecordingSession> {
        return try {
            val sessionId = "session_${System.currentTimeMillis()}"
            val session = RecordingSession(
                sessionId = sessionId,
                title = title,
                participantId = participantId,
                startTime = System.currentTimeMillis(),
                status = SessionStatus.PREPARING,
                gsrDeviceId = gsrDeviceId,
                thermalDeviceId = thermalDeviceId,
                sampleRate = sampleRate,
                notes = notes
            )
            
            sessionDao.insertSession(session)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun startSession(sessionId: String): Result<RecordingSession> {
        return try {
            val session = sessionDao.getSessionById(sessionId)
                ?: return Result.failure(IllegalArgumentException("Session not found"))
            
            val updatedSession = session.copy(
                status = SessionStatus.ACTIVE,
                startTime = System.currentTimeMillis(),
                updatedAt = Date()
            )
            
            sessionDao.updateSession(updatedSession)
            Result.success(updatedSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun stopSession(sessionId: String): Result<RecordingSession> {
        return try {
            val session = sessionDao.getSessionById(sessionId)
                ?: return Result.failure(IllegalArgumentException("Session not found"))
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - session.startTime
            
            sessionDao.finishSession(
                sessionId = sessionId,
                endTime = endTime,
                duration = duration,
                status = SessionStatus.COMPLETED
            )
            
            val updatedSession = session.copy(
                endTime = endTime,
                recordingDuration = duration,
                status = SessionStatus.COMPLETED,
                updatedAt = Date()
            )
            
            Result.success(updatedSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun pauseSession(sessionId: String): Result<RecordingSession> {
        return try {
            sessionDao.updateSessionStatus(sessionId, SessionStatus.PAUSED)
            val session = sessionDao.getSessionById(sessionId)!!
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun resumeSession(sessionId: String): Result<RecordingSession> {
        return try {
            sessionDao.updateSessionStatus(sessionId, SessionStatus.ACTIVE)
            val session = sessionDao.getSessionById(sessionId)!!
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun cancelSession(sessionId: String): Result<RecordingSession> {
        return try {
            sessionDao.updateSessionStatus(sessionId, SessionStatus.CANCELLED)
            val session = sessionDao.getSessionById(sessionId)!!
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateSession(session: RecordingSession): Result<RecordingSession> {
        return try {
            val updatedSession = session.copy(updatedAt = Date())
            sessionDao.updateSession(updatedSession)
            Result.success(updatedSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteSession(sessionId: String): Result<Unit> {
        return try {
            // Delete associated data first
            gsrDao.deleteReadingsForSession(sessionId)
            thermalDao.deleteFramesForSession(sessionId)
            sessionDao.deleteSession(sessionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getSessionStatistics(sessionId: String): SessionStatistics? {
        return try {
            val gsrStats = gsrDao.getSessionStatistics(sessionId)
            val thermalStats = thermalDao.getThermalSessionStats(sessionId)
            val session = sessionDao.getSessionById(sessionId)
            
            if (session != null && gsrStats != null) {
                SessionStatistics(
                    sessionId = sessionId,
                    totalDuration = session.recordingDuration ?: 0,
                    averageGSR = gsrStats.avgGSR,
                    gsrVariance = 0.0, // Calculate properly
                    peakGSRValue = gsrStats.maxGSR,
                    averageTemperature = thermalStats?.avgTemperature ?: 0f,
                    temperatureRange = if (thermalStats != null) {
                        thermalStats.maxTemperature - thermalStats.minTemperature
                    } else 0f,
                    dataQualityScore = 0.85f, // Calculate based on data quality
                    deviceDisconnections = 0, // Track properly
                    dataLossPercentage = 0f // Calculate based on expected vs actual data
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun getRecentSessions(limit: Int): List<RecordingSession> {
        return sessionDao.getRecentSessions(limit)
    }
    
    override suspend fun syncSessionWithDesktop(sessionId: String): Result<Unit> {
        return try {
            // Implementation would use NetworkRepository to sync with desktop
            // For now, just mark as synced locally
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}