"""
Master Clock Synchronizer for coordinating time across multiple devices.
Implements drift compensation and quality monitoring.
"""

import logging
import time
import threading
import json
from typing import Dict, List, Optional, Any, Callable
from dataclasses import dataclass, field
from enum import Enum

from .ntp_time_server import NTPTimeServer


class DeviceType(Enum):
    """Device type enumeration."""
    ANDROID = "android"
    SHIMMER = "shimmer"
    THERMAL_CAMERA = "thermal_camera"
    RGB_CAMERA = "rgb_camera"


@dataclass
class DeviceInfo:
    """Device information for synchronization."""
    device_id: str
    device_type: DeviceType
    name: str
    address: str
    capabilities: Dict[str, Any] = field(default_factory=dict)
    
    
@dataclass
class SyncMeasurement:
    """Synchronization measurement data."""
    timestamp: float
    device_id: str
    round_trip_time: float
    clock_offset: float
    measurement_id: str
    quality_score: float = 1.0


@dataclass
class SyncResult:
    """Result of synchronization operation."""
    device_id: str
    success: bool
    offset_ms: float
    drift_rate_ppm: float
    quality_score: float
    message: str = ""


@dataclass
class QualityMetrics:
    """Synchronization quality metrics."""
    device_id: str
    last_sync_time: float
    average_offset_ms: float
    offset_stability: float
    drift_rate_ppm: float
    quality_score: float


class ClockDriftCompensator:
    """Adaptive drift compensation using predictive algorithms."""
    
    def __init__(self):
        """Initialize drift compensator."""
        self.logger = logging.getLogger(__name__)
        self.device_models: Dict[str, Dict] = {}
        self.drift_history: Dict[str, List[SyncMeasurement]] = {}
        self._lock = threading.RLock()
        
        # Simple Kalman filter state for each device
        self.kalman_states: Dict[str, Dict] = {}
    
    def add_device(self, device_id: str, device_info: DeviceInfo) -> None:
        """
        Initialize drift compensation for new device.
        
        Args:
            device_id: Device identifier
            device_info: Device information
        """
        with self._lock:
            self.device_models[device_id] = {
                'device_type': device_info.device_type,
                'oscillator_type': device_info.capabilities.get('oscillator_type', 'crystal'),
                'base_drift_rate': 0.0,
                'temperature_coefficient': 0.0
            }
            
            self.drift_history[device_id] = []
            
            # Initialize Kalman filter state
            self.kalman_states[device_id] = {
                'offset': 0.0,
                'drift_rate': 0.0,
                'offset_variance': 1.0,
                'drift_variance': 0.001,
                'process_noise': 0.001,
                'measurement_noise': 0.1
            }
            
            self.logger.info(f"Added device {device_id} to drift compensator")
    
    def calculate_compensation(self, device_id: str, measurement: SyncMeasurement) -> Dict[str, float]:
        """
        Calculate drift compensation based on current measurement.
        
        Args:
            device_id: Device identifier
            measurement: Current synchronization measurement
            
        Returns:
            dict: Compensation values
        """
        with self._lock:
            if device_id not in self.kalman_states:
                return {'offset_correction': measurement.clock_offset, 'drift_rate': 0.0}
            
            # Add measurement to history
            self.drift_history[device_id].append(measurement)
            
            # Keep only recent measurements (last 100)
            if len(self.drift_history[device_id]) > 100:
                self.drift_history[device_id] = self.drift_history[device_id][-100:]
            
            # Update Kalman filter
            state = self.kalman_states[device_id]
            
            # Prediction step
            predicted_offset = state['offset'] + state['drift_rate']
            predicted_offset_var = state['offset_variance'] + state['process_noise']
            
            # Update step
            innovation = measurement.clock_offset - predicted_offset
            innovation_var = predicted_offset_var + state['measurement_noise']
            kalman_gain = predicted_offset_var / innovation_var
            
            # Update state
            state['offset'] = predicted_offset + kalman_gain * innovation
            state['offset_variance'] = (1 - kalman_gain) * predicted_offset_var
            
            # Simple drift rate calculation from recent measurements
            if len(self.drift_history[device_id]) >= 2:
                recent = self.drift_history[device_id][-10:]  # Last 10 measurements
                if len(recent) >= 2:
                    time_diff = recent[-1].timestamp - recent[0].timestamp
                    offset_diff = recent[-1].clock_offset - recent[0].clock_offset
                    if time_diff > 0:
                        state['drift_rate'] = offset_diff / time_diff
            
            return {
                'offset_correction': state['offset'],
                'drift_rate': state['drift_rate'],
                'quality_score': min(1.0, 1.0 / (1.0 + abs(innovation)))
            }
    
    def get_drift_prediction(self, device_id: str, future_time: float) -> float:
        """
        Predict clock drift at specified future time.
        
        Args:
            device_id: Device identifier
            future_time: Future timestamp
            
        Returns:
            float: Predicted drift
        """
        with self._lock:
            if device_id not in self.kalman_states:
                return 0.0
            
            state = self.kalman_states[device_id]
            current_time = time.time()
            time_delta = future_time - current_time
            
            return state['offset'] + state['drift_rate'] * time_delta


class MasterClockSynchronizer:
    """Central synchronization coordinator and time authority."""
    
    def __init__(self, ntp_port: int = 8889):
        """
        Initialize master clock synchronizer.
        
        Args:
            ntp_port: Port for NTP server
        """
        self.logger = logging.getLogger(__name__)
        
        # Core components
        self.ntp_server = NTPTimeServer(port=ntp_port)
        self.drift_compensator = ClockDriftCompensator()
        
        # Device registry
        self.devices: Dict[str, DeviceInfo] = {}
        self.device_sync_history: Dict[str, List[SyncResult]] = {}
        
        # State
        self.running = False
        self._lock = threading.RLock()
        
        # Callbacks
        self.callbacks: Dict[str, List[Callable]] = {
            'device_registered': [],
            'sync_completed': [],
            'sync_failed': [],
            'quality_alert': []
        }
        
        self.logger.info("Master clock synchronizer initialized")
    
    def start_synchronization_service(self) -> bool:
        """
        Initialize NTP server and synchronization coordination.
        
        Returns:
            bool: True if started successfully
        """
        try:
            # Start NTP server
            if not self.ntp_server.start():
                raise Exception("Failed to start NTP server")
            
            with self._lock:
                self.running = True
            
            self.logger.info("Synchronization service started successfully")
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to start synchronization service: {e}")
            return False
    
    def stop_synchronization_service(self) -> None:
        """Stop synchronization service."""
        with self._lock:
            if not self.running:
                return
            
            self.running = False
        
        # Stop NTP server
        self.ntp_server.stop()
        
        self.logger.info("Synchronization service stopped")
    
    def register_device(self, device_info: DeviceInfo) -> bool:
        """
        Register new device for synchronization management.
        
        Args:
            device_info: Device information
            
        Returns:
            bool: True if registered successfully
        """
        try:
            with self._lock:
                self.devices[device_info.device_id] = device_info
                self.device_sync_history[device_info.device_id] = []
            
            # Initialize drift compensation
            self.drift_compensator.add_device(device_info.device_id, device_info)
            
            self.logger.info(f"Registered device: {device_info.device_id}")
            self._trigger_callback('device_registered', device_info)
            
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to register device {device_info.device_id}: {e}")
            return False
    
    def synchronize_device(self, device_id: str) -> SyncResult:
        """
        Perform clock synchronization with specified device.
        
        Args:
            device_id: Device identifier
            
        Returns:
            SyncResult: Synchronization result
        """
        if device_id not in self.devices:
            result = SyncResult(
                device_id=device_id,
                success=False,
                offset_ms=0.0,
                drift_rate_ppm=0.0,
                quality_score=0.0,
                message="Device not registered"
            )
            self._trigger_callback('sync_failed', result)
            return result
        
        try:
            # Simulate synchronization process
            # In real implementation, this would send sync commands to the device
            current_time = time.time()
            
            # Simulate round-trip measurement
            simulated_rtt = 10.0  # 10ms
            simulated_offset = 5.0  # 5ms
            
            # Create measurement
            measurement = SyncMeasurement(
                timestamp=current_time,
                device_id=device_id,
                round_trip_time=simulated_rtt,
                clock_offset=simulated_offset,
                measurement_id=f"sync_{int(current_time)}",
                quality_score=0.9
            )
            
            # Calculate compensation
            compensation = self.drift_compensator.calculate_compensation(device_id, measurement)
            
            # Create result
            result = SyncResult(
                device_id=device_id,
                success=True,
                offset_ms=compensation['offset_correction'],
                drift_rate_ppm=compensation['drift_rate'] * 1e6,
                quality_score=compensation['quality_score'],
                message="Synchronization completed"
            )
            
            # Store result
            with self._lock:
                self.device_sync_history[device_id].append(result)
                # Keep only recent results
                if len(self.device_sync_history[device_id]) > 50:
                    self.device_sync_history[device_id] = self.device_sync_history[device_id][-50:]
            
            self.logger.info(f"Synchronized device {device_id}: offset={result.offset_ms:.2f}ms")
            self._trigger_callback('sync_completed', result)
            
            return result
            
        except Exception as e:
            result = SyncResult(
                device_id=device_id,
                success=False,
                offset_ms=0.0,
                drift_rate_ppm=0.0,
                quality_score=0.0,
                message=f"Synchronization failed: {e}"
            )
            
            self.logger.error(f"Failed to synchronize device {device_id}: {e}")
            self._trigger_callback('sync_failed', result)
            
            return result
    
    def get_sync_quality(self, device_id: str) -> Optional[QualityMetrics]:
        """
        Get current synchronization quality metrics for device.
        
        Args:
            device_id: Device identifier
            
        Returns:
            QualityMetrics: Quality metrics or None if not available
        """
        with self._lock:
            if device_id not in self.device_sync_history:
                return None
            
            history = self.device_sync_history[device_id]
            if not history:
                return None
            
            # Calculate metrics from recent history
            recent = history[-10:]  # Last 10 results
            
            avg_offset = sum(r.offset_ms for r in recent) / len(recent)
            offset_variance = sum((r.offset_ms - avg_offset) ** 2 for r in recent) / len(recent)
            offset_stability = 1.0 / (1.0 + offset_variance)
            
            avg_quality = sum(r.quality_score for r in recent) / len(recent)
            avg_drift = sum(r.drift_rate_ppm for r in recent) / len(recent)
            
            return QualityMetrics(
                device_id=device_id,
                last_sync_time=recent[-1].offset_ms if recent else 0.0,
                average_offset_ms=avg_offset,
                offset_stability=offset_stability,
                drift_rate_ppm=avg_drift,
                quality_score=avg_quality
            )
    
    def get_registered_devices(self) -> List[DeviceInfo]:
        """Get list of registered devices."""
        with self._lock:
            return list(self.devices.values())
    
    def get_ntp_status(self) -> Dict[str, Any]:
        """Get NTP server status."""
        return self.ntp_server.get_status()
    
    def add_callback(self, event: str, callback: Callable) -> None:
        """Add event callback."""
        if event in self.callbacks:
            self.callbacks[event].append(callback)
    
    def _trigger_callback(self, event: str, *args) -> None:
        """Trigger event callbacks."""
        if event in self.callbacks:
            for callback in self.callbacks[event]:
                try:
                    callback(*args)
                except Exception as e:
                    self.logger.error(f"Error in callback for {event}: {e}")