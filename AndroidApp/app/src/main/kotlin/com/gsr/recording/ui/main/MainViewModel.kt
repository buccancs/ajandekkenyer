package com.gsr.recording.ui.main

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject
import com.gsr.recording.domain.repository.SessionRepository
import com.gsr.recording.domain.repository.DeviceRepository
import com.gsr.recording.data.network.NetworkService
import com.gsr.recording.data.network.SessionConfiguration
import com.gsr.recording.device.ShimmerDeviceManager
import com.gsr.recording.device.ThermalCameraManager
import com.gsr.recording.service.DataCollectionService
import java.util.UUID

/**
 * ViewModel for the main screen of the GSR Recording application.
 * Manages UI state and coordinates with domain layer components.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val deviceRepository: DeviceRepository,
    private val networkService: NetworkService,
    private val shimmerDeviceManager: ShimmerDeviceManager,
    private val thermalCameraManager: ThermalCameraManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    init {
        // Initialize the view model
        loadInitialState()
        observeDeviceStates()
        observeNetworkState()
    }
    
    private fun loadInitialState() {
        viewModelScope.launch {
            // Load initial state
            updateUiState { 
                copy(
                    batteryLevel = getBatteryLevel(),
                    serverAddress = "192.168.1.100:9000" // Default or from preferences
                )
            }
            
            // Check initial device connections
            checkDeviceConnections()
            
            // Load recent sessions
            loadRecentSessions()
        }
    }
    
    private fun observeDeviceStates() {
        viewModelScope.launch {
            // Observe Shimmer device status
            shimmerDeviceManager.deviceStatus.collect { status ->
                updateUiState { 
                    copy(shimmerConnected = status.isConnected())
                }
            }
        }
        
        viewModelScope.launch {
            // Observe thermal camera status
            thermalCameraManager.deviceStatus.collect { status ->
                updateUiState { 
                    copy(thermalCameraConnected = status.isConnected())
                }
            }
        }
    }
    
    private fun observeNetworkState() {
        viewModelScope.launch {
            networkService.connectionState.collect { state ->
                updateUiState { 
                    copy(
                        isConnectedToPC = state.isConnected(),
                        isConnecting = state.isConnecting()
                    )
                }
            }
        }
        
        viewModelScope.launch {
            networkService.lastError.collect { error ->
                if (error != null) {
                    updateUiState { copy(errorMessage = error) }
                }
            }
        }
    }
    
    fun connectToPC() {
        viewModelScope.launch {
            try {
                updateUiState { copy(isConnecting = true) }
                
                // Initialize network service with server address
                val serverAddress = _uiState.value.serverAddress ?: "192.168.1.100:9000"
                networkService.initialize(serverAddress)
                
                // Attempt connection to desktop controller
                val deviceId = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
                
                val result = networkService.connectToDesktop(
                    deviceId = deviceId,
                    appVersion = "1.0.0"
                )
                
                if (result.isSuccess) {
                    updateUiState { 
                        copy(
                            isConnectedToPC = true,
                            isConnecting = false,
                            errorMessage = null
                        )
                    }
                } else {
                    updateUiState { 
                        copy(
                            isConnectedToPC = false,
                            isConnecting = false,
                            errorMessage = result.exceptionOrNull()?.message ?: "Connection failed"
                        )
                    }
                }
            } catch (e: Exception) {
                updateUiState { 
                    copy(
                        isConnectedToPC = false,
                        isConnecting = false,
                        errorMessage = "Failed to connect: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun disconnectFromPC() {
        viewModelScope.launch {
            try {
                networkService.disconnect()
                
                updateUiState { 
                    copy(
                        isConnectedToPC = false,
                        currentSessionId = null,
                        isRecording = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                updateUiState { 
                    copy(errorMessage = "Failed to disconnect: ${e.message}")
                }
            }
        }
    }
    
    fun startRecording() {
        if (!_uiState.value.isConnectedToPC) {
            updateUiState { copy(errorMessage = "Not connected to PC") }
            return
        }
        
        viewModelScope.launch {
            try {
                val sessionId = "session_${UUID.randomUUID()}"
                
                // Create session configuration
                val configuration = SessionConfiguration(
                    sampleRate = 51, // Hz
                    recordingDuration = null, // Unlimited
                    enableGSR = true,
                    enableThermal = true,
                    notes = "Mobile GSR recording session"
                )
                
                // Start session on desktop
                val sessionResult = networkService.startSession(
                    sessionId = sessionId,
                    participantId = null, // Can be set from UI
                    configuration = configuration
                )
                
                if (sessionResult.isSuccess) {
                    // Start local data collection service
                    val serviceIntent = Intent(context, DataCollectionService::class.java).apply {
                        action = DataCollectionService.ACTION_START_RECORDING
                        putExtra(DataCollectionService.EXTRA_SESSION_ID, sessionId)
                        putExtra(DataCollectionService.EXTRA_SAMPLE_RATE, 51)
                    }
                    context.startForegroundService(serviceIntent)
                    
                    updateUiState { 
                        copy(
                            isRecording = true,
                            currentSessionId = sessionId,
                            errorMessage = null
                        )
                    }
                    
                    // Add to recent sessions
                    addToRecentSessions(sessionId)
                } else {
                    updateUiState { 
                        copy(errorMessage = "Failed to start recording session: ${sessionResult.exceptionOrNull()?.message}")
                    }
                }
                
            } catch (e: Exception) {
                updateUiState { 
                    copy(errorMessage = "Failed to start recording: ${e.message}")
                }
            }
        }
    }
    
    fun stopRecording() {
        viewModelScope.launch {
            try {
                val currentSessionId = _uiState.value.currentSessionId
                if (currentSessionId != null) {
                    // Stop desktop session
                    networkService.stopSession(currentSessionId)
                    
                    // Stop local data collection service
                    val serviceIntent = Intent(context, DataCollectionService::class.java).apply {
                        action = DataCollectionService.ACTION_STOP_RECORDING
                    }
                    context.startService(serviceIntent)
                }
                
                updateUiState { 
                    copy(
                        isRecording = false,
                        errorMessage = null
                        // Keep session ID for reference
                    )
                }
                
            } catch (e: Exception) {
                updateUiState { 
                    copy(errorMessage = "Failed to stop recording: ${e.message}")
                }
            }
        }
    }
    
    fun checkDeviceConnections() {
        viewModelScope.launch {
            try {
                // Scan for available devices
                val shimmerDevices = shimmerDeviceManager.scanForDevices()
                val thermalDevices = thermalCameraManager.scanForDevices()
                
                // Update device status based on availability
                updateUiState { 
                    copy(
                        shimmerConnected = shimmerDevices.isNotEmpty() && shimmerDeviceManager.deviceStatus.value.isConnected(),
                        thermalCameraConnected = thermalDevices.isNotEmpty() && thermalCameraManager.deviceStatus.value.isConnected()
                    )
                }
                
                // Save discovered devices to repository
                (shimmerDevices + thermalDevices).forEach { device ->
                    deviceRepository.addDevice(device)
                }
                
            } catch (e: Exception) {
                updateUiState { 
                    copy(errorMessage = "Failed to check devices: ${e.message}")
                }
            }
        }
    }
    
    fun connectShimmerDevice() {
        viewModelScope.launch {
            try {
                val devices = shimmerDeviceManager.scanForDevices()
                if (devices.isNotEmpty()) {
                    val result = shimmerDeviceManager.connectDevice(devices.first())
                    if (result.isFailure) {
                        updateUiState { 
                            copy(errorMessage = "Failed to connect Shimmer device: ${result.exceptionOrNull()?.message}")
                        }
                    }
                } else {
                    updateUiState { 
                        copy(errorMessage = "No Shimmer devices found")
                    }
                }
            } catch (e: Exception) {
                updateUiState { 
                    copy(errorMessage = "Error connecting Shimmer device: ${e.message}")
                }
            }
        }
    }
    
    fun connectThermalCamera() {
        viewModelScope.launch {
            try {
                val devices = thermalCameraManager.scanForDevices()
                if (devices.isNotEmpty()) {
                    val result = thermalCameraManager.connectDevice(devices.first())
                    if (result.isFailure) {
                        updateUiState { 
                            copy(errorMessage = "Failed to connect thermal camera: ${result.exceptionOrNull()?.message}")
                        }
                    }
                } else {
                    updateUiState { 
                        copy(errorMessage = "No thermal cameras found")
                    }
                }
            } catch (e: Exception) {
                updateUiState { 
                    copy(errorMessage = "Error connecting thermal camera: ${e.message}")
                }
            }
        }
    }
    
    fun clearError() {
        updateUiState { copy(errorMessage = null) }
    }
    
    private fun updateUiState(update: MainUiState.() -> MainUiState) {
        _uiState.value = _uiState.value.update()
    }
    
    private fun getBatteryLevel(): Int {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            85 // Fallback value
        }
    }
    
    private fun addToRecentSessions(sessionId: String) {
        updateUiState { 
            copy(
                recentSessions = (listOf(sessionId) + recentSessions).take(5)
            )
        }
    }
    
    private suspend fun loadRecentSessions() {
        try {
            val recentSessions = sessionRepository.getRecentSessions(5)
                .getOrNull()
                ?.map { it.sessionId }
                ?: emptyList()
            
            updateUiState { 
                copy(recentSessions = recentSessions)
            }
        } catch (e: Exception) {
            // Ignore errors when loading recent sessions
        }
    }
}

// Extension functions for connection state
private fun com.gsr.recording.data.network.NetworkConnectionState.isConnected() = 
    this == com.gsr.recording.data.network.NetworkConnectionState.CONNECTED

private fun com.gsr.recording.data.network.NetworkConnectionState.isConnecting() = 
    this == com.gsr.recording.data.network.NetworkConnectionState.CONNECTING

private fun com.gsr.recording.data.model.ConnectionStatus.isConnected() = 
    this == com.gsr.recording.data.model.ConnectionStatus.CONNECTED || 
    this == com.gsr.recording.data.model.ConnectionStatus.STREAMING

/**
 * UI state for the main screen.
 */
data class MainUiState(
    val isConnectedToPC: Boolean = false,
    val isConnecting: Boolean = false,
    val serverAddress: String? = null,
    val currentSessionId: String? = null,
    val isRecording: Boolean = false,
    val shimmerConnected: Boolean = false,
    val thermalCameraConnected: Boolean = false,
    val batteryLevel: Int = 0,
    val recentSessions: List<String> = emptyList(),
    val errorMessage: String? = null
)