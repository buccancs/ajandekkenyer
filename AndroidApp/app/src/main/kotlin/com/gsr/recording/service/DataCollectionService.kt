package com.gsr.recording.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gsr.recording.MainActivity
import com.gsr.recording.R
import com.gsr.recording.data.model.*
import com.gsr.recording.data.network.NetworkService
import com.gsr.recording.data.network.GSRDataPoint
import com.gsr.recording.device.ShimmerDeviceManager
import com.gsr.recording.domain.repository.SessionRepository
import com.gsr.recording.domain.repository.DeviceRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Foreground service for continuous GSR data collection and synchronization.
 * Manages device connections, data streaming, and network communication.
 */
@AndroidEntryPoint
class DataCollectionService : Service() {
    
    @Inject lateinit var shimmerDeviceManager: ShimmerDeviceManager
    @Inject lateinit var networkService: NetworkService
    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var deviceRepository: DeviceRepository
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentSession: RecordingSession? = null
    private var dataCollectionJob: Job? = null
    private var heartbeatJob: Job? = null
    
    private val _serviceState = MutableStateFlow(ServiceState.IDLE)
    val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()
    
    private val gsrDataBuffer = mutableListOf<GSRReading>()
    private var lastDataUpload = System.currentTimeMillis()
    
    companion object {
        private const val TAG = "DataCollectionService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "GSR_RECORDING_CHANNEL"
        private const val CHANNEL_NAME = "GSR Recording"
        private const val HEARTBEAT_INTERVAL = 30_000L // 30 seconds
        private const val DATA_UPLOAD_INTERVAL = 5_000L // 5 seconds
        private const val MAX_BUFFER_SIZE = 500
    }
    
    // Binder for activity communication
    inner class LocalBinder : Binder() {
        fun getService(): DataCollectionService = this@DataCollectionService
    }
    
    private val binder = LocalBinder()
    
    override fun onBind(intent: Intent): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DataCollectionService created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "Service started with action: $action")
        
        when (action) {
            ACTION_START_RECORDING -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                val participantId = intent.getStringExtra(EXTRA_PARTICIPANT_ID)
                val sampleRate = intent.getIntExtra(EXTRA_SAMPLE_RATE, 51)
                
                if (sessionId != null) {
                    startRecording(sessionId, participantId, sampleRate)
                }
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
            }
            ACTION_CONNECT_DEVICE -> {
                val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID)
                if (deviceId != null) {
                    connectDevice(deviceId)
                }
            }
            ACTION_DISCONNECT_DEVICE -> {
                disconnectDevice()
            }
        }
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DataCollectionService destroyed")
        cleanup()
    }
    
    /**
     * Start GSR data recording session.
     */
    fun startRecording(sessionId: String, participantId: String?, sampleRate: Int) {
        serviceScope.launch {
            try {
                Log.d(TAG, "Starting recording session: $sessionId")
                _serviceState.value = ServiceState.STARTING
                
                // Create session record
                val session = RecordingSession(
                    sessionId = sessionId,
                    title = "GSR Recording ${System.currentTimeMillis()}",
                    participantId = participantId,
                    startTime = System.currentTimeMillis(),
                    status = SessionStatus.RECORDING,
                    sampleRate = sampleRate
                )
                
                sessionRepository.createSession(session)
                currentSession = session
                
                // Start foreground notification
                startForeground(NOTIFICATION_ID, createRecordingNotification(sessionId))
                
                // Start device data collection
                startDataCollection(sessionId, sampleRate)
                
                // Start heartbeat
                startHeartbeat()
                
                _serviceState.value = ServiceState.RECORDING
                Log.d(TAG, "Recording started successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                _serviceState.value = ServiceState.ERROR
            }
        }
    }
    
    /**
     * Stop current recording session.
     */
    fun stopRecording() {
        serviceScope.launch {
            try {
                Log.d(TAG, "Stopping recording session")
                _serviceState.value = ServiceState.STOPPING
                
                // Stop data collection
                stopDataCollection()
                
                // Stop heartbeat
                stopHeartbeat()
                
                // Finalize session
                currentSession?.let { session ->
                    val finalizedSession = session.copy(
                        endTime = System.currentTimeMillis(),
                        status = SessionStatus.COMPLETED
                    )
                    sessionRepository.updateSession(finalizedSession)
                    
                    // Upload any remaining data
                    uploadBufferedData()
                    
                    // Notify desktop of session end
                    networkService.stopSession(session.sessionId)
                }
                
                currentSession = null
                _serviceState.value = ServiceState.IDLE
                
                // Stop foreground service
                stopForeground(true)
                Log.d(TAG, "Recording stopped successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop recording", e)
                _serviceState.value = ServiceState.ERROR
            }
        }
    }
    
    /**
     * Connect to a Shimmer device.
     */
    fun connectDevice(deviceId: String) {
        serviceScope.launch {
            try {
                Log.d(TAG, "Connecting to device: $deviceId")
                
                // Get device from repository
                val device = deviceRepository.getDeviceById(deviceId).getOrNull()
                if (device == null) {
                    Log.e(TAG, "Device not found: $deviceId")
                    return@launch
                }
                
                // Connect via device manager
                val result = shimmerDeviceManager.connectDevice(device)
                if (result.isSuccess) {
                    Log.d(TAG, "Device connected successfully: $deviceId")
                    
                    // Update device status
                    val updatedDevice = device.copy(
                        connectionStatus = ConnectionStatus.CONNECTED,
                        lastSeen = System.currentTimeMillis()
                    )
                    deviceRepository.updateDevice(updatedDevice)
                } else {
                    Log.e(TAG, "Failed to connect device: ${result.exceptionOrNull()?.message}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception connecting device", e)
            }
        }
    }
    
    /**
     * Disconnect from current device.
     */
    fun disconnectDevice() {
        serviceScope.launch {
            try {
                Log.d(TAG, "Disconnecting device")
                shimmerDeviceManager.disconnectDevice()
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception disconnecting device", e)
            }
        }
    }
    
    private fun startDataCollection(sessionId: String, sampleRate: Int) {
        dataCollectionJob = serviceScope.launch {
            try {
                // Start Shimmer data streaming
                shimmerDeviceManager.startDataStreaming(sessionId, sampleRate)
                
                // Collect GSR data
                shimmerDeviceManager.gsrData
                    .filterNotNull()
                    .collect { gsrReading ->
                        // Update session ID and save to database
                        val sessionReading = gsrReading.copy(sessionId = sessionId)
                        
                        // Save to local database
                        sessionRepository.addGSRReading(sessionReading)
                        
                        // Buffer for network upload
                        synchronized(gsrDataBuffer) {
                            gsrDataBuffer.add(sessionReading)
                            
                            // Upload data periodically
                            if (gsrDataBuffer.size >= MAX_BUFFER_SIZE || 
                                System.currentTimeMillis() - lastDataUpload > DATA_UPLOAD_INTERVAL) {
                                uploadBufferedData()
                            }
                        }
                    }
                    
            } catch (e: Exception) {
                Log.e(TAG, "Error in data collection", e)
            }
        }
    }
    
    private fun stopDataCollection() {
        dataCollectionJob?.cancel()
        dataCollectionJob = null
        
        serviceScope.launch {
            shimmerDeviceManager.stopDataStreaming()
        }
    }
    
    private fun startHeartbeat() {
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                try {
                    val batteryLevel = getBatteryLevel()
                    val activeDevices = getActiveDeviceIds()
                    
                    networkService.sendHeartbeat(batteryLevel, activeDevices)
                    
                    delay(HEARTBEAT_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Heartbeat failed", e)
                    delay(HEARTBEAT_INTERVAL)
                }
            }
        }
    }
    
    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
    
    private suspend fun uploadBufferedData() {
        if (gsrDataBuffer.isEmpty()) return
        
        try {
            val currentSessionId = currentSession?.sessionId ?: return
            val dataToUpload = synchronized(gsrDataBuffer) {
                val data = gsrDataBuffer.toList()
                gsrDataBuffer.clear()
                data
            }
            
            val gsrDataPoints = dataToUpload.map { reading ->
                GSRDataPoint(
                    timestamp = reading.timestamp,
                    gsrValue = reading.gsrValue,
                    rawValue = reading.rawValue,
                    quality = reading.quality.name,
                    batteryLevel = reading.batteryLevel
                )
            }
            
            val batchId = "batch_${System.currentTimeMillis()}"
            val deviceId = dataToUpload.firstOrNull()?.deviceId ?: "unknown"
            
            val result = networkService.uploadGSRData(
                sessionId = currentSessionId,
                batchId = batchId,
                readings = gsrDataPoints,
                deviceId = deviceId
            )
            
            if (result.isSuccess) {
                lastDataUpload = System.currentTimeMillis()
                Log.d(TAG, "Uploaded ${gsrDataPoints.size} GSR readings")
            } else {
                Log.e(TAG, "Failed to upload GSR data: ${result.exceptionOrNull()?.message}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading data", e)
        }
    }
    
    private fun getBatteryLevel(): Int {
        return try {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            85 // Fallback value
        }
    }
    
    private suspend fun getActiveDeviceIds(): List<String> {
        return try {
            deviceRepository.getConnectedDevices()
                .getOrNull()
                ?.map { it.deviceId }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "GSR recording session notifications"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createRecordingNotification(sessionId: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GSR Recording Active")
            .setContentText("Session: $sessionId")
            .setSmallIcon(android.R.drawable.ic_media_play) // Using system icon temporarily
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    private fun cleanup() {
        serviceScope.cancel()
        dataCollectionJob?.cancel()
        heartbeatJob?.cancel()
        
        // Ensure device is disconnected
        serviceScope.launch {
            shimmerDeviceManager.disconnectDevice()
        }
    }
    
    // Static constants for service actions
    companion object {
        const val ACTION_START_RECORDING = "START_RECORDING"
        const val ACTION_STOP_RECORDING = "STOP_RECORDING"
        const val ACTION_CONNECT_DEVICE = "CONNECT_DEVICE"
        const val ACTION_DISCONNECT_DEVICE = "DISCONNECT_DEVICE"
        
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_PARTICIPANT_ID = "participant_id"
        const val EXTRA_SAMPLE_RATE = "sample_rate"
        const val EXTRA_DEVICE_ID = "device_id"
    }
}

/**
 * Service state enumeration.
 */
enum class ServiceState {
    IDLE,
    STARTING,
    RECORDING,
    STOPPING,
    ERROR
}