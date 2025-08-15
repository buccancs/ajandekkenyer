package com.gsr.recording.data.network

import retrofit2.Response
import retrofit2.http.*

/**
 * API interface for communication with the Python desktop controller.
 * Defines REST endpoints for synchronization and command exchange.
 */
interface DesktopControllerApi {
    
    /**
     * Establish connection and handshake with desktop controller.
     */
    @POST("api/v1/connect")
    suspend fun connectToDesktop(@Body request: ConnectionRequest): Response<ConnectionResponse>
    
    /**
     * Send heartbeat to maintain connection.
     */
    @POST("api/v1/heartbeat")
    suspend fun sendHeartbeat(@Body request: HeartbeatRequest): Response<HeartbeatResponse>
    
    /**
     * Start a new recording session on both mobile and desktop.
     */
    @POST("api/v1/session/start")
    suspend fun startSession(@Body request: StartSessionRequest): Response<SessionResponse>
    
    /**
     * Stop the current recording session.
     */
    @POST("api/v1/session/stop")
    suspend fun stopSession(@Body request: StopSessionRequest): Response<SessionResponse>
    
    /**
     * Sync session metadata and configuration.
     */
    @POST("api/v1/session/sync")
    suspend fun syncSession(@Body request: SessionSyncRequest): Response<SessionSyncResponse>
    
    /**
     * Upload GSR data batch to desktop for processing.
     */
    @POST("api/v1/data/gsr")
    suspend fun uploadGSRData(@Body request: GSRDataUpload): Response<DataUploadResponse>
    
    /**
     * Upload thermal frame metadata (actual frame files transferred separately).
     */
    @POST("api/v1/data/thermal")
    suspend fun uploadThermalMetadata(@Body request: ThermalDataUpload): Response<DataUploadResponse>
    
    /**
     * Get desktop application status and device information.
     */
    @GET("api/v1/status")
    suspend fun getDesktopStatus(): Response<DesktopStatus>
    
    /**
     * Send device connection updates to desktop.
     */
    @POST("api/v1/devices/update")
    suspend fun updateDeviceStatus(@Body request: DeviceUpdateRequest): Response<BasicResponse>
    
    /**
     * Disconnect from desktop controller.
     */
    @POST("api/v1/disconnect")
    suspend fun disconnect(@Body request: DisconnectRequest): Response<BasicResponse>
}

// Request/Response data classes

data class ConnectionRequest(
    val mobileDeviceId: String,
    val appVersion: String,
    val capabilities: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

data class ConnectionResponse(
    val success: Boolean,
    val connectionId: String?,
    val serverVersion: String?,
    val supportedFeatures: List<String>?,
    val message: String?
)

data class HeartbeatRequest(
    val connectionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val batteryLevel: Int?,
    val activeDevices: List<String>
)

data class HeartbeatResponse(
    val success: Boolean,
    val serverTimestamp: Long,
    val commands: List<RemoteCommand>?
)

data class StartSessionRequest(
    val connectionId: String,
    val sessionId: String,
    val participantId: String?,
    val configuration: SessionConfiguration
)

data class StopSessionRequest(
    val connectionId: String,
    val sessionId: String,
    val endReason: String = "USER_STOPPED"
)

data class SessionResponse(
    val success: Boolean,
    val sessionId: String?,
    val timestamp: Long,
    val message: String?
)

data class SessionSyncRequest(
    val connectionId: String,
    val sessionMetadata: SessionMetadata
)

data class SessionSyncResponse(
    val success: Boolean,
    val syncedTimestamp: Long,
    val conflicts: List<String>?
)

data class GSRDataUpload(
    val connectionId: String,
    val sessionId: String,
    val batchId: String,
    val readings: List<GSRDataPoint>,
    val deviceId: String
)

data class ThermalDataUpload(
    val connectionId: String,
    val sessionId: String,
    val batchId: String,
    val frames: List<ThermalFrameMetadata>,
    val deviceId: String
)

data class DataUploadResponse(
    val success: Boolean,
    val batchId: String,
    val receivedCount: Int,
    val nextBatchId: String?
)

data class DesktopStatus(
    val isRunning: Boolean,
    val version: String,
    val connectedDevices: List<String>,
    val activeSessions: List<String>,
    val storageSpace: StorageInfo,
    val systemLoad: SystemLoad
)

data class DeviceUpdateRequest(
    val connectionId: String,
    val deviceUpdates: List<DeviceStatusUpdate>
)

data class DisconnectRequest(
    val connectionId: String,
    val reason: String = "USER_DISCONNECT"
)

data class BasicResponse(
    val success: Boolean,
    val message: String?
)

// Supporting data classes

data class RemoteCommand(
    val command: String,
    val parameters: Map<String, Any>?,
    val timestamp: Long
)

data class SessionConfiguration(
    val sampleRate: Int,
    val recordingDuration: Long?,
    val enableGSR: Boolean,
    val enableThermal: Boolean,
    val notes: String?
)

data class SessionMetadata(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long?,
    val participantId: String?,
    val deviceIds: List<String>,
    val dataFilePath: String?
)

data class GSRDataPoint(
    val timestamp: Long,
    val gsrValue: Double,
    val rawValue: Int,
    val quality: String,
    val batteryLevel: Int?
)

data class ThermalFrameMetadata(
    val frameId: String,
    val timestamp: Long,
    val frameNumber: Int,
    val averageTemperature: Float,
    val maxTemperature: Float,
    val minTemperature: Float,
    val dataPath: String
)

data class StorageInfo(
    val totalSpace: Long,
    val availableSpace: Long,
    val usedSpace: Long
)

data class SystemLoad(
    val cpuUsage: Float,
    val memoryUsage: Float,
    val diskUsage: Float
)

data class DeviceStatusUpdate(
    val deviceId: String,
    val connectionStatus: String,
    val batteryLevel: Int?,
    val lastSeen: Long
)