package com.gsr.recording

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class for the GSR Recording System.
 * Initializes dependency injection and application-level components.
 */
@HiltAndroidApp
class GSRRecordingApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging
        android.util.Log.d(TAG, "GSR Recording Application starting...")
        
        // Any application-level initialization would go here
    }
    
    companion object {
        private const val TAG = "GSRRecordingApp"
    }
}