package com.gsr.recording.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Main screen of the GSR Recording application.
 * Displays device status, connection controls, and recording management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GSR Recording System",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Mobile Data Collection Unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        
        // Connection Status
        ConnectionStatusCard(
            isConnected = uiState.isConnectedToPC,
            serverAddress = uiState.serverAddress,
            onConnect = { viewModel.connectToPC() },
            onDisconnect = { viewModel.disconnectFromPC() }
        )
        
        // Recording Controls
        RecordingControlCard(
            isRecording = uiState.isRecording,
            sessionId = uiState.currentSessionId,
            onStartRecording = { viewModel.startRecording() },
            onStopRecording = { viewModel.stopRecording() }
        )
        
        // Device Status
        DeviceStatusCard(
            shimmerConnected = uiState.shimmerConnected,
            thermalCameraConnected = uiState.thermalCameraConnected,
            batteryLevel = uiState.batteryLevel
        )
        
        // Recent Sessions (if any)
        if (uiState.recentSessions.isNotEmpty()) {
            RecentSessionsCard(sessions = uiState.recentSessions)
        }
    }
}

@Composable
fun ConnectionStatusCard(
    isConnected: Boolean,
    serverAddress: String?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PC Connection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isConnected) "Connected to $serverAddress" else "Disconnected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isConnected) Color.Green else Color.Red
                    )
                }
                
                Button(
                    onClick = if (isConnected) onDisconnect else onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isConnected) "Disconnect" else "Connect")
                }
            }
        }
    }
}

@Composable
fun RecordingControlCard(
    isRecording: Boolean,
    sessionId: String?,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recording Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isRecording) "Recording: $sessionId" else "Ready to record",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isRecording) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Button(
                    onClick = if (isRecording) onStopRecording else onStartRecording,
                    enabled = sessionId != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) 
                            MaterialTheme.colorScheme.error 
                        else 
                            Color.Green
                    )
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isRecording) "Stop" else "Start"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRecording) "Stop" else "Start")
                }
            }
        }
    }
}

@Composable
fun DeviceStatusCard(
    shimmerConnected: Boolean,
    thermalCameraConnected: Boolean,
    batteryLevel: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Device Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusIndicator("Shimmer GSR", shimmerConnected)
                StatusIndicator("Thermal Camera", thermalCameraConnected)
                StatusIndicator("Battery", batteryLevel > 20, "$batteryLevel%")
            }
        }
    }
}

@Composable
fun StatusIndicator(
    label: String,
    isConnected: Boolean,
    extra: String = ""
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = if (isConnected) Color.Green else Color.Red
            ) {}
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
        if (extra.isNotEmpty()) {
            Text(
                text = extra,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun RecentSessionsCard(sessions: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recent Sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.heightIn(max = 120.dp)
            ) {
                items(sessions.take(3)) { session ->
                    Text(
                        text = session,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}