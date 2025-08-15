package com.gsr.recording.device

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.gsr.recording.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing Shimmer GSR device connections via Bluetooth.
 * Handles device discovery, connection, data streaming, and sensor configuration.
 */
@Singleton
class ShimmerDeviceManager @Inject constructor(
    private val context: Context
) {
    
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectedDevice: BluetoothDevice? = null
    
    private val _deviceStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val deviceStatus: StateFlow<ConnectionStatus> = _deviceStatus.asStateFlow()
    
    private val _gsrData = MutableStateFlow<GSRReading?>(null)
    val gsrData: StateFlow<GSRReading?> = _gsrData.asStateFlow()
    
    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()
    
    companion object {
        private const val TAG = "ShimmerDeviceManager"
        
        // Shimmer-specific UUIDs (example - would use actual Shimmer UUIDs)
        private const val SHIMMER_SERVICE_UUID = "0000FFF0-0000-1000-8000-00805F9B34FB"
        private const val GSR_CHARACTERISTIC_UUID = "0000FFF1-0000-1000-8000-00805F9B34FB"
        private const val BATTERY_CHARACTERISTIC_UUID = "0000FFF2-0000-1000-8000-00805F9B34FB"
        private const val CONFIG_CHARACTERISTIC_UUID = "0000FFF3-0000-1000-8000-00805F9B34FB"
    }
    
    /**
     * Scan for available Shimmer devices.
     */
    suspend fun scanForDevices(): List<Device> {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w(TAG, "Bluetooth adapter not available or disabled")
            return emptyList()
        }
        
        val pairedDevices = bluetoothAdapter.bondedDevices
        val shimmerDevices = mutableListOf<Device>()
        
        pairedDevices.forEach { bluetoothDevice ->
            if (isShimmerDevice(bluetoothDevice)) {
                val device = Device(
                    deviceId = bluetoothDevice.address,
                    name = bluetoothDevice.name ?: "Unknown Shimmer",
                    type = DeviceType.SHIMMER_GSR,
                    macAddress = bluetoothDevice.address,
                    connectionStatus = ConnectionStatus.DISCONNECTED
                )
                shimmerDevices.add(device)
            }
        }
        
        Log.d(TAG, "Found ${shimmerDevices.size} Shimmer devices")
        return shimmerDevices
    }
    
    /**
     * Connect to a Shimmer device.
     */
    suspend fun connectDevice(device: Device): Result<Unit> {
        if (bluetoothAdapter == null) {
            return Result.failure(Exception("Bluetooth not available"))
        }
        
        return try {
            _deviceStatus.value = ConnectionStatus.CONNECTING
            
            val bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.macAddress)
            connectedDevice = bluetoothDevice
            
            // Connect to GATT server
            bluetoothGatt = bluetoothDevice.connectGatt(
                context,
                false,
                gattCallback
            )
            
            Log.d(TAG, "Connecting to Shimmer device: ${device.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Shimmer device", e)
            _deviceStatus.value = ConnectionStatus.ERROR
            Result.failure(e)
        }
    }
    
    /**
     * Disconnect from current device.
     */
    suspend fun disconnectDevice(): Result<Unit> {
        return try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            connectedDevice = null
            _deviceStatus.value = ConnectionStatus.DISCONNECTED
            
            Log.d(TAG, "Disconnected from Shimmer device")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from Shimmer device", e)
            Result.failure(e)
        }
    }
    
    /**
     * Start GSR data streaming.
     */
    suspend fun startDataStreaming(sessionId: String, sampleRate: Int = 51): Result<Unit> {
        if (bluetoothGatt == null || _deviceStatus.value != ConnectionStatus.CONNECTED) {
            return Result.failure(Exception("Device not connected"))
        }
        
        return try {
            // Configure sample rate and enable GSR sensor
            configureGSRSensor(sampleRate)
            
            _deviceStatus.value = ConnectionStatus.STREAMING
            Log.d(TAG, "Started GSR data streaming at ${sampleRate}Hz")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start data streaming", e)
            Result.failure(e)
        }
    }
    
    /**
     * Stop data streaming.
     */
    suspend fun stopDataStreaming(): Result<Unit> {
        return try {
            // Send stop command to device
            stopGSRSensor()
            
            _deviceStatus.value = ConnectionStatus.CONNECTED
            Log.d(TAG, "Stopped GSR data streaming")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop data streaming", e)
            Result.failure(e)
        }
    }
    
    /**
     * Calibrate GSR sensor.
     */
    suspend fun calibrateGSRSensor(): Result<Unit> {
        if (bluetoothGatt == null || _deviceStatus.value != ConnectionStatus.CONNECTED) {
            return Result.failure(Exception("Device not connected"))
        }
        
        return try {
            // Send calibration command
            Log.d(TAG, "Starting GSR sensor calibration")
            
            // Implementation would send calibration commands to device
            // For now, simulate calibration process
            kotlinx.coroutines.delay(3000)
            
            Log.d(TAG, "GSR sensor calibration completed")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "GSR calibration failed", e)
            Result.failure(e)
        }
    }
    
    // GATT callback for handling Bluetooth LE communication
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server")
                    _deviceStatus.value = ConnectionStatus.CONNECTED
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server")
                    _deviceStatus.value = ConnectionStatus.DISCONNECTED
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")
                enableGSRNotifications()
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }
        
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.let { char ->
                when (char.uuid.toString().uppercase()) {
                    GSR_CHARACTERISTIC_UUID -> handleGSRData(char.value)
                    BATTERY_CHARACTERISTIC_UUID -> handleBatteryData(char.value)
                }
            }
        }
    }
    
    private fun isShimmerDevice(bluetoothDevice: BluetoothDevice): Boolean {
        // Check if device name contains "Shimmer" or matches known Shimmer patterns
        val deviceName = bluetoothDevice.name?.uppercase() ?: ""
        return deviceName.contains("SHIMMER") || 
               deviceName.contains("GSR") ||
               bluetoothDevice.address.startsWith("00:06:66") // Shimmer MAC prefix
    }
    
    private fun configureGSRSensor(sampleRate: Int) {
        // Send configuration commands to Shimmer device
        // Implementation would configure:
        // - Sample rate
        // - GSR sensor range
        // - Data packet format
        Log.d(TAG, "Configuring GSR sensor with sample rate: ${sampleRate}Hz")
    }
    
    private fun stopGSRSensor() {
        // Send stop streaming command to device
        Log.d(TAG, "Stopping GSR sensor")
    }
    
    private fun enableGSRNotifications() {
        // Enable notifications for GSR and battery characteristics
        Log.d(TAG, "Enabling GSR data notifications")
    }
    
    private fun handleGSRData(data: ByteArray) {
        try {
            // Parse GSR data from raw bytes
            // This would depend on Shimmer data packet format
            val gsrValue = parseGSRValue(data)
            val rawValue = parseRawValue(data)
            
            val reading = GSRReading(
                id = "gsr_${System.currentTimeMillis()}",
                sessionId = "", // Would be set by calling service
                timestamp = System.currentTimeMillis(),
                gsrValue = gsrValue,
                rawValue = rawValue,
                deviceId = connectedDevice?.address ?: "",
                batteryLevel = _batteryLevel.value.takeIf { it > 0 },
                quality = assessDataQuality(gsrValue)
            )
            
            _gsrData.value = reading
            Log.v(TAG, "GSR reading: ${gsrValue}μS")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GSR data", e)
        }
    }
    
    private fun handleBatteryData(data: ByteArray) {
        try {
            val batteryLevel = data[0].toInt() and 0xFF
            _batteryLevel.value = batteryLevel
            Log.v(TAG, "Battery level: $batteryLevel%")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing battery data", e)
        }
    }
    
    private fun parseGSRValue(data: ByteArray): Double {
        // Parse GSR value from Shimmer data packet
        // This is a simplified example - actual implementation would follow
        // Shimmer data format specification
        if (data.size >= 4) {
            val rawValue = ((data[1].toInt() and 0xFF) shl 8) or (data[0].toInt() and 0xFF)
            return convertRawToGSR(rawValue)
        }
        return 0.0
    }
    
    private fun parseRawValue(data: ByteArray): Int {
        if (data.size >= 2) {
            return ((data[1].toInt() and 0xFF) shl 8) or (data[0].toInt() and 0xFF)
        }
        return 0
    }
    
    private fun convertRawToGSR(rawValue: Int): Double {
        // Convert raw ADC value to GSR in microsiemens
        // This would use calibration parameters specific to the device
        val voltage = (rawValue / 4095.0) * 3.0 // Assuming 12-bit ADC, 3V reference
        return 1.0 / voltage // Simplified conversion
    }
    
    private fun assessDataQuality(gsrValue: Double): ReadingQuality {
        return when {
            gsrValue < 0.1 || gsrValue > 100.0 -> ReadingQuality.POOR
            gsrValue < 0.5 || gsrValue > 50.0 -> ReadingQuality.FAIR
            gsrValue < 1.0 || gsrValue > 25.0 -> ReadingQuality.GOOD
            else -> ReadingQuality.EXCELLENT
        }
    }
}