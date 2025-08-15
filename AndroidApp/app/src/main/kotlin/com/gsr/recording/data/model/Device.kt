package com.gsr.recording.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Data model representing a connected device (Shimmer GSR or thermal camera).
 * Contains device information and connection status.
 */
@Entity(tableName = "devices")
data class Device(
    @PrimaryKey
    val deviceId: String,
    val name: String,
    val type: DeviceType,
    val macAddress: String? = null, // For Bluetooth devices
    val usbVendorId: Int? = null, // For USB devices
    val usbProductId: Int? = null,
    val firmwareVersion: String? = null,
    val batteryLevel: Int? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val lastConnected: Date? = null,
    val isEnabled: Boolean = true,
    val calibrationData: String? = null, // JSON string with calibration parameters
    val deviceSettings: String? = null, // JSON string with device-specific settings
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

/**
 * Types of devices supported by the GSR recording system.
 */
enum class DeviceType {
    SHIMMER_GSR,    // Shimmer GSR+ sensor
    THERMAL_CAMERA, // TopDon thermal camera
    SMARTPHONE      // Android device itself
}

/**
 * Connection status for devices.
 */
enum class ConnectionStatus {
    DISCONNECTED,  // Not connected
    CONNECTING,    // Attempting to connect
    CONNECTED,     // Successfully connected
    STREAMING,     // Connected and actively streaming data
    ERROR,         // Connection error
    LOW_BATTERY    // Connected but battery critical
}

/**
 * Device capabilities and specifications.
 */
data class DeviceCapabilities(
    val deviceType: DeviceType,
    val sampleRates: List<Int>, // Supported sample rates in Hz
    val dataTypes: List<String>, // Types of data the device can provide
    val batteryMonitoring: Boolean,
    val wirelessConnection: Boolean,
    val calibrationRequired: Boolean,
    val maxRecordingDuration: Long? = null // Milliseconds, null if unlimited
)

/**
 * Real-time device status information.
 */
data class DeviceStatus(
    val deviceId: String,
    val connectionStatus: ConnectionStatus,
    val batteryLevel: Int?,
    val signalStrength: Int?, // 0-100 for wireless devices
    val dataRate: Float?, // Current data throughput
    val temperature: Float?, // Device temperature if available
    val lastHeartbeat: Long, // Last status update timestamp
    val errorMessage: String? = null
)