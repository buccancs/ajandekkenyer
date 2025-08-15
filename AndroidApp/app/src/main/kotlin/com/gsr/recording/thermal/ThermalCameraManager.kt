package com.gsr.recording.thermal

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for TopDon thermal camera integration via USB OTG connection.
 * Handles USB device detection, permissions, and thermal imaging operations.
 */
@Singleton
class ThermalCameraManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "ThermalCameraManager"
        private const val ACTION_USB_PERMISSION = "com.gsr.recording.USB_PERMISSION"
        
        // TopDon thermal camera USB identifiers
        private const val TOPDON_VENDOR_ID = 0x2c77  // Adjust based on actual device
        private const val TOPDON_PRODUCT_ID = 0x0001 // Adjust based on actual device
    }
    
    private lateinit var usbManager: UsbManager
    private var thermalDevice: UsbDevice? = null
    private var permissionReceiver: BroadcastReceiver? = null
    
    private val _connectionState = MutableStateFlow(ThermalConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ThermalConnectionState> = _connectionState.asStateFlow()
    
    private val _currentTemperature = MutableStateFlow(0.0f)
    val currentTemperature: StateFlow<Float> = _currentTemperature.asStateFlow()
    
    private val _frameRate = MutableStateFlow(30)
    val frameRate: StateFlow<Int> = _frameRate.asStateFlow()
    
    init {
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        setupUsbPermissionReceiver()
    }
    
    /**
     * Initialize thermal camera connection
     */
    fun initializeThermalCamera(): Boolean {
        return try {
            Log.d(TAG, "Initializing thermal camera connection")
            
            // Setup USB connection
            val usbConnection = setupUSBConnection()
            if (!usbConnection) {
                Log.e(TAG, "Failed to setup USB connection")
                return false
            }
            
            // Initialize TopDon device
            initializeTopdonDevice()
            
            // Configure camera settings
            configureCameraSettings()
            
            // Start thermal imaging
            startThermalImaging()
            
            _connectionState.value = ThermalConnectionState.CONNECTED
            Log.d(TAG, "Thermal camera initialized successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize thermal camera: ${e.message}", e)
            _connectionState.value = ThermalConnectionState.ERROR
            false
        }
    }
    
    /**
     * Setup USB OTG connection with thermal camera
     */
    fun setupUSBConnection(): Boolean {
        Log.d(TAG, "Setting up USB connection")
        
        // Find TopDon thermal camera
        val deviceList = usbManager.deviceList
        thermalDevice = deviceList.values.find { device ->
            device.vendorId == TOPDON_VENDOR_ID && 
            device.productId == TOPDON_PRODUCT_ID
        }
        
        return if (thermalDevice != null) {
            Log.d(TAG, "TopDon thermal camera found: ${thermalDevice?.deviceName}")
            requestUSBPermission()
        } else {
            Log.e(TAG, "TopDon thermal camera not found")
            scanForAvailableDevices()
            false
        }
    }
    
    /**
     * Request USB permission for thermal camera
     */
    private fun requestUSBPermission(): Boolean {
        val permissionIntent = PendingIntent.getBroadcast(
            context, 0, 
            Intent(ACTION_USB_PERMISSION), 
            PendingIntent.FLAG_MUTABLE
        )
        
        thermalDevice?.let { device ->
            if (usbManager.hasPermission(device)) {
                Log.d(TAG, "USB permission already granted")
                return true
            } else {
                Log.d(TAG, "Requesting USB permission")
                usbManager.requestPermission(device, permissionIntent)
                return true
            }
        }
        return false
    }
    
    /**
     * Setup USB permission receiver
     */
    private fun setupUsbPermissionReceiver() {
        permissionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (ACTION_USB_PERMISSION == intent?.action) {
                    synchronized(this) {
                        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.let {
                                Log.d(TAG, "USB permission granted for ${it.deviceName}")
                                onUsbPermissionGranted(it)
                            }
                        } else {
                            Log.e(TAG, "USB permission denied for ${device?.deviceName}")
                            _connectionState.value = ThermalConnectionState.PERMISSION_DENIED
                        }
                    }
                }
            }
        }
        
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        context.registerReceiver(permissionReceiver, filter)
    }
    
    /**
     * Handle USB permission granted
     */
    private fun onUsbPermissionGranted(device: UsbDevice) {
        try {
            val connection = usbManager.openDevice(device)
            if (connection != null) {
                Log.d(TAG, "USB device connection established")
                _connectionState.value = ThermalConnectionState.USB_CONNECTED
                // Continue with thermal camera initialization
                initializeTopdonDevice()
            } else {
                Log.e(TAG, "Failed to open USB device connection")
                _connectionState.value = ThermalConnectionState.ERROR
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening USB device: ${e.message}", e)
            _connectionState.value = ThermalConnectionState.ERROR
        }
    }
    
    /**
     * Initialize TopDon device using SDK
     */
    private fun initializeTopdonDevice() {
        try {
            // TODO: Initialize actual TopDon SDK
            // topdonDevice.initialize(usbConnection.device)
            Log.d(TAG, "TopDon device initialized (mock)")
            _connectionState.value = ThermalConnectionState.INITIALIZING
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TopDon device: ${e.message}", e)
            _connectionState.value = ThermalConnectionState.ERROR
        }
    }
    
    /**
     * Configure camera settings
     */
    private fun configureCameraSettings() {
        try {
            // TODO: Configure actual camera settings
            // topdonDevice.setEmissivity(0.95f)
            // topdonDevice.setTemperatureRange(-40f, 120f)
            Log.d(TAG, "Camera settings configured (mock)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure camera settings: ${e.message}", e)
        }
    }
    
    /**
     * Start thermal imaging
     */
    private fun startThermalImaging(): Boolean {
        return try {
            // TODO: Start actual thermal imaging
            // topdonDevice.startThermalImaging()
            Log.d(TAG, "Thermal imaging started (mock)")
            _connectionState.value = ThermalConnectionState.IMAGING
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start thermal imaging: ${e.message}", e)
            _connectionState.value = ThermalConnectionState.ERROR
            false
        }
    }
    
    /**
     * Start thermal recording with configuration
     */
    fun startThermalRecording(config: ThermalConfig): Boolean {
        return try {
            Log.d(TAG, "Starting thermal recording with config: $config")
            
            // Configure recording parameters
            setFrameRate(config.frameRate)
            setResolution(config.resolution)
            setTemperatureRange(config.temperatureRange)
            
            // Start recording with synchronization
            // synchronizer.startSynchronizedRecording(config.syncTimestamp)
            
            // Begin data capture
            startDataCapture()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start thermal recording: ${e.message}", e)
            false
        }
    }
    
    /**
     * Stop thermal recording
     */
    fun stopThermalRecording(): Boolean {
        return try {
            Log.d(TAG, "Stopping thermal recording")
            // TODO: Stop actual recording
            // topdonDevice.stopCapture()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop thermal recording: ${e.message}", e)
            false
        }
    }
    
    /**
     * Set frame rate
     */
    fun setFrameRate(frameRate: Int) {
        try {
            // TODO: Set actual frame rate
            // topdonDevice.setFrameRate(frameRate)
            _frameRate.value = frameRate
            Log.d(TAG, "Frame rate set to $frameRate")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set frame rate: ${e.message}", e)
        }
    }
    
    /**
     * Set resolution
     */
    fun setResolution(resolution: ThermalResolution) {
        try {
            // TODO: Set actual resolution
            // topdonDevice.setResolution(resolution.width, resolution.height)
            Log.d(TAG, "Resolution set to ${resolution.width}x${resolution.height}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set resolution: ${e.message}", e)
        }
    }
    
    /**
     * Set temperature range
     */
    fun setTemperatureRange(range: TemperatureRange) {
        try {
            // TODO: Set actual temperature range
            // topdonDevice.setTemperatureRange(range.min, range.max)
            Log.d(TAG, "Temperature range set to ${range.min}°C - ${range.max}°C")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set temperature range: ${e.message}", e)
        }
    }
    
    /**
     * Start data capture
     */
    private fun startDataCapture() {
        try {
            // TODO: Start actual data capture with callback
            // topdonDevice.startCapture { thermalFrame ->
            //     processThermalFrame(thermalFrame)
            // }
            Log.d(TAG, "Data capture started (mock)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start data capture: ${e.message}", e)
        }
    }
    
    /**
     * Process thermal frame
     */
    private fun processThermalFrame(frame: ThermalFrame) {
        try {
            // Apply calibration
            // val calibratedFrame = thermalProcessor.applyCalibration(frame)
            
            // Synchronize timestamp
            // val syncedFrame = synchronizer.synchronizeFrame(calibratedFrame)
            
            // Store frame data
            // thermalProcessor.storeFrame(syncedFrame)
            
            // Update real-time display
            // updateThermalDisplay(syncedFrame)
            
            // Update current temperature
            _currentTemperature.value = frame.averageTemperature
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing thermal frame: ${e.message}", e)
        }
    }
    
    /**
     * Scan for available USB devices
     */
    private fun scanForAvailableDevices() {
        val deviceList = usbManager.deviceList
        Log.d(TAG, "Available USB devices:")
        deviceList.values.forEach { device ->
            Log.d(TAG, "Device: ${device.deviceName}, VID: ${device.vendorId}, PID: ${device.productId}")
        }
    }
    
    /**
     * Get current connection status
     */
    fun isConnected(): Boolean {
        return _connectionState.value == ThermalConnectionState.CONNECTED ||
               _connectionState.value == ThermalConnectionState.IMAGING
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            permissionReceiver?.let { receiver ->
                context.unregisterReceiver(receiver)
                permissionReceiver = null
            }
            
            // TODO: Cleanup TopDon device
            // topdonDevice.disconnect()
            
            _connectionState.value = ThermalConnectionState.DISCONNECTED
            Log.d(TAG, "Thermal camera manager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}", e)
        }
    }
}

/**
 * Thermal camera connection states
 */
enum class ThermalConnectionState {
    DISCONNECTED,
    SCANNING,
    USB_CONNECTED,
    PERMISSION_DENIED,
    INITIALIZING,
    CONNECTED,
    IMAGING,
    ERROR
}

/**
 * Thermal recording configuration
 */
data class ThermalConfig(
    val frameRate: Int = 30,
    val resolution: ThermalResolution = ThermalResolution.HD,
    val temperatureRange: TemperatureRange = TemperatureRange.STANDARD,
    val syncTimestamp: Long = System.currentTimeMillis()
)

/**
 * Thermal camera resolution options
 */
enum class ThermalResolution(val width: Int, val height: Int) {
    VGA(640, 480),
    HD(1280, 720),
    FULL_HD(1920, 1080)
}

/**
 * Temperature range configuration
 */
data class TemperatureRange(
    val min: Float,
    val max: Float
) {
    companion object {
        val STANDARD = TemperatureRange(-20f, 60f)
        val EXTENDED = TemperatureRange(-40f, 120f)
        val BODY_TEMPERATURE = TemperatureRange(30f, 45f)
    }
}

/**
 * Thermal frame data
 */
data class ThermalFrame(
    val timestamp: Long,
    val width: Int,
    val height: Int,
    val temperatureData: Array<FloatArray>,
    val averageTemperature: Float,
    val maxTemperature: Float,
    val minTemperature: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ThermalFrame

        if (timestamp != other.timestamp) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (!temperatureData.contentDeepEquals(other.temperatureData)) return false
        if (averageTemperature != other.averageTemperature) return false
        if (maxTemperature != other.maxTemperature) return false
        if (minTemperature != other.minTemperature) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + temperatureData.contentDeepHashCode()
        result = 31 * result + averageTemperature.hashCode()
        result = 31 * result + maxTemperature.hashCode()
        result = 31 * result + minTemperature.hashCode()
        return result
    }
}