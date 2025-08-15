"""
Shimmer GSR sensor management for the Multi-Sensor Recording System.
Handles connection, data streaming, and management of Shimmer3 GSR+ sensors.
"""

import csv
import logging
import threading
import time
from typing import Dict, List, Optional, Callable, Any
from dataclasses import dataclass
from pathlib import Path
import queue


@dataclass
class ShimmerDataSample:
    """Single Shimmer sensor data sample."""
    timestamp: float
    gsr: Optional[float] = None
    ppg: Optional[float] = None
    accelerometer_x: Optional[float] = None
    accelerometer_y: Optional[float] = None
    accelerometer_z: Optional[float] = None
    gyroscope_x: Optional[float] = None
    gyroscope_y: Optional[float] = None
    gyroscope_z: Optional[float] = None
    magnetometer_x: Optional[float] = None
    magnetometer_y: Optional[float] = None
    magnetometer_z: Optional[float] = None


@dataclass
class ShimmerDeviceInfo:
    """Shimmer device information."""
    device_id: str
    name: str
    mac_address: Optional[str] = None
    firmware_version: Optional[str] = None
    battery_level: Optional[float] = None
    sampling_rate: int = 128
    enabled_sensors: List[str] = None
    is_connected: bool = False
    is_streaming: bool = False
    
    def __post_init__(self):
        if self.enabled_sensors is None:
            self.enabled_sensors = []


class ShimmerManager:
    """Manages Shimmer GSR sensor connections and data streaming."""
    
    def __init__(self, config):
        """
        Initialize Shimmer manager.
        
        Args:
            config: System configuration object
        """
        self.config = config
        self.logger = logging.getLogger(__name__)
        
        # Device management
        self.devices: Dict[str, ShimmerDeviceInfo] = {}
        self.connections: Dict[str, Any] = {}  # Shimmer device connections
        
        # Data streaming
        self.data_queues: Dict[str, queue.Queue] = {}
        self.data_writers: Dict[str, csv.DictWriter] = {}
        self.data_files: Dict[str, Any] = {}
        
        # Threading
        self.streaming_threads: Dict[str, threading.Thread] = {}
        self.running = False
        self._lock = threading.RLock()
        
        # Callbacks
        self.callbacks: Dict[str, List[Callable]] = {
            'device_connected': [],
            'device_disconnected': [],
            'data_received': [],
            'streaming_started': [],
            'streaming_stopped': [],
            'error': []
        }
        
        # Try to import Shimmer libraries
        self.shimmer_available = self._check_shimmer_libraries()
        
        if not self.shimmer_available:
            self.logger.warning("Shimmer libraries not available - running in simulation mode")
        
        self.logger.info("Shimmer manager initialized")
    
    def _check_shimmer_libraries(self) -> bool:
        """Check if Shimmer libraries are available."""
        try:
            # Try to import PyShimmer or other Shimmer libraries
            # This is a placeholder since PyShimmer might not be available
            # import shimmer3
            # return True
            return False  # For now, assume not available
        except ImportError:
            return False
    
    def start(self) -> None:
        """Start the Shimmer manager."""
        with self._lock:
            if self.running:
                self.logger.warning("Shimmer manager already running")
                return
            
            self.running = True
            self.logger.info("Shimmer manager started")
    
    def stop(self) -> None:
        """Stop the Shimmer manager and close all connections."""
        with self._lock:
            if not self.running:
                return
            
            self.running = False
            
            # Stop all streaming
            for device_id in list(self.devices.keys()):
                self.stop_streaming(device_id)
                self.disconnect_device(device_id)
            
            self.logger.info("Shimmer manager stopped")
    
    def scan_devices(self) -> List[ShimmerDeviceInfo]:
        """
        Scan for available Shimmer devices.
        
        Returns:
            List[ShimmerDeviceInfo]: List of discovered devices
        """
        discovered_devices = []
        
        if self.shimmer_available:
            try:
                # TODO: Implement actual Shimmer device scanning
                # This would use the Shimmer library to discover devices
                pass
            except Exception as e:
                self.logger.error(f"Device scan failed: {e}")
        else:
            # Simulation mode - create mock devices
            mock_device = ShimmerDeviceInfo(
                device_id="shimmer_sim_001",
                name="Shimmer3 GSR+ (Simulated)",
                mac_address="00:11:22:33:44:55",
                firmware_version="1.0.0-sim",
                sampling_rate=self.config.sensors.shimmer_sampling_rate,
                enabled_sensors=["GSR", "PPG", "Accelerometer"]
            )
            discovered_devices.append(mock_device)
            
            with self._lock:
                self.devices[mock_device.device_id] = mock_device
        
        self.logger.info(f"Discovered {len(discovered_devices)} Shimmer devices")
        return discovered_devices
    
    def connect_device(self, device_id: str) -> bool:
        """
        Connect to a Shimmer device.
        
        Args:
            device_id: Device identifier
            
        Returns:
            bool: True if connection successful
        """
        with self._lock:
            if device_id not in self.devices:
                self.logger.error(f"Device {device_id} not found")
                return False
            
            device = self.devices[device_id]
            
            if device.is_connected:
                self.logger.warning(f"Device {device_id} already connected")
                return True
            
            try:
                if self.shimmer_available:
                    # TODO: Implement actual Shimmer connection
                    # connection = shimmer3.ShimmerDevice(device.mac_address)
                    # connection.connect()
                    # self.connections[device_id] = connection
                    pass
                else:
                    # Simulation mode
                    self.connections[device_id] = SimulatedShimmerDevice(device_id)
                
                device.is_connected = True
                
                self.logger.info(f"Connected to Shimmer device: {device_id}")
                self._trigger_callback('device_connected', device_id)
                
                return True
                
            except Exception as e:
                self.logger.error(f"Failed to connect to device {device_id}: {e}")
                self._trigger_callback('error', device_id, str(e))
                return False
    
    def disconnect_device(self, device_id: str) -> None:
        """
        Disconnect from a Shimmer device.
        
        Args:
            device_id: Device identifier
        """
        with self._lock:
            if device_id not in self.devices:
                return
            
            device = self.devices[device_id]
            
            # Stop streaming first
            if device.is_streaming:
                self.stop_streaming(device_id)
            
            # Close connection
            if device_id in self.connections:
                try:
                    if self.shimmer_available:
                        # TODO: Implement actual disconnection
                        # self.connections[device_id].disconnect()
                        pass
                    del self.connections[device_id]
                except Exception as e:
                    self.logger.error(f"Error disconnecting {device_id}: {e}")
            
            device.is_connected = False
            
            self.logger.info(f"Disconnected from Shimmer device: {device_id}")
            self._trigger_callback('device_disconnected', device_id)
    
    def start_streaming(self, device_id: str, output_file: Optional[Path] = None) -> bool:
        """
        Start data streaming from a Shimmer device.
        
        Args:
            device_id: Device identifier
            output_file: Optional output CSV file path
            
        Returns:
            bool: True if streaming started successfully
        """
        with self._lock:
            if device_id not in self.devices:
                self.logger.error(f"Device {device_id} not found")
                return False
            
            device = self.devices[device_id]
            
            if not device.is_connected:
                self.logger.error(f"Device {device_id} not connected")
                return False
            
            if device.is_streaming:
                self.logger.warning(f"Device {device_id} already streaming")
                return True
            
            try:
                # Setup data queue
                self.data_queues[device_id] = queue.Queue()
                
                # Setup CSV writer if output file specified
                if output_file:
                    output_file.parent.mkdir(parents=True, exist_ok=True)
                    file_handle = open(output_file, 'w', newline='')
                    
                    fieldnames = [
                        'timestamp', 'gsr', 'ppg',
                        'accelerometer_x', 'accelerometer_y', 'accelerometer_z',
                        'gyroscope_x', 'gyroscope_y', 'gyroscope_z',
                        'magnetometer_x', 'magnetometer_y', 'magnetometer_z'
                    ]
                    
                    writer = csv.DictWriter(file_handle, fieldnames=fieldnames)
                    writer.writeheader()
                    
                    self.data_files[device_id] = file_handle
                    self.data_writers[device_id] = writer
                
                # Start streaming thread
                streaming_thread = threading.Thread(
                    target=self._streaming_loop,
                    args=(device_id,),
                    daemon=True
                )
                streaming_thread.start()
                self.streaming_threads[device_id] = streaming_thread
                
                device.is_streaming = True
                
                self.logger.info(f"Started streaming from device: {device_id}")
                self._trigger_callback('streaming_started', device_id)
                
                return True
                
            except Exception as e:
                self.logger.error(f"Failed to start streaming from {device_id}: {e}")
                self._trigger_callback('error', device_id, str(e))
                return False
    
    def stop_streaming(self, device_id: str) -> None:
        """
        Stop data streaming from a Shimmer device.
        
        Args:
            device_id: Device identifier
        """
        with self._lock:
            if device_id not in self.devices:
                return
            
            device = self.devices[device_id]
            
            if not device.is_streaming:
                return
            
            device.is_streaming = False
            
            # Wait for streaming thread to finish
            if device_id in self.streaming_threads:
                thread = self.streaming_threads[device_id]
                if thread.is_alive():
                    thread.join(timeout=5.0)
                del self.streaming_threads[device_id]
            
            # Close data file
            if device_id in self.data_files:
                try:
                    self.data_files[device_id].close()
                except:
                    pass
                del self.data_files[device_id]
            
            # Clean up
            if device_id in self.data_writers:
                del self.data_writers[device_id]
            if device_id in self.data_queues:
                del self.data_queues[device_id]
            
            self.logger.info(f"Stopped streaming from device: {device_id}")
            self._trigger_callback('streaming_stopped', device_id)
    
    def _streaming_loop(self, device_id: str) -> None:
        """
        Main streaming loop for a device.
        
        Args:
            device_id: Device identifier
        """
        device = self.devices[device_id]
        connection = self.connections[device_id]
        
        self.logger.debug(f"Starting streaming loop for {device_id}")
        
        try:
            while self.running and device.is_streaming:
                try:
                    # Get data from device
                    if self.shimmer_available:
                        # TODO: Implement actual data reading
                        # data = connection.read_data()
                        data = None
                    else:
                        # Simulation mode
                        data = connection.read_data()
                    
                    if data:
                        # Convert to our data format
                        sample = self._convert_data_sample(data)
                        
                        # Add to queue
                        if device_id in self.data_queues:
                            self.data_queues[device_id].put(sample)
                        
                        # Write to CSV file
                        if device_id in self.data_writers:
                            sample_dict = {
                                'timestamp': sample.timestamp,
                                'gsr': sample.gsr,
                                'ppg': sample.ppg,
                                'accelerometer_x': sample.accelerometer_x,
                                'accelerometer_y': sample.accelerometer_y,
                                'accelerometer_z': sample.accelerometer_z,
                                'gyroscope_x': sample.gyroscope_x,
                                'gyroscope_y': sample.gyroscope_y,
                                'gyroscope_z': sample.gyroscope_z,
                                'magnetometer_x': sample.magnetometer_x,
                                'magnetometer_y': sample.magnetometer_y,
                                'magnetometer_z': sample.magnetometer_z
                            }
                            self.data_writers[device_id].writerow(sample_dict)
                            self.data_files[device_id].flush()
                        
                        # Trigger callback
                        self._trigger_callback('data_received', device_id, sample)
                    
                    # Control sampling rate
                    time.sleep(1.0 / device.sampling_rate)
                    
                except Exception as e:
                    if device.is_streaming:  # Only log if still supposed to be streaming
                        self.logger.error(f"Streaming error for {device_id}: {e}")
                    break
        
        except Exception as e:
            self.logger.error(f"Streaming loop error for {device_id}: {e}")
        finally:
            self.logger.debug(f"Streaming loop ended for {device_id}")
    
    def _convert_data_sample(self, raw_data: Any) -> ShimmerDataSample:
        """
        Convert raw device data to ShimmerDataSample.
        
        Args:
            raw_data: Raw data from device
            
        Returns:
            ShimmerDataSample: Converted data sample
        """
        # This would depend on the actual Shimmer data format
        # For now, assume raw_data is already in the right format
        if hasattr(raw_data, 'timestamp'):
            return raw_data
        else:
            # Create from dictionary or other format
            return ShimmerDataSample(
                timestamp=time.time(),
                gsr=getattr(raw_data, 'gsr', None),
                ppg=getattr(raw_data, 'ppg', None),
                accelerometer_x=getattr(raw_data, 'acc_x', None),
                accelerometer_y=getattr(raw_data, 'acc_y', None),
                accelerometer_z=getattr(raw_data, 'acc_z', None)
            )
    
    def get_connected_devices(self) -> List[ShimmerDeviceInfo]:
        """
        Get list of connected Shimmer devices.
        
        Returns:
            List[ShimmerDeviceInfo]: Connected devices
        """
        with self._lock:
            return [device for device in self.devices.values() if device.is_connected]
    
    def get_streaming_devices(self) -> List[ShimmerDeviceInfo]:
        """
        Get list of streaming Shimmer devices.
        
        Returns:
            List[ShimmerDeviceInfo]: Streaming devices
        """
        with self._lock:
            return [device for device in self.devices.values() if device.is_streaming]
    
    def get_device_info(self, device_id: str) -> Optional[ShimmerDeviceInfo]:
        """
        Get information about a specific device.
        
        Args:
            device_id: Device identifier
            
        Returns:
            Optional[ShimmerDeviceInfo]: Device information
        """
        with self._lock:
            return self.devices.get(device_id)
    
    def register_callback(self, event: str, callback: Callable) -> None:
        """
        Register callback for Shimmer events.
        
        Args:
            event: Event name
            callback: Callback function
        """
        if event in self.callbacks:
            self.callbacks[event].append(callback)
        else:
            self.logger.warning(f"Unknown callback event: {event}")
    
    def _trigger_callback(self, event: str, *args) -> None:
        """Trigger callbacks for an event."""
        for callback in self.callbacks.get(event, []):
            try:
                callback(*args)
            except Exception as e:
                self.logger.error(f"Callback error for {event}: {e}")


class SimulatedShimmerDevice:
    """Simulated Shimmer device for testing."""
    
    def __init__(self, device_id: str):
        """Initialize simulated device."""
        self.device_id = device_id
        self.start_time = time.time()
        self.sample_count = 0
        
    def read_data(self) -> ShimmerDataSample:
        """Generate simulated sensor data."""
        import random
        import math
        
        current_time = time.time()
        elapsed = current_time - self.start_time
        
        # Generate realistic GSR data (2-50 µS range)
        base_gsr = 10.0
        noise = random.gauss(0, 0.5)
        trend = 5.0 * math.sin(elapsed / 30.0)  # Slow variation
        gsr_value = max(2.0, min(50.0, base_gsr + trend + noise))
        
        # Generate PPG data (heart rate simulation)
        heart_rate = 70  # BPM
        ppg_value = 512 + 100 * math.sin(2 * math.pi * heart_rate * elapsed / 60.0)
        ppg_value += random.gauss(0, 10)  # Add noise
        
        # Generate accelerometer data (mostly static with small movements)
        acc_x = random.gauss(0, 0.1)
        acc_y = random.gauss(0, 0.1)
        acc_z = random.gauss(1.0, 0.1)  # Gravity
        
        self.sample_count += 1
        
        return ShimmerDataSample(
            timestamp=current_time,
            gsr=gsr_value,
            ppg=ppg_value,
            accelerometer_x=acc_x,
            accelerometer_y=acc_y,
            accelerometer_z=acc_z
        )