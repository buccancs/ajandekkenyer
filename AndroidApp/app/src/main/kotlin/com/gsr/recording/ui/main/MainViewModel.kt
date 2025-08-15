package com.gsr.recording.ui.main

import android.content.Context
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsr.recording.shimmer.ShimmerManager
import com.gsr.recording.thermal.ThermalCameraManager
import com.gsr.recording.service.RecordingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the main screen of the GSR Recording application.
 * Manages UI state and coordinates with domain layer components.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shimmerManager: ShimmerManager,
    private val thermalCameraManager: ThermalCameraManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    init {
        // Initialize the view model
        loadInitialState()
        observeDeviceStates()
    }
    
    private fun loadInitialState() {
        viewModelScope.launch {
            // Load initial state from repositories
            updateUiState { 
                copy(
                    batteryLevel = getBatteryLevel(),
                    serverAddress = "192.168.1.100:9000" // Default or from preferences
                )
            }
            
            // Check initial device connections
            checkDeviceConnections()
        }
    }
    
    private fun observeDeviceStates() {
        // Observe Shimmer connection state
        viewModelScope.launch {
            shimmerManager.connectionState.collect { state ->
                updateUiState { 
                    copy(shimmerConnected = shimmerManager.isConnected())
                }
            }
        }
        
        // Observe thermal camera connection state
        viewModelScope.launch {
            thermalCameraManager.connectionState.collect { state ->
                updateUiState { 
                    copy(thermalCameraConnected = thermalCameraManager.isConnected())
                }
            }
        }
    }
    
    fun connectToPC() {
        viewModelScope.launch {
            try {
                updateUiState { copy(isConnecting = true) }
                
                // Implement actual PC connection logic
                // This would use a network repository to connect to the PC controller
                // For now, simulate connection
                kotlinx.coroutines.delay(2000)
                
                updateUiState { 
                    copy(
                        isConnectedToPC = true,
                        isConnecting = false
                    )
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
                // Implement actual PC disconnection logic
                // pcConnectionRepository.disconnect()
                
                updateUiState { 
                    copy(
                        isConnectedToPC = false,
                        currentSessionId = null,
                        isRecording = false
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
                // Implement actual recording start logic using RecordingService
                val sessionId = "session_${System.currentTimeMillis()}"
                
                // Start the recording service
                val serviceIntent = android.content.Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_START_RECORDING
                    putExtra(RecordingService.EXTRA_SESSION_ID, sessionId)
                }
                context.startForegroundService(serviceIntent)
                
                updateUiState { 
                    copy(
                        isRecording = true,
                        currentSessionId = sessionId
                    )
                }
                
                // Add to recent sessions
                addToRecentSessions(sessionId)
                
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
                // Implement actual recording stop logic using RecordingService
                val serviceIntent = android.content.Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_STOP_RECORDING
                }
                context.startService(serviceIntent)
                
                updateUiState { 
                    copy(
                        isRecording = false
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
                // Implement actual device checking logic
                val shimmerStatus = shimmerManager.isConnected()
                val thermalStatus = thermalCameraManager.isConnected()
                
                updateUiState { 
                    copy(
                        shimmerConnected = shimmerStatus,
                        thermalCameraConnected = thermalStatus
                    )
                }
            } catch (e: Exception) {
                updateUiState { 
                    copy(errorMessage = "Failed to check devices: ${e.message}")
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
        // Implement actual battery level reading
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
}

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