package com.gsr.recording.device

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.gsr.recording.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Device manager for TopDon thermal cameras via USB OTG.
 * Handles device discovery, connection, and thermal frame capture.
 */
@Singleton
class ThermalCameraManager @Inject constructor(
    private val context: Context
) {
    
    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var connectedCamera: UsbDevice? = null
    
    private val _deviceStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val deviceStatus: StateFlow<ConnectionStatus> = _deviceStatus.asStateFlow()
    
    private val _thermalFrame = MutableStateFlow<ThermalFrame?>(null)
    val thermalFrame: StateFlow<ThermalFrame?> = _thermalFrame.asStateFlow()
    
    private val _cameraTemperature = MutableStateFlow(0f)
    val cameraTemperature: StateFlow<Float> = _cameraTemperature.asStateFlow()
    
    companion object {
        private const val TAG = "ThermalCameraManager"
        
        // TopDon thermal camera USB identifiers (example values)
        private const val TOPDON_VENDOR_ID = 0x1234
        private const val TOPDON_PRODUCT_ID = 0x5678
        
        // Alternative vendor/product IDs for different TopDon models
        private val SUPPORTED_DEVICE_IDS = mapOf(
            0x1234 to listOf(0x5678, 0x5679, 0x567A), // TopDon TC series
            0x2345 to listOf(0x6789, 0x678A, 0x678B)  // Alternative series
        )
    }
    
    /**
     * Scan for available thermal cameras.
     */
    suspend fun scanForDevices(): List<Device> {
        val usbDevices = usbManager.deviceList
        val thermalCameras = mutableListOf<Device>()
        
        usbDevices.values.forEach { usbDevice ->
            if (isThermalCamera(usbDevice)) {
                val device = Device(
                    deviceId = usbDevice.deviceName,
                    name = "${usbDevice.manufacturerName} ${usbDevice.productName}",
                    type = DeviceType.THERMAL_CAMERA,
                    macAddress = usbDevice.deviceName, // USB device name as identifier
                    connectionStatus = ConnectionStatus.DISCONNECTED
                )
                thermalCameras.add(device)
            }
        }
        
        Log.d(TAG, "Found ${thermalCameras.size} thermal cameras")
        return thermalCameras
    }
    
    /**
     * Connect to a thermal camera device.
     */
    suspend fun connectDevice(device: Device): Result<Unit> {
        return try {
            _deviceStatus.value = ConnectionStatus.CONNECTING
            
            val usbDevice = usbManager.deviceList[device.macAddress]
            if (usbDevice == null) {
                throw Exception("USB device not found: ${device.macAddress}")
            }
            
            // Check USB permissions
            if (!usbManager.hasPermission(usbDevice)) {
                throw Exception("USB permission not granted for device")
            }
            
            // Initialize camera connection
            initializeCameraConnection(usbDevice)
            connectedCamera = usbDevice
            
            _deviceStatus.value = ConnectionStatus.CONNECTED
            Log.d(TAG, "Connected to thermal camera: ${device.name}")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to thermal camera", e)
            _deviceStatus.value = ConnectionStatus.ERROR
            Result.failure(e)
        }
    }
    
    /**
     * Disconnect from current thermal camera.
     */
    suspend fun disconnectDevice(): Result<Unit> {
        return try {
            connectedCamera?.let { camera ->
                // Close camera connection
                closeCameraConnection(camera)
            }
            
            connectedCamera = null
            _deviceStatus.value = ConnectionStatus.DISCONNECTED
            
            Log.d(TAG, "Disconnected from thermal camera")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from thermal camera", e)
            Result.failure(e)
        }
    }
    
    /**
     * Start thermal frame capture.
     */
    suspend fun startFrameCapture(sessionId: String, frameRate: Int = 10): Result<Unit> {
        if (connectedCamera == null || _deviceStatus.value != ConnectionStatus.CONNECTED) {
            return Result.failure(Exception("Camera not connected"))
        }
        
        return try {
            // Configure frame capture settings
            configureCameraSettings(frameRate)
            
            _deviceStatus.value = ConnectionStatus.STREAMING
            Log.d(TAG, "Started thermal frame capture at ${frameRate}fps")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start frame capture", e)
            Result.failure(e)
        }
    }
    
    /**
     * Stop thermal frame capture.
     */
    suspend fun stopFrameCapture(): Result<Unit> {
        return try {
            // Send stop command to camera
            stopCameraCapture()
            
            _deviceStatus.value = ConnectionStatus.CONNECTED
            Log.d(TAG, "Stopped thermal frame capture")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop frame capture", e)
            Result.failure(e)
        }
    }
    
    /**
     * Calibrate thermal camera.
     */
    suspend fun calibrateCamera(): Result<Unit> {
        if (connectedCamera == null || _deviceStatus.value != ConnectionStatus.CONNECTED) {
            return Result.failure(Exception("Camera not connected"))
        }
        
        return try {
            Log.d(TAG, "Starting thermal camera calibration")
            
            // Send calibration command to camera
            // Implementation would send specific calibration commands
            performCameraCalibration()
            
            Log.d(TAG, "Thermal camera calibration completed")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Camera calibration failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Capture a single thermal frame.
     */
    suspend fun captureFrame(): Result<ThermalFrame> {
        if (connectedCamera == null || _deviceStatus.value == ConnectionStatus.DISCONNECTED) {
            return Result.failure(Exception("Camera not connected"))
        }
        
        return try {
            // Capture thermal frame from camera
            val frameData = captureThermalFrameData()
            
            val thermalFrame = ThermalFrame(
                frameId = "frame_${System.currentTimeMillis()}",
                sessionId = "", // Will be set by calling service
                timestamp = System.currentTimeMillis(),
                frameNumber = generateFrameNumber(),
                averageTemperature = frameData.averageTemp,
                maxTemperature = frameData.maxTemp,
                minTemperature = frameData.minTemp,
                deviceId = connectedCamera?.deviceName ?: "",
                dataFilePath = frameData.filePath
            )
            
            _thermalFrame.value = thermalFrame
            Log.v(TAG, "Captured thermal frame: avg=${frameData.averageTemp}°C")
            Result.success(thermalFrame)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture thermal frame", e)
            Result.failure(e)
        }
    }
    
    private fun isThermalCamera(usbDevice: UsbDevice): Boolean {
        val vendorId = usbDevice.vendorId
        val productId = usbDevice.productId
        
        return SUPPORTED_DEVICE_IDS[vendorId]?.contains(productId) == true ||
               usbDevice.productName?.contains("Thermal", ignoreCase = true) == true ||
               usbDevice.manufacturerName?.contains("TopDon", ignoreCase = true) == true
    }
    
    private fun initializeCameraConnection(usbDevice: UsbDevice) {
        // Initialize USB connection and camera communication
        // This would involve:
        // 1. Opening USB connection
        // 2. Setting up communication protocol
        // 3. Initializing camera settings
        // 4. Verifying camera response
        
        Log.d(TAG, "Initializing camera connection for device: ${usbDevice.deviceName}")
        
        // Implementation would use Android USB Host API:
        // val connection = usbManager.openDevice(usbDevice)
        // val interface = usbDevice.getInterface(0)
        // connection.claimInterface(interface, true)
        
        // For simulation, we'll just log the initialization
        simulateCameraInitialization(usbDevice)
    }
    
    private fun closeCameraConnection(usbDevice: UsbDevice) {
        // Close USB connection and cleanup resources
        Log.d(TAG, "Closing camera connection for device: ${usbDevice.deviceName}")
        
        // Implementation would:
        // 1. Stop any ongoing capture
        // 2. Release USB interface
        // 3. Close USB connection
        // 4. Cleanup resources
        
        simulateCameraCleanup(usbDevice)
    }
    
    private fun configureCameraSettings(frameRate: Int) {
        // Configure camera capture settings
        // This would send configuration commands to the camera
        Log.d(TAG, "Configuring camera settings: frameRate=${frameRate}fps")
        
        // Implementation would configure:
        // - Frame rate
        // - Resolution
        // - Temperature range
        // - Image format
        // - Emissivity settings
    }
    
    private fun stopCameraCapture() {
        // Send stop capture command to camera
        Log.d(TAG, "Stopping camera capture")
    }
    
    private fun performCameraCalibration() {
        // Perform thermal camera calibration
        // This would involve:
        // 1. Shutter calibration
        // 2. Temperature reference calibration
        // 3. Sensor calibration
        
        Log.d(TAG, "Performing camera calibration")
        
        // Simulate calibration delay
        Thread.sleep(2000)
    }
    
    private fun captureThermalFrameData(): ThermalFrameData {
        // Capture actual thermal frame data from camera
        // This would:
        // 1. Request frame from camera
        // 2. Read thermal data
        // 3. Process temperature matrix
        // 4. Calculate statistics
        // 5. Save frame data to file
        
        // For demonstration, generate simulated thermal data
        return generateSimulatedThermalFrame()
    }
    
    private fun generateFrameNumber(): Int {
        // Generate sequential frame number
        return (System.currentTimeMillis() % 1000000).toInt()
    }
    
    // Simulation functions for development/testing
    private fun simulateCameraInitialization(usbDevice: UsbDevice) {
        // Simulate camera initialization process
        Log.d(TAG, "Simulating camera initialization for ${usbDevice.productName}")
        
        // Simulate initialization delay
        Thread.sleep(1000)
        
        _cameraTemperature.value = 25.0f + (Math.random() * 10).toFloat()
    }
    
    private fun simulateCameraCleanup(usbDevice: UsbDevice) {
        // Simulate camera cleanup process
        Log.d(TAG, "Simulating camera cleanup for ${usbDevice.productName}")
        
        _cameraTemperature.value = 0f
    }
    
    private fun generateSimulatedThermalFrame(): ThermalFrameData {
        // Generate simulated thermal frame data for testing
        val baseTemp = 20.0f + (Math.random() * 15).toFloat()
        val variation = 5.0f
        
        return ThermalFrameData(
            averageTemp = baseTemp + (Math.random() * variation).toFloat(),
            maxTemp = baseTemp + variation + (Math.random() * 5).toFloat(),
            minTemp = baseTemp - variation + (Math.random() * 5).toFloat(),
            filePath = "/android_asset/thermal_frames/frame_${System.currentTimeMillis()}.dat"
        )
    }
}

/**
 * Data class for thermal frame capture data.
 */
data class ThermalFrameData(
    val averageTemp: Float,
    val maxTemp: Float,
    val minTemp: Float,
    val filePath: String
)