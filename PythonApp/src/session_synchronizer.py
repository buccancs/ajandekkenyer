"""
Session Synchronizer for coordinated recording sessions.
Manages synchronized start/stop across multiple devices.
"""

import logging
import time
import threading
import json
from typing import Dict, List, Optional, Any, Callable
from dataclasses import dataclass, field
from enum import Enum

from .master_clock_synchronizer import MasterClockSynchronizer, DeviceInfo
from .lsl_synchronizer import LSLSynchronizer, StreamInfo, StreamType


class SessionState(Enum):
    """Recording session states."""
    IDLE = "idle"
    PREPARING = "preparing"
    SYNCHRONIZED = "synchronized"
    RECORDING = "recording"
    STOPPING = "stopping"
    COMPLETED = "completed"
    ERROR = "error"


@dataclass
class SessionConfig:
    """Session configuration."""
    session_id: str
    duration_seconds: Optional[float] = None
    sync_precision_ms: float = 5.0
    auto_start_delay_ms: float = 1000.0
    quality_threshold: float = 0.8
    retry_attempts: int = 3


@dataclass
class DeviceSession:
    """Device session state."""
    device_id: str
    device_info: DeviceInfo
    state: SessionState = SessionState.IDLE
    sync_quality: float = 0.0
    last_sync_time: float = 0.0
    stream_outlets: List[str] = field(default_factory=list)
    error_message: str = ""


class SessionSynchronizer:
    """Coordinates synchronized recording sessions across multiple devices."""
    
    def __init__(self, master_sync: MasterClockSynchronizer, lsl_sync: LSLSynchronizer):
        """
        Initialize session synchronizer.
        
        Args:
            master_sync: Master clock synchronizer
            lsl_sync: LSL synchronizer
        """
        self.master_sync = master_sync
        self.lsl_sync = lsl_sync
        self.logger = logging.getLogger(__name__)
        
        # Session state
        self.current_session: Optional[SessionConfig] = None
        self.session_state = SessionState.IDLE
        self.device_sessions: Dict[str, DeviceSession] = {}
        
        # Threading
        self._session_thread = None
        self._lock = threading.RLock()
        
        # Callbacks
        self.callbacks: Dict[str, List[Callable]] = {
            'session_started': [],
            'session_stopped': [],
            'session_error': [],
            'device_synchronized': [],
            'sync_quality_alert': []
        }
        
        self.logger.info("Session synchronizer initialized")
    
    def prepare_session(self, session_config: SessionConfig, devices: List[DeviceInfo]) -> bool:
        """
        Prepare a recording session with specified devices.
        
        Args:
            session_config: Session configuration
            devices: List of devices to include
            
        Returns:
            bool: True if preparation successful
        """
        with self._lock:
            if self.session_state != SessionState.IDLE:
                self.logger.error(f"Cannot prepare session in state: {self.session_state}")
                return False
            
            self.session_state = SessionState.PREPARING
            self.current_session = session_config
            self.device_sessions.clear()
        
        try:
            # Initialize device sessions
            for device in devices:
                device_session = DeviceSession(
                    device_id=device.device_id,
                    device_info=device,
                    state=SessionState.PREPARING
                )
                self.device_sessions[device.device_id] = device_session
                
                # Register device with master synchronizer if not already registered
                self.master_sync.register_device(device)
            
            self.logger.info(f"Prepared session {session_config.session_id} with {len(devices)} devices")
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to prepare session: {e}")
            self.session_state = SessionState.ERROR
            return False
    
    def synchronize_devices(self) -> bool:
        """
        Synchronize all devices in the session.
        
        Returns:
            bool: True if synchronization successful
        """
        with self._lock:
            if self.session_state != SessionState.PREPARING:
                self.logger.error(f"Cannot synchronize in state: {self.session_state}")
                return False
            
            self.session_state = SessionState.SYNCHRONIZED
        
        try:
            sync_successful = True
            
            # Synchronize each device
            for device_id, device_session in self.device_sessions.items():
                self.logger.info(f"Synchronizing device: {device_id}")
                
                # Perform synchronization attempts
                for attempt in range(self.current_session.retry_attempts):
                    sync_result = self.master_sync.synchronize_device(device_id)
                    
                    if sync_result.success and sync_result.quality_score >= self.current_session.quality_threshold:
                        device_session.sync_quality = sync_result.quality_score
                        device_session.last_sync_time = time.time()
                        device_session.state = SessionState.SYNCHRONIZED
                        
                        self.logger.info(f"Device {device_id} synchronized: quality={sync_result.quality_score:.3f}")
                        self._trigger_callback('device_synchronized', device_session)
                        break
                    else:
                        self.logger.warning(f"Sync attempt {attempt + 1} failed for {device_id}: {sync_result.message}")
                        time.sleep(0.5)  # Brief delay before retry
                else:
                    # All attempts failed
                    device_session.state = SessionState.ERROR
                    device_session.error_message = f"Synchronization failed after {self.current_session.retry_attempts} attempts"
                    sync_successful = False
                    
                    self.logger.error(f"Failed to synchronize device {device_id}")
            
            if not sync_successful:
                self.session_state = SessionState.ERROR
                return False
            
            self.logger.info("All devices synchronized successfully")
            return True
            
        except Exception as e:
            self.logger.error(f"Error during synchronization: {e}")
            self.session_state = SessionState.ERROR
            return False
    
    def start_recording(self, scheduled_start_time: Optional[float] = None) -> bool:
        """
        Start synchronized recording session.
        
        Args:
            scheduled_start_time: Scheduled start time (current + delay if None)
            
        Returns:
            bool: True if recording started successfully
        """
        with self._lock:
            if self.session_state != SessionState.SYNCHRONIZED:
                self.logger.error(f"Cannot start recording in state: {self.session_state}")
                return False
        
        try:
            # Calculate start time
            if scheduled_start_time is None:
                scheduled_start_time = time.time() + (self.current_session.auto_start_delay_ms / 1000.0)
            
            self.logger.info(f"Scheduling recording start at {scheduled_start_time}")
            
            # Send start commands to all devices
            start_successful = True
            for device_id, device_session in self.device_sessions.items():
                try:
                    # Create appropriate stream outlets for device
                    if device_session.device_info.device_type.value == "shimmer":
                        self._create_shimmer_streams(device_session, scheduled_start_time)
                    elif device_session.device_info.device_type.value == "android":
                        self._create_android_streams(device_session, scheduled_start_time)
                    
                    device_session.state = SessionState.RECORDING
                    
                except Exception as e:
                    self.logger.error(f"Failed to start recording on {device_id}: {e}")
                    device_session.state = SessionState.ERROR
                    device_session.error_message = str(e)
                    start_successful = False
            
            if start_successful:
                with self._lock:
                    self.session_state = SessionState.RECORDING
                
                # Start session monitoring thread
                self._session_thread = threading.Thread(target=self._session_monitor_loop, daemon=True)
                self._session_thread.start()
                
                self.logger.info(f"Recording session {self.current_session.session_id} started")
                self._trigger_callback('session_started', self.current_session)
                return True
            else:
                self.session_state = SessionState.ERROR
                return False
                
        except Exception as e:
            self.logger.error(f"Error starting recording: {e}")
            self.session_state = SessionState.ERROR
            return False
    
    def stop_recording(self) -> bool:
        """
        Stop recording session.
        
        Returns:
            bool: True if stopped successfully
        """
        with self._lock:
            if self.session_state != SessionState.RECORDING:
                self.logger.warning(f"Not recording (state: {self.session_state})")
                return True
            
            self.session_state = SessionState.STOPPING
        
        try:
            # Stop recording on all devices
            for device_id, device_session in self.device_sessions.items():
                try:
                    # Stop stream outlets
                    for outlet_id in device_session.stream_outlets:
                        if outlet_id in self.lsl_sync.outlets:
                            self.lsl_sync.outlets[outlet_id].stop()
                    
                    device_session.state = SessionState.COMPLETED
                    
                except Exception as e:
                    self.logger.error(f"Error stopping recording on {device_id}: {e}")
            
            with self._lock:
                self.session_state = SessionState.COMPLETED
            
            self.logger.info(f"Recording session {self.current_session.session_id} stopped")
            self._trigger_callback('session_stopped', self.current_session)
            
            return True
            
        except Exception as e:
            self.logger.error(f"Error stopping recording: {e}")
            self.session_state = SessionState.ERROR
            return False
    
    def get_session_status(self) -> Dict[str, Any]:
        """Get current session status."""
        with self._lock:
            status = {
                'state': self.session_state.value,
                'session_id': self.current_session.session_id if self.current_session else None,
                'devices': {}
            }
            
            for device_id, device_session in self.device_sessions.items():
                status['devices'][device_id] = {
                    'state': device_session.state.value,
                    'sync_quality': device_session.sync_quality,
                    'last_sync_time': device_session.last_sync_time,
                    'error_message': device_session.error_message,
                    'stream_count': len(device_session.stream_outlets)
                }
            
            return status
    
    def _create_shimmer_streams(self, device_session: DeviceSession, start_time: float) -> None:
        """Create stream outlets for Shimmer device."""
        device_id = device_session.device_id
        
        # GSR stream
        gsr_info = StreamInfo(
            name=f"Shimmer_GSR_{device_id}",
            type="GSR",
            channel_count=1,
            nominal_srate=128.0,  # Default Shimmer rate
            channel_format=StreamType.FLOAT32,
            source_id=f"{device_id}_gsr",
            device_id=device_id,
            description="Galvanic Skin Response from Shimmer3"
        )
        
        gsr_outlet = self.lsl_sync.create_outlet(gsr_info)
        port = gsr_outlet.start()
        device_session.stream_outlets.append(gsr_info.source_id)
        
        # PPG stream
        ppg_info = StreamInfo(
            name=f"Shimmer_PPG_{device_id}",
            type="PPG",
            channel_count=1,
            nominal_srate=128.0,
            channel_format=StreamType.FLOAT32,
            source_id=f"{device_id}_ppg",
            device_id=device_id,
            description="Photoplethysmography from Shimmer3"
        )
        
        ppg_outlet = self.lsl_sync.create_outlet(ppg_info)
        ppg_outlet.start()
        device_session.stream_outlets.append(ppg_info.source_id)
        
        self.logger.info(f"Created Shimmer streams for {device_id}")
    
    def _create_android_streams(self, device_session: DeviceSession, start_time: float) -> None:
        """Create stream outlets for Android device."""
        device_id = device_session.device_id
        
        # RGB camera stream metadata
        rgb_info = StreamInfo(
            name=f"Android_RGB_{device_id}",
            type="VideoRGB",
            channel_count=3,  # RGB channels
            nominal_srate=30.0,  # 30 FPS
            channel_format=StreamType.FLOAT32,
            source_id=f"{device_id}_rgb",
            device_id=device_id,
            description="RGB camera frames from Android device"
        )
        
        rgb_outlet = self.lsl_sync.create_outlet(rgb_info)
        rgb_outlet.start()
        device_session.stream_outlets.append(rgb_info.source_id)
        
        # Thermal camera stream metadata
        thermal_info = StreamInfo(
            name=f"Android_Thermal_{device_id}",
            type="VideoThermal",
            channel_count=1,  # Temperature data
            nominal_srate=25.0,  # 25 FPS
            channel_format=StreamType.FLOAT32,
            source_id=f"{device_id}_thermal",
            device_id=device_id,
            description="Thermal camera data from Android device"
        )
        
        thermal_outlet = self.lsl_sync.create_outlet(thermal_info)
        thermal_outlet.start()
        device_session.stream_outlets.append(thermal_info.source_id)
        
        self.logger.info(f"Created Android streams for {device_id}")
    
    def _session_monitor_loop(self) -> None:
        """Monitor session progress and handle duration limits."""
        start_time = time.time()
        
        try:
            while self.session_state == SessionState.RECORDING:
                # Check duration limit
                if (self.current_session.duration_seconds and 
                    time.time() - start_time >= self.current_session.duration_seconds):
                    self.logger.info("Session duration reached, stopping recording")
                    self.stop_recording()
                    break
                
                # Monitor sync quality
                self._monitor_sync_quality()
                
                time.sleep(1.0)  # Check every second
                
        except Exception as e:
            self.logger.error(f"Error in session monitor: {e}")
            self.session_state = SessionState.ERROR
    
    def _monitor_sync_quality(self) -> None:
        """Monitor synchronization quality of all devices."""
        for device_id, device_session in self.device_sessions.items():
            if device_session.state == SessionState.RECORDING:
                # Check if sync is getting stale
                time_since_sync = time.time() - device_session.last_sync_time
                if time_since_sync > 30.0:  # 30 seconds
                    # Trigger re-synchronization
                    sync_result = self.master_sync.synchronize_device(device_id)
                    if sync_result.success:
                        device_session.sync_quality = sync_result.quality_score
                        device_session.last_sync_time = time.time()
                    else:
                        self.logger.warning(f"Re-sync failed for {device_id}")
                        self._trigger_callback('sync_quality_alert', device_session)
    
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