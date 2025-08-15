package com.gsr.recording.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Data model representing a recording session.
 * Contains metadata and configuration for a complete GSR recording session.
 */
@Entity(tableName = "recording_sessions")
data class RecordingSession(
    @PrimaryKey
    val sessionId: String,
    val title: String? = null,
    val participantId: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val status: SessionStatus = SessionStatus.ACTIVE,
    val gsrDeviceId: String? = null,
    val thermalDeviceId: String? = null,
    val pcConnectionId: String? = null,
    val dataFilePath: String? = null, // Path to session data directory
    val sampleRate: Int = 51, // Hz
    val recordingDuration: Long? = null, // Milliseconds
    val gsrReadingCount: Int = 0,
    val thermalFrameCount: Int = 0,
    val notes: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

/**
 * Recording session status to track lifecycle.
 */
enum class SessionStatus {
    PREPARING,  // Setting up devices and connections
    ACTIVE,     // Currently recording data
    PAUSED,     // Temporarily suspended
    COMPLETED,  // Successfully finished
    FAILED,     // Terminated due to error
    CANCELLED   // User cancelled session
}

/**
 * Session statistics for monitoring and analysis.
 */
data class SessionStatistics(
    val sessionId: String,
    val totalDuration: Long, // Milliseconds
    val averageGSR: Double, // μS
    val gsrVariance: Double,
    val peakGSRValue: Double,
    val averageTemperature: Float, // Celsius
    val temperatureRange: Float,
    val dataQualityScore: Float, // 0.0 - 1.0
    val deviceDisconnections: Int,
    val dataLossPercentage: Float
)