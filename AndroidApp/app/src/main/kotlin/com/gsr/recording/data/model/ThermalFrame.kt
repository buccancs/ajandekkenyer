package com.gsr.recording.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Data model representing a thermal camera frame capture.
 * Contains thermal imaging data with temperature measurements.
 */
@Entity(tableName = "thermal_frames")
data class ThermalFrame(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val frameNumber: Int,
    val frameDataPath: String, // Path to stored thermal frame file
    val averageTemperature: Float, // Celsius
    val maxTemperature: Float,
    val minTemperature: Float,
    val deviceId: String, // TopDon thermal camera identifier
    val frameRate: Float = 9.0f, // Hz - TopDon TC001 spec
    val resolution: String = "32x32", // Thermal sensor resolution
    val createdAt: Date = Date()
)

/**
 * Temperature measurement point within a thermal frame.
 * Used for region-of-interest analysis and face temperature tracking.
 */
data class TemperaturePoint(
    val x: Int, // Pixel coordinate
    val y: Int, // Pixel coordinate
    val temperature: Float, // Celsius
    val confidence: Float // Measurement confidence (0.0 - 1.0)
)

/**
 * Processed thermal analysis results for a frame.
 * Contains derived measurements useful for stress assessment.
 */
data class ThermalAnalysis(
    val frameId: String,
    val faceRegionTemperature: Float?, // Average face temperature if detected
    val foreheadTemperature: Float?, // Forehead region temperature
    val noseTemperature: Float?, // Nose tip temperature
    val temperatureVariance: Float, // Variance across frame
    val hotSpots: List<TemperaturePoint>, // Regions above threshold
    val analysisTimestamp: Long = System.currentTimeMillis()
)