package com.gsr.recording.usb

import android.app.Activity
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity

/**
 * Activity for handling USB device permissions and connections.
 * Launched when USB devices are attached or when permissions are needed.
 */
class UsbPermissionActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "UsbPermissionActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "UsbPermissionActivity created")
        
        handleUsbIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "UsbPermissionActivity new intent")
        
        intent?.let { handleUsbIntent(it) }
    }
    
    private fun handleUsbIntent(intent: Intent) {
        Log.d(TAG, "Handling USB intent: ${intent.action}")
        
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                handleUsbDeviceAttached(intent)
            }
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                handleUsbDeviceDetached(intent)
            }
            else -> {
                Log.w(TAG, "Unknown USB action: ${intent.action}")
            }
        }
        
        // Close the activity after handling
        finish()
    }
    
    private fun handleUsbDeviceAttached(intent: Intent) {
        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        
        if (device != null) {
            Log.d(TAG, "USB device attached: ${device.deviceName}, VID: ${device.vendorId}, PID: ${device.productId}")
            
            // Check if this is a thermal camera device
            if (isThermalCameraDevice(device)) {
                Log.d(TAG, "Thermal camera device detected")
                
                // Send broadcast to notify the app
                val broadcastIntent = Intent("com.gsr.recording.USB_THERMAL_CAMERA_ATTACHED").apply {
                    putExtra(UsbManager.EXTRA_DEVICE, device)
                }
                sendBroadcast(broadcastIntent)
                
                // Optionally launch main activity
                launchMainActivity()
            } else {
                Log.d(TAG, "Non-thermal camera USB device attached")
            }
        } else {
            Log.w(TAG, "USB device attached but device is null")
        }
    }
    
    private fun handleUsbDeviceDetached(intent: Intent) {
        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        
        if (device != null) {
            Log.d(TAG, "USB device detached: ${device.deviceName}")
            
            if (isThermalCameraDevice(device)) {
                Log.d(TAG, "Thermal camera device detached")
                
                // Send broadcast to notify the app
                val broadcastIntent = Intent("com.gsr.recording.USB_THERMAL_CAMERA_DETACHED").apply {
                    putExtra(UsbManager.EXTRA_DEVICE, device)
                }
                sendBroadcast(broadcastIntent)
            }
        }
    }
    
    private fun isThermalCameraDevice(device: UsbDevice): Boolean {
        // Check vendor and product IDs for known thermal cameras
        return when {
            // TopDon thermal camera identifiers
            device.vendorId == 0x2c77 && device.productId == 0x0001 -> true
            
            // FLIR thermal camera identifiers (if supported)
            device.vendorId == 0x09cb -> true
            
            // Add other thermal camera vendor/product IDs as needed
            else -> {
                Log.d(TAG, "Unknown USB device: VID=${device.vendorId}, PID=${device.productId}")
                false
            }
        }
    }
    
    private fun launchMainActivity() {
        try {
            val mainIntent = Intent(this, com.gsr.recording.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("usb_device_attached", true)
            }
            startActivity(mainIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch main activity: ${e.message}", e)
        }
    }
}