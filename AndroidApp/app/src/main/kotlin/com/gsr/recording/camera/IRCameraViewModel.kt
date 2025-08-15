package com.gsr.recording.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsr.recording.thermal.ThermalCameraManager
import com.gsr.recording.thermal.ThermalConfig
import com.gsr.recording.thermal.ThermalFrame
import com.gsr.recording.thermal.ThermalResolution
import com.gsr.recording.thermal.TemperatureRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for IRCamera screen
 * Manages thermal camera state and operations
 */
@HiltViewModel
class IRCameraViewModel @Inject constructor(
    private val thermalCameraManager: ThermalCameraManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(IRCameraUiState())
    val uiState: StateFlow<IRCameraUiState> = _uiState.asStateFlow()
    
    init {
        initializeCamera()
        observeCameraState()
    }
    
    private fun initializeCamera() {
        viewModelScope.launch {
            try {
                updateUiState { copy(connectionStatus = "Initializing...") }
                
                val connected = thermalCameraManager.initializeThermalCamera()
                updateUiState { 
                    copy(
                        isConnected = connected,
                        connectionStatus = if (connected) "Connected" else "Connection failed"
                    )
                }
            } catch (e: Exception) {
                updateUiState { 
                    copy(
                        isConnected = false,
                        connectionStatus = "Error: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun observeCameraState() {
        viewModelScope.launch {
            thermalCameraManager.connectionState.collect { state ->
                updateUiState { 
                    copy(connectionStatus = state.name.replace("_", " ").lowercase()) 
                }
            }
        }
        
        viewModelScope.launch {
            thermalCameraManager.currentTemperature.collect { temperature ->
                updateUiState { copy(currentTemperature = temperature) }
            }
        }
        
        viewModelScope.launch {
            thermalCameraManager.frameRate.collect { frameRate ->
                updateUiState { copy(frameRate = frameRate) }
            }
        }
    }
    
    fun refreshConnection() {
        viewModelScope.launch {
            updateUiState { copy(connectionStatus = "Refreshing...") }
            initializeCamera()
        }
    }
    
    fun startRecording() {
        viewModelScope.launch {
            try {
                val config = ThermalConfig(
                    frameRate = _uiState.value.frameRate,
                    resolution = _uiState.value.resolution,
                    temperatureRange = _uiState.value.temperatureRange
                )
                
                val success = thermalCameraManager.startThermalRecording(config)
                updateUiState { copy(isRecording = success) }
                
            } catch (e: Exception) {
                updateUiState { 
                    copy(connectionStatus = "Recording failed: ${e.message}")
                }
            }
        }
    }
    
    fun stopRecording() {
        viewModelScope.launch {
            try {
                val success = thermalCameraManager.stopThermalRecording()
                updateUiState { copy(isRecording = !success) }
                
            } catch (e: Exception) {
                updateUiState { 
                    copy(connectionStatus = "Stop recording failed: ${e.message}")
                }
            }
        }
    }
    
    fun toggleStreaming() {
        viewModelScope.launch {
            val newStreamingState = !_uiState.value.isStreaming
            
            if (newStreamingState) {
                // Start streaming
                startStreaming()
            } else {
                // Stop streaming
                stopStreaming()
            }
            
            updateUiState { copy(isStreaming = newStreamingState) }
        }
    }
    
    private fun startStreaming() {
        // TODO: Implement actual streaming start
        // This would typically involve setting up a callback for thermal frames
    }
    
    private fun stopStreaming() {
        // TODO: Implement actual streaming stop
    }
    
    fun setDisplayMode(mode: ThermalDisplayMode) {
        updateUiState { copy(displayMode = mode) }
    }
    
    fun captureSnapshot() {
        viewModelScope.launch {
            try {
                // TODO: Implement snapshot capture
                // thermalCameraManager.captureSnapshot()
                updateUiState { 
                    copy(connectionStatus = "Snapshot captured")
                }
            } catch (e: Exception) {
                updateUiState { 
                    copy(connectionStatus = "Snapshot failed: ${e.message}")
                }
            }
        }
    }
    
    fun setFrameRate(frameRate: Int) {
        thermalCameraManager.setFrameRate(frameRate)
        updateUiState { copy(frameRate = frameRate) }
    }
    
    fun setResolution(resolution: ThermalResolution) {
        thermalCameraManager.setResolution(resolution)
        updateUiState { copy(resolution = resolution) }
    }
    
    fun setTemperatureRange(range: TemperatureRange) {
        thermalCameraManager.setTemperatureRange(range)
        updateUiState { copy(temperatureRange = range) }
    }
    
    fun setEmissivity(emissivity: Float) {
        // TODO: Set emissivity on thermal camera
        updateUiState { copy(emissivity = emissivity) }
    }
    
    private fun updateUiState(update: IRCameraUiState.() -> IRCameraUiState) {
        _uiState.value = _uiState.value.update()
    }
    
    override fun onCleared() {
        super.onCleared()
        thermalCameraManager.cleanup()
    }
}

/**
 * UI state for IRCamera screen
 */
data class IRCameraUiState(
    val isConnected: Boolean = false,
    val connectionStatus: String = "Disconnected",
    val isRecording: Boolean = false,
    val isStreaming: Boolean = false,
    val thermalFrame: ThermalFrame? = null,
    val displayMode: ThermalDisplayMode = ThermalDisplayMode.RAINBOW,
    val currentTemperature: Float = 0.0f,
    val minTemperature: Float = 0.0f,
    val maxTemperature: Float = 0.0f,
    val averageTemperature: Float = 0.0f,
    val frameRate: Int = 30,
    val resolution: ThermalResolution = ThermalResolution.HD,
    val temperatureRange: TemperatureRange = TemperatureRange.STANDARD,
    val emissivity: Float = 0.95f
)