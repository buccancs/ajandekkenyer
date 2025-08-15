package com.gsr.recording.data.network

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network service for communication with the Python desktop controller.
 * Handles connection management, data synchronization, and command processing.
 */
@Singleton
class NetworkService @Inject constructor() {
    
    private var api: DesktopControllerApi? = null
    private var connectionId: String? = null
    private var baseUrl: String = "http://192.168.1.100:9000"
    
    private val _connectionState = MutableStateFlow(NetworkConnectionState.DISCONNECTED)
    val connectionState: StateFlow<NetworkConnectionState> = _connectionState.asStateFlow()
    
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()
    
    companion object {
        private const val TAG = "NetworkService"
        private const val CONNECT_TIMEOUT = 10L
        private const val READ_TIMEOUT = 30L
        private const val WRITE_TIMEOUT = 30L
    }
    
    /**
     * Initialize the network service with server address.
     */
    fun initialize(serverAddress: String) {
        baseUrl = if (serverAddress.startsWith("http")) {
            serverAddress
        } else {
            "http://$serverAddress"
        }
        
        Log.d(TAG, "Initializing network service with server: $baseUrl")
        setupRetrofit()
    }
    
    /**
     * Connect to the desktop controller.
     */
    suspend fun connectToDesktop(deviceId: String, appVersion: String): Result<ConnectionResponse> {
        return try {
            _connectionState.value = NetworkConnectionState.CONNECTING
            
            val request = ConnectionRequest(
                mobileDeviceId = deviceId,
                appVersion = appVersion,
                capabilities = listOf("GSR_RECORDING", "THERMAL_CAPTURE", "REAL_TIME_SYNC")
            )
            
            val response = api?.connectToDesktop(request)
            
            if (response?.isSuccessful == true) {
                val connectionResponse = response.body()
                if (connectionResponse?.success == true) {
                    connectionId = connectionResponse.connectionId
                    _connectionState.value = NetworkConnectionState.CONNECTED
                    _lastError.value = null
                    Log.d(TAG, "Successfully connected to desktop controller")
                    Result.success(connectionResponse)
                } else {
                    val error = connectionResponse?.message ?: "Connection failed"
                    _connectionState.value = NetworkConnectionState.ERROR
                    _lastError.value = error
                    Log.e(TAG, "Connection failed: $error")
                    Result.failure(Exception(error))
                }
            } else {
                val error = "HTTP ${response?.code()}: ${response?.message()}"
                _connectionState.value = NetworkConnectionState.ERROR
                _lastError.value = error
                Log.e(TAG, "HTTP error: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            _connectionState.value = NetworkConnectionState.ERROR
            _lastError.value = e.message
            Log.e(TAG, "Exception during connection", e)
            Result.failure(e)
        }
    }
    
    /**
     * Disconnect from the desktop controller.
     */
    suspend fun disconnect(): Result<Unit> {
        return try {
            connectionId?.let { id ->
                val request = DisconnectRequest(
                    connectionId = id,
                    reason = "USER_DISCONNECT"
                )
                api?.disconnect(request)
            }
            
            connectionId = null
            _connectionState.value = NetworkConnectionState.DISCONNECTED
            _lastError.value = null
            Log.d(TAG, "Disconnected from desktop controller")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
            Result.failure(e)
        }
    }
    
    /**
     * Send heartbeat to maintain connection.
     */
    suspend fun sendHeartbeat(batteryLevel: Int, activeDevices: List<String>): Result<HeartbeatResponse> {
        val currentConnectionId = connectionId ?: return Result.failure(Exception("Not connected"))
        
        return try {
            val request = HeartbeatRequest(
                connectionId = currentConnectionId,
                batteryLevel = batteryLevel,
                activeDevices = activeDevices
            )
            
            val response = api?.sendHeartbeat(request)
            
            if (response?.isSuccessful == true) {
                val heartbeatResponse = response.body()
                if (heartbeatResponse?.success == true) {
                    Result.success(heartbeatResponse)
                } else {
                    Result.failure(Exception("Heartbeat failed"))
                }
            } else {
                Result.failure(Exception("HTTP ${response?.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Heartbeat failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Start a recording session on both mobile and desktop.
     */
    suspend fun startSession(
        sessionId: String,
        participantId: String?,
        configuration: SessionConfiguration
    ): Result<SessionResponse> {
        val currentConnectionId = connectionId ?: return Result.failure(Exception("Not connected"))
        
        return try {
            val request = StartSessionRequest(
                connectionId = currentConnectionId,
                sessionId = sessionId,
                participantId = participantId,
                configuration = configuration
            )
            
            val response = api?.startSession(request)
            
            if (response?.isSuccessful == true) {
                val sessionResponse = response.body()
                if (sessionResponse?.success == true) {
                    Log.d(TAG, "Session started successfully: $sessionId")
                    Result.success(sessionResponse)
                } else {
                    val error = sessionResponse?.message ?: "Session start failed"
                    Log.e(TAG, "Session start failed: $error")
                    Result.failure(Exception(error))
                }
            } else {
                val error = "HTTP ${response?.code()}: ${response?.message()}"
                Log.e(TAG, "Session start HTTP error: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during session start", e)
            Result.failure(e)
        }
    }
    
    /**
     * Stop the current recording session.
     */
    suspend fun stopSession(sessionId: String, endReason: String = "USER_STOPPED"): Result<SessionResponse> {
        val currentConnectionId = connectionId ?: return Result.failure(Exception("Not connected"))
        
        return try {
            val request = StopSessionRequest(
                connectionId = currentConnectionId,
                sessionId = sessionId,
                endReason = endReason
            )
            
            val response = api?.stopSession(request)
            
            if (response?.isSuccessful == true) {
                val sessionResponse = response.body()
                if (sessionResponse?.success == true) {
                    Log.d(TAG, "Session stopped successfully: $sessionId")
                    Result.success(sessionResponse)
                } else {
                    val error = sessionResponse?.message ?: "Session stop failed"
                    Log.e(TAG, "Session stop failed: $error")
                    Result.failure(Exception(error))
                }
            } else {
                val error = "HTTP ${response?.code()}: ${response?.message()}"
                Log.e(TAG, "Session stop HTTP error: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during session stop", e)
            Result.failure(e)
        }
    }
    
    /**
     * Upload GSR data batch to desktop.
     */
    suspend fun uploadGSRData(
        sessionId: String,
        batchId: String,
        readings: List<GSRDataPoint>,
        deviceId: String
    ): Result<DataUploadResponse> {
        val currentConnectionId = connectionId ?: return Result.failure(Exception("Not connected"))
        
        return try {
            val request = GSRDataUpload(
                connectionId = currentConnectionId,
                sessionId = sessionId,
                batchId = batchId,
                readings = readings,
                deviceId = deviceId
            )
            
            val response = api?.uploadGSRData(request)
            
            if (response?.isSuccessful == true) {
                val uploadResponse = response.body()
                if (uploadResponse?.success == true) {
                    Log.d(TAG, "GSR data uploaded successfully: ${readings.size} readings")
                    Result.success(uploadResponse)
                } else {
                    Log.e(TAG, "GSR data upload failed")
                    Result.failure(Exception("Upload failed"))
                }
            } else {
                val error = "HTTP ${response?.code()}: ${response?.message()}"
                Log.e(TAG, "GSR data upload HTTP error: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during GSR data upload", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get desktop application status.
     */
    suspend fun getDesktopStatus(): Result<DesktopStatus> {
        return try {
            val response = api?.getDesktopStatus()
            
            if (response?.isSuccessful == true) {
                val status = response.body()
                if (status != null) {
                    Result.success(status)
                } else {
                    Result.failure(Exception("Empty status response"))
                }
            } else {
                Result.failure(Exception("HTTP ${response?.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting desktop status", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update device status on desktop.
     */
    suspend fun updateDeviceStatus(deviceUpdates: List<DeviceStatusUpdate>): Result<BasicResponse> {
        val currentConnectionId = connectionId ?: return Result.failure(Exception("Not connected"))
        
        return try {
            val request = DeviceUpdateRequest(
                connectionId = currentConnectionId,
                deviceUpdates = deviceUpdates
            )
            
            val response = api?.updateDeviceStatus(request)
            
            if (response?.isSuccessful == true) {
                val updateResponse = response.body()
                if (updateResponse?.success == true) {
                    Result.success(updateResponse)
                } else {
                    Result.failure(Exception(updateResponse?.message ?: "Update failed"))
                }
            } else {
                Result.failure(Exception("HTTP ${response?.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception updating device status", e)
            Result.failure(e)
        }
    }
    
    private fun setupRetrofit() {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("$TAG-HTTP", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        api = retrofit.create(DesktopControllerApi::class.java)
        Log.d(TAG, "Retrofit configured for $baseUrl")
    }
}

/**
 * Network connection state enumeration.
 */
enum class NetworkConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}