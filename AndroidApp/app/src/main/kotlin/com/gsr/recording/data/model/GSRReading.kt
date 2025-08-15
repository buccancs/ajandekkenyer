package com.gsr.recording.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Data model representing a single GSR (Galvanic Skin Response) reading.
 * Contains sensor data with timestamp and metadata for analysis.
 */
@Entity(tableName = "gsr_readings")
data class GSRReading(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val gsrValue: Double, // μS (microsiemens)
    val rawValue: Int, // Raw ADC value from sensor
    val deviceId: String, // Shimmer device identifier
    val batteryLevel: Int? = null, // Device battery level at time of reading
    val quality: ReadingQuality = ReadingQuality.GOOD,
    val sampleRate: Int = 51, // Hz - Shimmer GSR+ default sample rate
    val createdAt: Date = Date()
)

/**
 * Quality assessment for GSR readings to help filter unreliable data.
 */
enum class ReadingQuality {
    EXCELLENT, // Strong signal, no artifacts
    GOOD,      // Usable signal with minor noise
    FAIR,      // Acceptable but may need filtering
    POOR       // Unreliable, high noise or disconnection
}