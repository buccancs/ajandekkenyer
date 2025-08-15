package com.gsr.recording.domain.repository

import com.gsr.recording.data.model.*
import com.gsr.recording.data.database.GSRSessionStats
import com.gsr.recording.data.database.ThermalSessionStats
import com.gsr.recording.data.network.DesktopStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for recording session management.
 * Defines operations for session lifecycle and data coordination.
 */
interface SessionRepository {
    
    /**
     * Get all recording sessions.
     */
    fun getAllSessions(): Flow<List<RecordingSession>>
    
    /**
     * Get sessions by status.
     */
    fun getSessionsByStatus(status: SessionStatus): Flow<List<RecordingSession>>
    
    /**
     * Get a specific session by ID.
     */
    suspend fun getSessionById(sessionId: String): RecordingSession?
    
    /**
     * Get the currently active session.
     */
    suspend fun getActiveSession(): RecordingSession?
    
    /**
     * Create a new recording session.
     */
    suspend fun createSession(
        title: String?,
        participantId: String?,
        gsrDeviceId: String?,
        thermalDeviceId: String?,
        sampleRate: Int = 51,
        notes: String?
    ): Result<RecordingSession>
    
    /**
     * Start recording for a session.
     */
    suspend fun startSession(sessionId: String): Result<RecordingSession>
    
    /**
     * Stop recording for a session.
     */
    suspend fun stopSession(sessionId: String): Result<RecordingSession>
    
    /**
     * Pause recording for a session.
     */
    suspend fun pauseSession(sessionId: String): Result<RecordingSession>
    
    /**
     * Resume recording for a paused session.
     */
    suspend fun resumeSession(sessionId: String): Result<RecordingSession>
    
    /**
     * Cancel a session.
     */
    suspend fun cancelSession(sessionId: String): Result<RecordingSession>
    
    /**
     * Update session metadata.
     */
    suspend fun updateSession(session: RecordingSession): Result<RecordingSession>
    
    /**
     * Delete a session and all associated data.
     */
    suspend fun deleteSession(sessionId: String): Result<Unit>
    
    /**
     * Get session statistics.
     */
    suspend fun getSessionStatistics(sessionId: String): SessionStatistics?
    
    /**
     * Get recent sessions.
     */
    suspend fun getRecentSessions(limit: Int = 10): List<RecordingSession>
    
    /**
     * Sync session with desktop controller.
     */
    suspend fun syncSessionWithDesktop(sessionId: String): Result<Unit>
}

/**
 * Repository interface for GSR data management.
 */
interface GSRRepository {
    
    /**
     * Get all GSR readings.
     */
    fun getAllReadings(): Flow<List<GSRReading>>
    
    /**
     * Get readings for a specific session.
     */
    fun getReadingsForSession(sessionId: String): Flow<List<GSRReading>>
    
    /**
     * Insert GSR reading.
     */
    suspend fun insertReading(reading: GSRReading): Result<Unit>
    
    /**
     * Insert batch of GSR readings.
     */
    suspend fun insertReadings(readings: List<GSRReading>): Result<Unit>
    
    /**
     * Get session GSR statistics.
     */
    suspend fun getSessionGSRStats(sessionId: String): GSRSessionStats?
    
    /**
     * Upload GSR data to desktop controller.
     */
    suspend fun uploadGSRData(sessionId: String, deviceId: String): Result<Unit>
    
    /**
     * Process and filter GSR readings.
     */
    suspend fun processGSRReadings(readings: List<GSRReading>): List<GSRReading>
}

/**
 * Repository interface for thermal data management.
 */
interface ThermalRepository {
    
    /**
     * Get all thermal frames.
     */
    fun getAllFrames(): Flow<List<ThermalFrame>>
    
    /**
     * Get frames for a specific session.
     */
    fun getFramesForSession(sessionId: String): Flow<List<ThermalFrame>>
    
    /**
     * Insert thermal frame.
     */
    suspend fun insertFrame(frame: ThermalFrame): Result<Unit>
    
    /**
     * Insert batch of thermal frames.
     */
    suspend fun insertFrames(frames: List<ThermalFrame>): Result<Unit>
    
    /**
     * Get session thermal statistics.
     */
    suspend fun getSessionThermalStats(sessionId: String): ThermalSessionStats?
    
    /**
     * Upload thermal data to desktop controller.
     */
    suspend fun uploadThermalData(sessionId: String, deviceId: String): Result<Unit>
    
    /**
     * Process thermal frame for analysis.
     */
    suspend fun processThermalFrame(frame: ThermalFrame): ThermalAnalysis?
}

/**
 * Repository interface for device management.
 */
interface DeviceRepository {
    
    /**
     * Get all devices.
     */
    fun getAllDevices(): Flow<List<Device>>
    
    /**
     * Get devices by type.
     */
    fun getDevicesByType(type: DeviceType): Flow<List<Device>>
    
    /**
     * Get connected devices.
     */
    suspend fun getConnectedDevices(): List<Device>
    
    /**
     * Add or update device.
     */
    suspend fun saveDevice(device: Device): Result<Unit>
    
    /**
     * Connect to device.
     */
    suspend fun connectDevice(deviceId: String): Result<Unit>
    
    /**
     * Disconnect device.
     */
    suspend fun disconnectDevice(deviceId: String): Result<Unit>
    
    /**
     * Update device status.
     */
    suspend fun updateDeviceStatus(deviceId: String, status: ConnectionStatus): Result<Unit>
    
    /**
     * Update device battery level.
     */
    suspend fun updateDeviceBattery(deviceId: String, batteryLevel: Int): Result<Unit>
    
    /**
     * Scan for available devices.
     */
    suspend fun scanForDevices(): Result<List<Device>>
    
    /**
     * Calibrate device.
     */
    suspend fun calibrateDevice(deviceId: String): Result<Unit>
    
    /**
     * Remove device.
     */
    suspend fun removeDevice(deviceId: String): Result<Unit>
}

/**
 * Repository interface for PC communication.
 */
interface NetworkRepository {
    
    /**
     * Connect to desktop controller.
     */
    suspend fun connectToDesktop(serverAddress: String): Result<String>
    
    /**
     * Disconnect from desktop controller.
     */
    suspend fun disconnectFromDesktop(): Result<Unit>
    
    /**
     * Check if connected to desktop.
     */
    suspend fun isConnectedToDesktop(): Boolean
    
    /**
     * Send heartbeat to desktop.
     */
    suspend fun sendHeartbeat(): Result<Unit>
    
    /**
     * Get desktop status.
     */
    suspend fun getDesktopStatus(): Result<DesktopStatus>
    
    /**
     * Sync session with desktop.
     */
    suspend fun syncSession(session: RecordingSession): Result<Unit>
    
    /**
     * Upload data batch to desktop.
     */
    suspend fun uploadDataBatch(sessionId: String, batchData: Any): Result<Unit>
}