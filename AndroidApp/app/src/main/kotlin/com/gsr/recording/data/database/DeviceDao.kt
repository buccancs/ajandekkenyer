package com.gsr.recording.data.database

import androidx.room.*
import com.gsr.recording.data.model.Device
import com.gsr.recording.data.model.DeviceType
import com.gsr.recording.data.model.ConnectionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for devices.
 * Provides database operations for device management.
 */
@Dao
interface DeviceDao {
    
    @Query("SELECT * FROM devices ORDER BY name ASC")
    fun getAllDevices(): Flow<List<Device>>
    
    @Query("SELECT * FROM devices WHERE type = :type ORDER BY name ASC")
    fun getDevicesByType(type: DeviceType): Flow<List<Device>>
    
    @Query("SELECT * FROM devices WHERE connectionStatus = :status ORDER BY lastConnected DESC")
    fun getDevicesByConnectionStatus(status: ConnectionStatus): Flow<List<Device>>
    
    @Query("SELECT * FROM devices WHERE isEnabled = 1 ORDER BY name ASC")
    fun getEnabledDevices(): Flow<List<Device>>
    
    @Query("SELECT * FROM devices WHERE deviceId = :deviceId")
    suspend fun getDeviceById(deviceId: String): Device?
    
    @Query("SELECT * FROM devices WHERE macAddress = :macAddress")
    suspend fun getDeviceByMacAddress(macAddress: String): Device?
    
    @Query("SELECT * FROM devices WHERE usbVendorId = :vendorId AND usbProductId = :productId")
    suspend fun getDeviceByUSBIds(vendorId: Int, productId: Int): Device?
    
    @Query("UPDATE devices SET connectionStatus = :status, lastConnected = :timestamp, updatedAt = :updatedAt WHERE deviceId = :deviceId")
    suspend fun updateConnectionStatus(deviceId: String, status: ConnectionStatus, timestamp: Long, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE devices SET batteryLevel = :batteryLevel, updatedAt = :updatedAt WHERE deviceId = :deviceId")
    suspend fun updateBatteryLevel(deviceId: String, batteryLevel: Int, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE devices SET isEnabled = :enabled, updatedAt = :updatedAt WHERE deviceId = :deviceId")
    suspend fun updateDeviceEnabled(deviceId: String, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE devices SET calibrationData = :calibrationData, updatedAt = :updatedAt WHERE deviceId = :deviceId")
    suspend fun updateCalibrationData(deviceId: String, calibrationData: String, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE devices SET deviceSettings = :settings, updatedAt = :updatedAt WHERE deviceId = :deviceId")
    suspend fun updateDeviceSettings(deviceId: String, settings: String, updatedAt: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM devices WHERE deviceId = :deviceId")
    suspend fun deleteDevice(deviceId: String)
    
    @Query("DELETE FROM devices WHERE lastConnected < :cutoffTime AND connectionStatus = 'DISCONNECTED'")
    suspend fun deleteOldDisconnectedDevices(cutoffTime: Long)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<Device>)
    
    @Update
    suspend fun updateDevice(device: Device)
    
    @Delete
    suspend fun deleteDevice(device: Device)
    
    /**
     * Get devices that are currently connected and streaming.
     */
    @Query("SELECT * FROM devices WHERE connectionStatus IN ('CONNECTED', 'STREAMING') ORDER BY type, name")
    suspend fun getActiveDevices(): List<Device>
    
    /**
     * Get devices with low battery for alerts.
     */
    @Query("SELECT * FROM devices WHERE batteryLevel IS NOT NULL AND batteryLevel < :threshold ORDER BY batteryLevel ASC")
    suspend fun getLowBatteryDevices(threshold: Int = 20): List<Device>
    
    /**
     * Get device counts by type for dashboard.
     */
    @Query("SELECT type, COUNT(*) as count FROM devices WHERE isEnabled = 1 GROUP BY type")
    suspend fun getDeviceCountsByType(): List<DeviceTypeCount>
    
    /**
     * Get device counts by connection status for monitoring.
     */
    @Query("SELECT connectionStatus, COUNT(*) as count FROM devices WHERE isEnabled = 1 GROUP BY connectionStatus")
    suspend fun getDeviceCountsByStatus(): List<ConnectionStatusCount>
}

/**
 * Data class for device type count query result.
 */
data class DeviceTypeCount(
    val type: DeviceType,
    val count: Int
)

/**
 * Data class for connection status count query result.
 */
data class ConnectionStatusCount(
    val connectionStatus: ConnectionStatus,
    val count: Int
)