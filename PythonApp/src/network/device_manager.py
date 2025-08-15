"""
Device management for the Multi-Sensor Recording System.
Handles discovery, connection, and communication with Android and sensor devices.
"""

import json
import logging
import socket
import threading
import time
from typing import Dict, List, Optional, Callable, Any
from dataclasses import dataclass
from enum import Enum
import asyncio


class DeviceType(Enum):
    """Device type enumeration."""
    ANDROID = "android"
    SHIMMER = "shimmer"
    UNKNOWN = "unknown"


class DeviceStatus(Enum):
    """Device status enumeration."""
    DISCONNECTED = "disconnected"
    CONNECTING = "connecting"
    CONNECTED = "connected"
    RECORDING = "recording"
    ERROR = "error"


@dataclass
class DeviceInfo:
    """Device information structure."""
    device_id: str
    device_type: DeviceType
    name: str
    address: str
    port: Optional[int] = None
    status: DeviceStatus = DeviceStatus.DISCONNECTED
    capabilities: List[str] = None
    metadata: Dict[str, Any] = None
    last_seen: Optional[float] = None
    
    def __post_init__(self):
        if self.capabilities is None:
            self.capabilities = []
        if self.metadata is None:
            self.metadata = {}


class DeviceManager:
    """Manages device discovery, connection, and communication."""
    
    def __init__(self, config):
        """
        Initialize device manager.
        
        Args:
            config: System configuration object
        """
        self.config = config
        self.logger = logging.getLogger(__name__)
        
        # Device registry
        self.devices: Dict[str, DeviceInfo] = {}
        self.connections: Dict[str, socket.socket] = {}
        
        # Network server for Android devices
        self.server_socket: Optional[socket.socket] = None
        self.server_thread: Optional[threading.Thread] = None
        self.running = False
        
        # Thread safety
        self._lock = threading.RLock()
        
        # Callbacks
        self.callbacks: Dict[str, List[Callable]] = {
            'device_discovered': [],
            'device_connected': [],
            'device_disconnected': [],
            'data_received': [],
            'error': []
        }
        
        self.logger.info("Device manager initialized")
    
    def start(self) -> None:
        """Start the device manager and network server."""
        with self._lock:
            if self.running:
                self.logger.warning("Device manager already running")
                return
            
            self.running = True
            
            # Start network server for Android devices
            self._start_server()
            
            self.logger.info("Device manager started")
    
    def stop(self) -> None:
        """Stop the device manager and close all connections."""
        with self._lock:
            if not self.running:
                return
            
            self.running = False
            
            # Close all device connections
            for device_id in list(self.connections.keys()):
                self.disconnect_device(device_id)
            
            # Stop network server
            self._stop_server()
            
            self.logger.info("Device manager stopped")
    
    def _start_server(self) -> None:
        """Start the TCP server for Android device connections."""
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind((self.config.network.host, self.config.network.port))
            self.server_socket.listen(self.config.network.max_connections)
            
            self.server_thread = threading.Thread(target=self._server_loop, daemon=True)
            self.server_thread.start()
            
            self.logger.info(f"Server started on {self.config.network.host}:{self.config.network.port}")
            
        except Exception as e:
            self.logger.error(f"Failed to start server: {e}")
            raise
    
    def _stop_server(self) -> None:
        """Stop the TCP server."""
        if self.server_socket:
            try:
                self.server_socket.close()
            except:
                pass
            self.server_socket = None
        
        if self.server_thread and self.server_thread.is_alive():
            self.server_thread.join(timeout=5.0)
        
        self.server_thread = None
    
    def _server_loop(self) -> None:
        """Main server loop for accepting connections."""
        self.logger.info("Server loop started")
        
        while self.running and self.server_socket:
            try:
                # Accept incoming connections
                client_socket, address = self.server_socket.accept()
                
                self.logger.info(f"New connection from {address}")
                
                # Handle connection in separate thread
                client_thread = threading.Thread(
                    target=self._handle_client,
                    args=(client_socket, address),
                    daemon=True
                )
                client_thread.start()
                
            except socket.error as e:
                if self.running:
                    self.logger.error(f"Server socket error: {e}")
                break
            except Exception as e:
                self.logger.error(f"Unexpected server error: {e}")
                if self.running:
                    time.sleep(1)  # Brief pause before retrying
    
    def _handle_client(self, client_socket: socket.socket, address: tuple) -> None:
        """
        Handle communication with a connected client.
        
        Args:
            client_socket: Client socket connection
            address: Client address tuple (host, port)
        """
        device_id = None
        
        try:
            client_socket.settimeout(self.config.network.timeout)
            
            # Wait for device registration message
            registration_msg = self._receive_message(client_socket)
            if not registration_msg:
                self.logger.warning(f"No registration message from {address}")
                return
            
            # Parse registration
            try:
                reg_data = json.loads(registration_msg)
                if reg_data.get('type') != 'device_registration':
                    raise ValueError("Invalid registration message type")
                
                device_id = reg_data['device_id']
                device_name = reg_data.get('device_name', f"Device_{device_id}")
                capabilities = reg_data.get('capabilities', [])
                metadata = reg_data.get('metadata', {})
                
            except (json.JSONDecodeError, KeyError, ValueError) as e:
                self.logger.error(f"Invalid registration from {address}: {e}")
                return
            
            # Register device
            device_info = DeviceInfo(
                device_id=device_id,
                device_type=DeviceType.ANDROID,
                name=device_name,
                address=address[0],
                port=address[1],
                status=DeviceStatus.CONNECTED,
                capabilities=capabilities,
                metadata=metadata,
                last_seen=time.time()
            )
            
            with self._lock:
                self.devices[device_id] = device_info
                self.connections[device_id] = client_socket
            
            # Send registration confirmation
            confirmation = {
                'type': 'registration_confirmed',
                'device_id': device_id,
                'server_time': time.time()
            }
            self._send_message(client_socket, json.dumps(confirmation))
            
            self.logger.info(f"Device registered: {device_id} ({device_name})")
            self._trigger_callback('device_connected', device_id, device_info)
            
            # Handle ongoing communication
            self._handle_device_communication(client_socket, device_id)
            
        except Exception as e:
            self.logger.error(f"Client handler error for {address}: {e}")
        finally:
            # Cleanup on disconnection
            if device_id:
                self._handle_device_disconnection(device_id)
            
            try:
                client_socket.close()
            except:
                pass
    
    def _handle_device_communication(self, client_socket: socket.socket, device_id: str) -> None:
        """
        Handle ongoing communication with a device.
        
        Args:
            client_socket: Client socket
            device_id: Device identifier
        """
        while self.running:
            try:
                message = self._receive_message(client_socket)
                if not message:
                    break
                
                # Update last seen time
                with self._lock:
                    if device_id in self.devices:
                        self.devices[device_id].last_seen = time.time()
                
                # Process message
                self._process_device_message(device_id, message)
                
            except socket.timeout:
                # Check if device is still responsive
                if not self._ping_device(client_socket):
                    self.logger.warning(f"Device {device_id} not responding")
                    break
            except socket.error as e:
                self.logger.info(f"Device {device_id} disconnected: {e}")
                break
            except Exception as e:
                self.logger.error(f"Communication error with {device_id}: {e}")
                break
    
    def _handle_device_disconnection(self, device_id: str) -> None:
        """Handle device disconnection cleanup."""
        with self._lock:
            if device_id in self.devices:
                self.devices[device_id].status = DeviceStatus.DISCONNECTED
                self.logger.info(f"Device disconnected: {device_id}")
                self._trigger_callback('device_disconnected', device_id)
            
            if device_id in self.connections:
                del self.connections[device_id]
    
    def _receive_message(self, sock: socket.socket) -> Optional[str]:
        """
        Receive a complete message from socket.
        
        Args:
            sock: Socket to receive from
            
        Returns:
            Optional[str]: Received message or None if connection closed
        """
        try:
            # First, receive message length (4 bytes)
            length_data = sock.recv(4)
            if len(length_data) != 4:
                return None
            
            message_length = int.from_bytes(length_data, byteorder='big')
            
            # Receive the actual message
            message_data = b''
            while len(message_data) < message_length:
                chunk = sock.recv(message_length - len(message_data))
                if not chunk:
                    return None
                message_data += chunk
            
            return message_data.decode('utf-8')
            
        except Exception as e:
            self.logger.debug(f"Message receive error: {e}")
            return None
    
    def _send_message(self, sock: socket.socket, message: str) -> bool:
        """
        Send a complete message to socket.
        
        Args:
            sock: Socket to send to
            message: Message to send
            
        Returns:
            bool: True if successful
        """
        try:
            message_bytes = message.encode('utf-8')
            length_bytes = len(message_bytes).to_bytes(4, byteorder='big')
            
            sock.sendall(length_bytes + message_bytes)
            return True
            
        except Exception as e:
            self.logger.debug(f"Message send error: {e}")
            return False
    
    def _ping_device(self, sock: socket.socket) -> bool:
        """
        Send ping to device and wait for pong.
        
        Args:
            sock: Socket to ping
            
        Returns:
            bool: True if device responded
        """
        try:
            ping_msg = json.dumps({'type': 'ping', 'timestamp': time.time()})
            if not self._send_message(sock, ping_msg):
                return False
            
            # Wait for pong response (with short timeout)
            sock.settimeout(5.0)
            response = self._receive_message(sock)
            sock.settimeout(self.config.network.timeout)
            
            if response:
                try:
                    data = json.loads(response)
                    return data.get('type') == 'pong'
                except:
                    pass
            
            return False
            
        except Exception:
            return False
    
    def _process_device_message(self, device_id: str, message: str) -> None:
        """
        Process incoming message from device.
        
        Args:
            device_id: Device identifier
            message: Received message
        """
        try:
            data = json.loads(message)
            msg_type = data.get('type')
            
            if msg_type == 'status_update':
                self._handle_status_update(device_id, data)
            elif msg_type == 'sensor_data':
                self._handle_sensor_data(device_id, data)
            elif msg_type == 'pong':
                # Ping response - no action needed
                pass
            elif msg_type == 'error':
                self._handle_device_error(device_id, data)
            else:
                self.logger.warning(f"Unknown message type from {device_id}: {msg_type}")
            
            # Trigger data received callback
            self._trigger_callback('data_received', device_id, data)
            
        except json.JSONDecodeError as e:
            self.logger.error(f"Invalid JSON from {device_id}: {e}")
        except Exception as e:
            self.logger.error(f"Message processing error for {device_id}: {e}")
    
    def _handle_status_update(self, device_id: str, data: Dict[str, Any]) -> None:
        """Handle device status update."""
        with self._lock:
            if device_id in self.devices:
                device = self.devices[device_id]
                
                # Update device metadata
                if 'battery_level' in data:
                    device.metadata['battery_level'] = data['battery_level']
                if 'recording_status' in data:
                    recording = data['recording_status']
                    device.status = DeviceStatus.RECORDING if recording else DeviceStatus.CONNECTED
                
                self.logger.debug(f"Status update from {device_id}: {data}")
    
    def _handle_sensor_data(self, device_id: str, data: Dict[str, Any]) -> None:
        """Handle incoming sensor data from device."""
        # This would be handled by specific sensor managers
        self.logger.debug(f"Sensor data from {device_id}: {data.get('sensor_type', 'unknown')}")
    
    def _handle_device_error(self, device_id: str, data: Dict[str, Any]) -> None:
        """Handle device error report."""
        error_msg = data.get('error', 'Unknown error')
        self.logger.error(f"Device {device_id} reported error: {error_msg}")
        
        with self._lock:
            if device_id in self.devices:
                self.devices[device_id].status = DeviceStatus.ERROR
        
        self._trigger_callback('error', device_id, error_msg)
    
    def send_command(self, device_id: str, command: Dict[str, Any]) -> bool:
        """
        Send command to a specific device.
        
        Args:
            device_id: Target device identifier
            command: Command dictionary
            
        Returns:
            bool: True if command was sent successfully
        """
        with self._lock:
            if device_id not in self.connections:
                self.logger.error(f"Device {device_id} not connected")
                return False
            
            sock = self.connections[device_id]
        
        try:
            command_json = json.dumps(command)
            return self._send_message(sock, command_json)
        except Exception as e:
            self.logger.error(f"Failed to send command to {device_id}: {e}")
            return False
    
    def broadcast_command(self, command: Dict[str, Any]) -> int:
        """
        Send command to all connected devices.
        
        Args:
            command: Command dictionary
            
        Returns:
            int: Number of devices that received the command
        """
        success_count = 0
        
        with self._lock:
            device_ids = list(self.connections.keys())
        
        for device_id in device_ids:
            if self.send_command(device_id, command):
                success_count += 1
        
        self.logger.info(f"Broadcast command sent to {success_count}/{len(device_ids)} devices")
        return success_count
    
    def disconnect_device(self, device_id: str) -> None:
        """
        Disconnect a specific device.
        
        Args:
            device_id: Device identifier
        """
        with self._lock:
            if device_id in self.connections:
                try:
                    self.connections[device_id].close()
                except:
                    pass
                del self.connections[device_id]
            
            if device_id in self.devices:
                self.devices[device_id].status = DeviceStatus.DISCONNECTED
        
        self.logger.info(f"Device {device_id} disconnected")
        self._trigger_callback('device_disconnected', device_id)
    
    def get_connected_devices(self) -> List[DeviceInfo]:
        """
        Get list of currently connected devices.
        
        Returns:
            List[DeviceInfo]: Connected devices
        """
        with self._lock:
            return [
                device for device in self.devices.values()
                if device.status in [DeviceStatus.CONNECTED, DeviceStatus.RECORDING]
            ]
    
    def get_device_info(self, device_id: str) -> Optional[DeviceInfo]:
        """
        Get information about a specific device.
        
        Args:
            device_id: Device identifier
            
        Returns:
            Optional[DeviceInfo]: Device information or None if not found
        """
        with self._lock:
            return self.devices.get(device_id)
    
    def register_callback(self, event: str, callback: Callable) -> None:
        """
        Register callback for device events.
        
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
    
    def __del__(self):
        """Cleanup on object destruction."""
        self.stop()