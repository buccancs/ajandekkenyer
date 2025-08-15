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
import com.gsr.recording.shimmer.ShimmerManager
import com.gsr.recording.thermal.ThermalCameraManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service for GSR and thermal camera recording.
 * Handles background data collection and maintains device connections.
 */
@AndroidEntryPoint
class RecordingService : Service() {
    
    companion object {
        private const val TAG = "RecordingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "GSR_RECORDING_CHANNEL"
        private const val CHANNEL_NAME = "GSR Recording Service"
        
        const val ACTION_START_RECORDING = "com.gsr.recording.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.gsr.recording.STOP_RECORDING"
        const val ACTION_PAUSE_RECORDING = "com.gsr.recording.PAUSE_RECORDING"
        
        const val EXTRA_SESSION_ID = "session_id"
    }
    
    @Inject
    lateinit var shimmerManager: ShimmerManager
    
    @Inject
    lateinit var thermalCameraManager: ThermalCameraManager
    
    private val binder = RecordingBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    
    private val _recordingState = MutableStateFlow(RecordingState.STOPPED)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    private val _sessionInfo = MutableStateFlow<SessionInfo?>(null)
    val sessionInfo: StateFlow<SessionInfo?> = _sessionInfo.asStateFlow()
    
    private var currentSessionId: String? = null
    private var recordingStartTime: Long = 0L
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RecordingService created")
        
        createNotificationChannel()
        initializeManagers()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: generateSessionId()
                startRecording(sessionId)
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
            }
            ACTION_PAUSE_RECORDING -> {
                pauseRecording()
            }
        }
        
        return START_STICKY // Restart service if killed
    }
    
    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "Service bound")
        return binder
    }
    
    override fun onDestroy() {
        Log.d(TAG, "RecordingService destroyed")
        
        // Clean up resources
        stopRecording()
        shimmerManager.cleanup()
        thermalCameraManager.cleanup()
        serviceScope.cancel()
        
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifications for GSR recording service"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun initializeManagers() {
        serviceScope.launch {
            try {
                // Initialize Shimmer devices
                if (shimmerManager.startDeviceDiscovery()) {
                    Log.d(TAG, "Shimmer device discovery started")
                }
                
                // Initialize thermal camera
                if (thermalCameraManager.initializeThermalCamera()) {
                    Log.d(TAG, "Thermal camera initialized")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize managers: ${e.message}", e)
            }
        }
    }
    
    private fun startRecording(sessionId: String) {
        serviceScope.launch {
            try {
                Log.d(TAG, "Starting recording session: $sessionId")
                
                currentSessionId = sessionId
                recordingStartTime = System.currentTimeMillis()
                _recordingState.value = RecordingState.STARTING
                
                // Start foreground service with notification
                startForeground(NOTIFICATION_ID, createRecordingNotification(sessionId))
                
                // Start Shimmer GSR streaming
                val shimmerStarted = shimmerManager.startGSRStreaming()
                Log.d(TAG, "Shimmer streaming started: $shimmerStarted")
                
                // Start thermal camera recording
                val thermalConfig = com.gsr.recording.thermal.ThermalConfig(
                    frameRate = 30,
                    resolution = com.gsr.recording.thermal.ThermalResolution.HD,
                    temperatureRange = com.gsr.recording.thermal.TemperatureRange.STANDARD,
                    syncTimestamp = recordingStartTime
                )
                val thermalStarted = thermalCameraManager.startThermalRecording(thermalConfig)
                Log.d(TAG, "Thermal recording started: $thermalStarted")
                
                if (shimmerStarted || thermalStarted) {
                    _recordingState.value = RecordingState.RECORDING
                    updateSessionInfo(sessionId)
                    Log.d(TAG, "Recording session $sessionId started successfully")
                } else {
                    _recordingState.value = RecordingState.ERROR
                    Log.e(TAG, "Failed to start recording devices")
                    stopSelf()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording: ${e.message}", e)
                _recordingState.value = RecordingState.ERROR
                stopSelf()
            }
        }
    }
    
    private fun stopRecording() {
        serviceScope.launch {
            try {
                Log.d(TAG, "Stopping recording session: $currentSessionId")
                _recordingState.value = RecordingState.STOPPING
                
                // Stop Shimmer streaming
                shimmerManager.stopGSRStreaming()
                
                // Stop thermal camera recording
                thermalCameraManager.stopThermalRecording()
                
                // Update session info with final stats
                finalizeSessionInfo()
                
                _recordingState.value = RecordingState.STOPPED
                currentSessionId = null
                recordingStartTime = 0L
                
                // Stop foreground service
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                
                Log.d(TAG, "Recording stopped successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop recording: ${e.message}", e)
            }
        }
    }
    
    private fun pauseRecording() {
        serviceScope.launch {
            try {
                Log.d(TAG, "Pausing recording session: $currentSessionId")
                
                if (_recordingState.value == RecordingState.RECORDING) {
                    _recordingState.value = RecordingState.PAUSED
                    
                    // Pause data collection but keep connections
                    shimmerManager.stopGSRStreaming()
                    thermalCameraManager.stopThermalRecording()
                    
                    updateNotification("Recording Paused")
                    
                } else if (_recordingState.value == RecordingState.PAUSED) {
                    // Resume recording
                    _recordingState.value = RecordingState.RECORDING
                    
                    shimmerManager.startGSRStreaming()
                    
                    val thermalConfig = com.gsr.recording.thermal.ThermalConfig(
                        frameRate = 30,
                        resolution = com.gsr.recording.thermal.ThermalResolution.HD,
                        temperatureRange = com.gsr.recording.thermal.TemperatureRange.STANDARD,
                        syncTimestamp = System.currentTimeMillis()
                    )
                    thermalCameraManager.startThermalRecording(thermalConfig)
                    
                    updateNotification("Recording")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause/resume recording: ${e.message}", e)
            }
        }
    }
    
    private fun createRecordingNotification(sessionId: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GSR Recording Active")
            .setContentText("Session: $sessionId")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_stop,
                "Stop",
                stopPendingIntent
            )
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
    
    private fun updateNotification(status: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GSR Recording")
            .setContentText("$status - Session: $currentSessionId")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()
            
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}"
    }
    
    private fun updateSessionInfo(sessionId: String) {
        val sessionInfo = SessionInfo(
            sessionId = sessionId,
            startTime = recordingStartTime,
            duration = System.currentTimeMillis() - recordingStartTime,
            shimmerConnected = shimmerManager.isConnected(),
            thermalConnected = thermalCameraManager.isConnected(),
            shimmerDeviceCount = shimmerManager.getConnectedDevices().size,
            status = _recordingState.value
        )
        _sessionInfo.value = sessionInfo
    }
    
    private fun finalizeSessionInfo() {
        _sessionInfo.value?.let { info ->
            val finalInfo = info.copy(
                duration = System.currentTimeMillis() - recordingStartTime,
                status = RecordingState.STOPPED
            )
            _sessionInfo.value = finalInfo
        }
    }
    
    /**
     * Binder for service communication
     */
    inner class RecordingBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }
    
    /**
     * Public interface for external components
     */
    fun startRecordingSession(sessionId: String) {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_START_RECORDING
            putExtra(EXTRA_SESSION_ID, sessionId)
        }
        startService(intent)
    }
    
    fun stopRecordingSession() {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        startService(intent)
    }
    
    fun pauseRecordingSession() {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_PAUSE_RECORDING
        }
        startService(intent)
    }
    
    fun isRecording(): Boolean {
        return _recordingState.value == RecordingState.RECORDING
    }
    
    fun getCurrentSessionId(): String? {
        return currentSessionId
    }
}

/**
 * Recording states
 */
enum class RecordingState {
    STOPPED,
    STARTING,
    RECORDING,
    PAUSED,
    STOPPING,
    ERROR
}

/**
 * Session information
 */
data class SessionInfo(
    val sessionId: String,
    val startTime: Long,
    val duration: Long,
    val shimmerConnected: Boolean,
    val thermalConnected: Boolean,
    val shimmerDeviceCount: Int,
    val status: RecordingState
)