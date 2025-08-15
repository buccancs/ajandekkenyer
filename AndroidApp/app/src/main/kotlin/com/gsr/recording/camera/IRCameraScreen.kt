package com.gsr.recording.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * IRCamera UI Component based on @buccancs/IRCamera
 * Provides thermal camera display and controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IRCameraScreen(
    modifier: Modifier = Modifier,
    viewModel: IRCameraViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with camera status
        IRCameraHeader(
            isConnected = uiState.isConnected,
            connectionStatus = uiState.connectionStatus,
            onRefresh = { viewModel.refreshConnection() }
        )
        
        // Main thermal display
        IRCameraDisplay(
            thermalFrame = uiState.thermalFrame,
            displayMode = uiState.displayMode,
            temperatureRange = uiState.temperatureRange,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        
        // Temperature information panel
        TemperatureInfoPanel(
            currentTemperature = uiState.currentTemperature,
            minTemperature = uiState.minTemperature,
            maxTemperature = uiState.maxTemperature,
            averageTemperature = uiState.averageTemperature
        )
        
        // Camera controls
        IRCameraControls(
            isRecording = uiState.isRecording,
            isStreaming = uiState.isStreaming,
            displayMode = uiState.displayMode,
            onStartRecording = { viewModel.startRecording() },
            onStopRecording = { viewModel.stopRecording() },
            onToggleStreaming = { viewModel.toggleStreaming() },
            onDisplayModeChange = { mode -> viewModel.setDisplayMode(mode) },
            onCaptureSnapshot = { viewModel.captureSnapshot() }
        )
        
        // Settings panel
        IRCameraSettings(
            frameRate = uiState.frameRate,
            resolution = uiState.resolution,
            temperatureRange = uiState.temperatureRange,
            emissivity = uiState.emissivity,
            onFrameRateChange = { viewModel.setFrameRate(it) },
            onResolutionChange = { viewModel.setResolution(it) },
            onTemperatureRangeChange = { viewModel.setTemperatureRange(it) },
            onEmissivityChange = { viewModel.setEmissivity(it) }
        )
    }
}

@Composable
fun IRCameraHeader(
    isConnected: Boolean,
    connectionStatus: String,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "IR Thermal Camera",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = connectionStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isConnected) Color.Green else Color.Red
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Connection status indicator
                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (isConnected) Color.Green else Color.Red
                ) {}
                
                // Refresh button
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Connection"
                    )
                }
            }
        }
    }
}

@Composable
fun IRCameraDisplay(
    thermalFrame: ThermalFrame?,
    displayMode: ThermalDisplayMode,
    temperatureRange: TemperatureRange,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (thermalFrame != null) {
                // Actual thermal display
                ThermalImageView(
                    thermalFrame = thermalFrame,
                    displayMode = displayMode,
                    temperatureRange = temperatureRange,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Overlay crosshair for temperature measurement
                ThermalCrosshair(
                    modifier = Modifier.fillMaxSize()
                )
                
            } else {
                // Placeholder when no thermal data
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "No Camera",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No Thermal Data",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Connect thermal camera to start",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun ThermalImageView(
    thermalFrame: ThermalFrame,
    displayMode: ThermalDisplayMode,
    temperatureRange: TemperatureRange,
    modifier: Modifier = Modifier
) {
    // Custom thermal image renderer
    AndroidView(
        modifier = modifier,
        factory = { context ->
            // TODO: Create actual thermal image view using TopDon SDK
            // This would be a custom view that renders thermal data as a colored image
            android.widget.ImageView(context).apply {
                // Configure for thermal display
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(Color.Black.toArgb())
            }
        },
        update = { imageView ->
            // TODO: Update with actual thermal frame data
            // thermalRenderer.renderFrame(thermalFrame, displayMode, temperatureRange)
            // imageView.setImageBitmap(renderedBitmap)
        }
    )
}

@Composable
fun ThermalCrosshair(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawThermalCrosshair(this)
    }
}

private fun drawThermalCrosshair(drawScope: DrawScope) {
    val centerX = drawScope.size.width / 2
    val centerY = drawScope.size.height / 2
    val crosshairSize = 30f
    
    drawScope.drawLine(
        color = Color.White,
        start = androidx.compose.ui.geometry.Offset(centerX - crosshairSize, centerY),
        end = androidx.compose.ui.geometry.Offset(centerX + crosshairSize, centerY),
        strokeWidth = 2f
    )
    
    drawScope.drawLine(
        color = Color.White,
        start = androidx.compose.ui.geometry.Offset(centerX, centerY - crosshairSize),
        end = androidx.compose.ui.geometry.Offset(centerX, centerY + crosshairSize),
        strokeWidth = 2f
    )
}

@Composable
fun TemperatureInfoPanel(
    currentTemperature: Float,
    minTemperature: Float,
    maxTemperature: Float,
    averageTemperature: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Temperature Readings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TemperatureReading("Current", currentTemperature, Color.Blue)
                TemperatureReading("Min", minTemperature, Color.Cyan)
                TemperatureReading("Max", maxTemperature, Color.Red)
                TemperatureReading("Avg", averageTemperature, Color.Green)
            }
        }
    }
}

@Composable
fun TemperatureReading(
    label: String,
    temperature: Float,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = String.format("%.1f°C", temperature),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun IRCameraControls(
    isRecording: Boolean,
    isStreaming: Boolean,
    displayMode: ThermalDisplayMode,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onToggleStreaming: () -> Unit,
    onDisplayModeChange: (ThermalDisplayMode) -> Unit,
    onCaptureSnapshot: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Camera Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            // Recording controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = if (isRecording) onStopRecording else onStartRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color.Red else Color.Green
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isRecording) "Stop" else "Record"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRecording) "Stop Recording" else "Start Recording")
                }
                
                Button(
                    onClick = onToggleStreaming,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isStreaming) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isStreaming) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = if (isStreaming) "Stop Stream" else "Start Stream"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isStreaming) "Stop Stream" else "Start Stream")
                }
            }
            
            // Additional controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCaptureSnapshot,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "Snapshot"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Snapshot")
                }
                
                // Display mode selector
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = displayMode.name,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        label = { Text("Display Mode") },
                        modifier = Modifier.menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ThermalDisplayMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.name) },
                                onClick = {
                                    onDisplayModeChange(mode)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IRCameraSettings(
    frameRate: Int,
    resolution: ThermalResolution,
    temperatureRange: TemperatureRange,
    emissivity: Float,
    onFrameRateChange: (Int) -> Unit,
    onResolutionChange: (ThermalResolution) -> Unit,
    onTemperatureRangeChange: (TemperatureRange) -> Unit,
    onEmissivityChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Camera Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            // Frame rate setting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Frame Rate: ${frameRate} fps")
                
                Row {
                    [15, 30, 60].forEach { rate ->
                        FilterChip(
                            onClick = { onFrameRateChange(rate) },
                            label = { Text("${rate}") },
                            selected = frameRate == rate,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
            
            // Temperature range setting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Range: ${temperatureRange.min}°C to ${temperatureRange.max}°C")
                
                Row {
                    [TemperatureRange.BODY_TEMPERATURE, TemperatureRange.STANDARD, TemperatureRange.EXTENDED].forEach { range ->
                        FilterChip(
                            onClick = { onTemperatureRangeChange(range) },
                            label = { 
                                Text(when(range) {
                                    TemperatureRange.BODY_TEMPERATURE -> "Body"
                                    TemperatureRange.STANDARD -> "Std"
                                    TemperatureRange.EXTENDED -> "Ext"
                                    else -> "Custom"
                                })
                            },
                            selected = temperatureRange == range,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
            
            // Emissivity setting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Emissivity: ${String.format("%.2f", emissivity)}")
                
                Slider(
                    value = emissivity,
                    onValueChange = onEmissivityChange,
                    valueRange = 0.1f..1.0f,
                    modifier = Modifier.width(150.dp)
                )
            }
        }
    }
}

/**
 * Thermal display modes for different visualization options
 */
enum class ThermalDisplayMode {
    RAINBOW,
    IRONBOW,
    GRAYSCALE,
    HIGH_CONTRAST
}