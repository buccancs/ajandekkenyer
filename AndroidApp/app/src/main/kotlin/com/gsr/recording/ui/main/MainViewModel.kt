package com.gsr.recording.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
    // Inject repository/use case dependencies here when available
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    init {
        // Initialize the view model
        loadInitialState()
    }
    
    private fun loadInitialState() {
        viewModelScope.launch {
            // Load initial state from repositories
            // This would typically fetch current device status, connection state, etc.
            updateUiState { 
                copy(
                    batteryLevel = getBatteryLevel(),
                    serverAddress = "192.168.1.100:9000" // Default or from preferences
                )
            }
        }
    }
    
    fun connectToPC() {
        viewModelScope.launch {
            try {
                updateUiState { copy(isConnecting = true) }
                
                // TODO: Implement actual PC connection logic
                // val connectionResult = pcConnectionRepository.connect(serverAddress)
                
                // Simulate connection for now
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
                // TODO: Implement actual PC disconnection logic
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
                // TODO: Implement actual recording start logic
                // recordingRepository.startRecording()
                
                val sessionId = "session_${System.currentTimeMillis()}"
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
                // TODO: Implement actual recording stop logic
                // recordingRepository.stopRecording()
                
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
                // TODO: Implement actual device checking logic
                // val shimmerStatus = shimmerRepository.isConnected()
                // val thermalStatus = thermalCameraRepository.isConnected()
                
                // Simulate device status for now
                updateUiState { 
                    copy(
                        shimmerConnected = true, // Mock status
                        thermalCameraConnected = false // Mock status
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
        // TODO: Implement actual battery level reading
        // val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        // return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return 85 // Mock value
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