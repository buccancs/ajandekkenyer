"""
LSL (Lab Streaming Layer) Alternative for Multi-Device Synchronization.
Provides real-time data streaming with precise timing synchronization.
"""

import logging
import threading
import time
import json
import uuid
from typing import Dict, List, Optional, Any, Callable, Union
from dataclasses import dataclass, field
from enum import Enum
import socket
import struct

from .master_clock_synchronizer import MasterClockSynchronizer, DeviceInfo, DeviceType


class StreamType(Enum):
    """Stream data types."""
    FLOAT32 = "float32"
    FLOAT64 = "float64"
    INT32 = "int32"
    STRING = "string"


@dataclass
class StreamInfo:
    """Stream information metadata."""
    name: str
    type: str
    channel_count: int
    nominal_srate: float
    channel_format: StreamType
    source_id: str
    device_id: str = ""
    description: str = ""
    channels: List[Dict[str, str]] = field(default_factory=list)
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary."""
        return {
            'name': self.name,
            'type': self.type,
            'channel_count': self.channel_count,
            'nominal_srate': self.nominal_srate,
            'channel_format': self.channel_format.value,
            'source_id': self.source_id,
            'device_id': self.device_id,
            'description': self.description,
            'channels': self.channels
        }


@dataclass 
class StreamSample:
    """Data sample with timing information."""
    data: List[Union[float, int, str]]
    timestamp: float
    stream_id: str
    sequence_number: int = 0


class StreamOutlet:
    """Stream outlet for publishing data."""
    
    def __init__(self, info: StreamInfo, chunk_size: int = 0, max_buffered: int = 360):
        """
        Initialize stream outlet.
        
        Args:
            info: Stream information
            chunk_size: Preferred chunk size for transmission
            max_buffered: Maximum number of samples to buffer
        """
        self.info = info
        self.chunk_size = chunk_size
        self.max_buffered = max_buffered
        self.logger = logging.getLogger(__name__)
        
        # State
        self.active = False
        self._socket = None
        self._clients: List[socket.socket] = []
        self._buffer: List[StreamSample] = []
        self._sequence_number = 0
        self._lock = threading.RLock()
        
        # Threading
        self._server_thread = None
        self._broadcast_thread = None
        
        self.logger.info(f"Created stream outlet: {info.name}")
    
    def start(self, port: int = 0) -> int:
        """
        Start the outlet server.
        
        Args:
            port: Port to bind to (0 for auto-assign)
            
        Returns:
            int: Actual port number used
        """
        try:
            # Create server socket
            self._socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self._socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self._socket.bind(('0.0.0.0', port))
            actual_port = self._socket.getsockname()[1]
            self._socket.listen(5)
            
            with self._lock:
                self.active = True
            
            # Start server thread
            self._server_thread = threading.Thread(target=self._server_loop, daemon=True)
            self._server_thread.start()
            
            # Start broadcast thread
            self._broadcast_thread = threading.Thread(target=self._broadcast_loop, daemon=True)
            self._broadcast_thread.start()
            
            self.logger.info(f"Stream outlet started on port {actual_port}")
            return actual_port
            
        except Exception as e:
            self.logger.error(f"Failed to start stream outlet: {e}")
            self.stop()
            raise
    
    def stop(self) -> None:
        """Stop the outlet."""
        with self._lock:
            if not self.active:
                return
            self.active = False
        
        # Close client connections
        for client in self._clients[:]:
            try:
                client.close()
            except:
                pass
        self._clients.clear()
        
        # Close server socket
        if self._socket:
            try:
                self._socket.close()
            except:
                pass
            self._socket = None
        
        # Wait for threads
        if self._server_thread and self._server_thread.is_alive():
            self._server_thread.join(timeout=2.0)
        if self._broadcast_thread and self._broadcast_thread.is_alive():
            self._broadcast_thread.join(timeout=2.0)
        
        self.logger.info("Stream outlet stopped")
    
    def push_sample(self, sample: List[Union[float, int, str]], timestamp: Optional[float] = None) -> None:
        """
        Push a single sample.
        
        Args:
            sample: Sample data
            timestamp: Sample timestamp (current time if None)
        """
        if not self.active:
            return
        
        if timestamp is None:
            timestamp = time.time()
        
        stream_sample = StreamSample(
            data=sample,
            timestamp=timestamp,
            stream_id=self.info.source_id,
            sequence_number=self._sequence_number
        )
        
        with self._lock:
            self._buffer.append(stream_sample)
            self._sequence_number += 1
            
            # Limit buffer size
            if len(self._buffer) > self.max_buffered:
                self._buffer = self._buffer[-self.max_buffered:]
    
    def push_chunk(self, samples: List[List[Union[float, int, str]]], timestamps: Optional[List[float]] = None) -> None:
        """
        Push multiple samples.
        
        Args:
            samples: List of sample data
            timestamps: List of timestamps (auto-generated if None)
        """
        if not self.active:
            return
        
        if timestamps is None:
            base_time = time.time()
            timestamps = [base_time + i / self.info.nominal_srate for i in range(len(samples))]
        
        stream_samples = []
        for i, sample in enumerate(samples):
            stream_sample = StreamSample(
                data=sample,
                timestamp=timestamps[i],
                stream_id=self.info.source_id,
                sequence_number=self._sequence_number + i
            )
            stream_samples.append(stream_sample)
        
        with self._lock:
            self._buffer.extend(stream_samples)
            self._sequence_number += len(samples)
            
            # Limit buffer size
            if len(self._buffer) > self.max_buffered:
                self._buffer = self._buffer[-self.max_buffered:]
    
    def _server_loop(self) -> None:
        """Accept new client connections."""
        while self.active:
            try:
                if self._socket:
                    client_socket, address = self._socket.accept()
                    
                    # Send stream info to new client
                    self._send_stream_info(client_socket)
                    
                    with self._lock:
                        self._clients.append(client_socket)
                    
                    self.logger.debug(f"New client connected from {address}")
            
            except OSError:
                # Socket closed
                break
            except Exception as e:
                if self.active:
                    self.logger.error(f"Error in server loop: {e}")
                break
    
    def _broadcast_loop(self) -> None:
        """Broadcast samples to connected clients."""
        while self.active:
            try:
                # Get samples to send
                samples_to_send = []
                with self._lock:
                    if self._buffer:
                        # Send up to chunk_size samples
                        chunk_size = self.chunk_size if self.chunk_size > 0 else len(self._buffer)
                        samples_to_send = self._buffer[:chunk_size]
                        self._buffer = self._buffer[chunk_size:]
                
                if samples_to_send:
                    self._broadcast_samples(samples_to_send)
                else:
                    time.sleep(0.01)  # Short sleep if no data
                    
            except Exception as e:
                if self.active:
                    self.logger.error(f"Error in broadcast loop: {e}")
    
    def _send_stream_info(self, client_socket: socket.socket) -> None:
        """Send stream info to client."""
        try:
            info_data = json.dumps(self.info.to_dict()).encode('utf-8')
            header = struct.pack('!I', len(info_data))
            client_socket.send(header + info_data)
        except Exception as e:
            self.logger.error(f"Failed to send stream info: {e}")
    
    def _broadcast_samples(self, samples: List[StreamSample]) -> None:
        """Broadcast samples to all clients."""
        if not samples:
            return
        
        # Serialize samples
        try:
            samples_data = []
            for sample in samples:
                sample_dict = {
                    'data': sample.data,
                    'timestamp': sample.timestamp,
                    'stream_id': sample.stream_id,
                    'sequence_number': sample.sequence_number
                }
                samples_data.append(sample_dict)
            
            message = json.dumps(samples_data).encode('utf-8')
            header = struct.pack('!I', len(message))
            
            # Send to all clients
            failed_clients = []
            with self._lock:
                for client in self._clients:
                    try:
                        client.send(header + message)
                    except Exception:
                        failed_clients.append(client)
                
                # Remove failed clients
                for client in failed_clients:
                    try:
                        client.close()
                    except:
                        pass
                    if client in self._clients:
                        self._clients.remove(client)
        
        except Exception as e:
            self.logger.error(f"Failed to broadcast samples: {e}")


class StreamInlet:
    """Stream inlet for receiving data."""
    
    def __init__(self, info: StreamInfo, max_buflen: int = 360, max_chunklen: int = 0):
        """
        Initialize stream inlet.
        
        Args:
            info: Stream information
            max_buflen: Maximum buffer length
            max_chunklen: Maximum chunk length
        """
        self.info = info
        self.max_buflen = max_buflen
        self.max_chunklen = max_chunklen
        self.logger = logging.getLogger(__name__)
        
        # State
        self.connected = False
        self._socket = None
        self._buffer: List[StreamSample] = []
        self._lock = threading.RLock()
        
        # Threading
        self._receive_thread = None
        
        self.logger.info(f"Created stream inlet: {info.name}")
    
    def connect(self, host: str, port: int) -> bool:
        """
        Connect to stream outlet.
        
        Args:
            host: Outlet host
            port: Outlet port
            
        Returns:
            bool: True if connected successfully
        """
        try:
            self._socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self._socket.connect((host, port))
            
            # Receive stream info
            self._receive_stream_info()
            
            self.connected = True
            
            # Start receive thread
            self._receive_thread = threading.Thread(target=self._receive_loop, daemon=True)
            self._receive_thread.start()
            
            self.logger.info(f"Connected to stream at {host}:{port}")
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to connect to stream: {e}")
            if self._socket:
                self._socket.close()
                self._socket = None
            return False
    
    def disconnect(self) -> None:
        """Disconnect from stream."""
        self.connected = False
        
        if self._socket:
            try:
                self._socket.close()
            except:
                pass
            self._socket = None
        
        if self._receive_thread and self._receive_thread.is_alive():
            self._receive_thread.join(timeout=2.0)
        
        self.logger.info("Disconnected from stream")
    
    def pull_sample(self, timeout: float = 0.0) -> Optional[StreamSample]:
        """
        Pull a single sample.
        
        Args:
            timeout: Timeout in seconds (0 = non-blocking)
            
        Returns:
            StreamSample or None
        """
        start_time = time.time()
        
        while True:
            with self._lock:
                if self._buffer:
                    return self._buffer.pop(0)
            
            if timeout <= 0:
                return None
            
            if time.time() - start_time >= timeout:
                return None
            
            time.sleep(0.001)
    
    def pull_chunk(self, timeout: float = 0.0, max_samples: int = 0) -> List[StreamSample]:
        """
        Pull multiple samples.
        
        Args:
            timeout: Timeout in seconds
            max_samples: Maximum samples to return (0 = all available)
            
        Returns:
            List of StreamSample
        """
        samples = []
        start_time = time.time()
        
        while True:
            sample = self.pull_sample(0.0)  # Non-blocking
            if sample:
                samples.append(sample)
                if max_samples > 0 and len(samples) >= max_samples:
                    break
            else:
                if samples or timeout <= 0:
                    break
                if time.time() - start_time >= timeout:
                    break
                time.sleep(0.001)
        
        return samples
    
    def _receive_stream_info(self) -> None:
        """Receive stream info from outlet."""
        try:
            header = self._socket.recv(4)
            if len(header) != 4:
                raise Exception("Failed to receive header")
            
            length = struct.unpack('!I', header)[0]
            data = self._socket.recv(length)
            
            if len(data) != length:
                raise Exception("Failed to receive complete stream info")
            
            info_dict = json.loads(data.decode('utf-8'))
            # Update our info with received data
            self.info.description = info_dict.get('description', '')
            
        except Exception as e:
            raise Exception(f"Failed to receive stream info: {e}")
    
    def _receive_loop(self) -> None:
        """Receive samples from outlet."""
        while self.connected:
            try:
                if not self._socket:
                    break
                
                # Receive header
                header = self._socket.recv(4)
                if len(header) != 4:
                    break
                
                length = struct.unpack('!I', header)[0]
                
                # Receive data
                data = b''
                while len(data) < length:
                    chunk = self._socket.recv(length - len(data))
                    if not chunk:
                        break
                    data += chunk
                
                if len(data) != length:
                    break
                
                # Parse samples
                samples_data = json.loads(data.decode('utf-8'))
                samples = []
                
                for sample_dict in samples_data:
                    sample = StreamSample(
                        data=sample_dict['data'],
                        timestamp=sample_dict['timestamp'],
                        stream_id=sample_dict['stream_id'],
                        sequence_number=sample_dict['sequence_number']
                    )
                    samples.append(sample)
                
                # Add to buffer
                with self._lock:
                    self._buffer.extend(samples)
                    
                    # Limit buffer size
                    if len(self._buffer) > self.max_buflen:
                        self._buffer = self._buffer[-self.max_buflen:]
            
            except Exception as e:
                if self.connected:
                    self.logger.error(f"Error in receive loop: {e}")
                break


class LSLSynchronizer:
    """LSL-style synchronizer with time correction."""
    
    def __init__(self, master_sync: MasterClockSynchronizer):
        """
        Initialize LSL synchronizer.
        
        Args:
            master_sync: Master clock synchronizer
        """
        self.master_sync = master_sync
        self.logger = logging.getLogger(__name__)
        
        # Stream registry
        self.outlets: Dict[str, StreamOutlet] = {}
        self.inlets: Dict[str, StreamInlet] = {}
        
        # Discovery service
        self._discovery_socket = None
        self._discovery_thread = None
        self._discovery_port = 16571  # Standard LSL discovery port + offset
        
        self.logger.info("LSL synchronizer initialized")
    
    def start_discovery_service(self) -> bool:
        """Start stream discovery service."""
        try:
            self._discovery_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self._discovery_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self._discovery_socket.bind(('0.0.0.0', self._discovery_port))
            
            self._discovery_thread = threading.Thread(target=self._discovery_loop, daemon=True)
            self._discovery_thread.start()
            
            self.logger.info(f"Discovery service started on port {self._discovery_port}")
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to start discovery service: {e}")
            return False
    
    def stop_discovery_service(self) -> None:
        """Stop discovery service."""
        if self._discovery_socket:
            self._discovery_socket.close()
            self._discovery_socket = None
        
        if self._discovery_thread and self._discovery_thread.is_alive():
            self._discovery_thread.join(timeout=2.0)
    
    def create_outlet(self, info: StreamInfo) -> StreamOutlet:
        """Create and register stream outlet."""
        outlet = StreamOutlet(info)
        self.outlets[info.source_id] = outlet
        self.logger.info(f"Created outlet: {info.name}")
        return outlet
    
    def create_inlet(self, info: StreamInfo) -> StreamInlet:
        """Create stream inlet."""
        inlet = StreamInlet(info)
        self.inlets[info.source_id] = inlet
        self.logger.info(f"Created inlet: {info.name}")
        return inlet
    
    def resolve_streams(self, name: Optional[str] = None, type: Optional[str] = None) -> List[StreamInfo]:
        """Resolve available streams."""
        # This would implement stream discovery
        # For now, return registered outlets
        streams = []
        for outlet in self.outlets.values():
            if name and outlet.info.name != name:
                continue
            if type and outlet.info.type != type:
                continue
            streams.append(outlet.info)
        
        return streams
    
    def local_clock(self) -> float:
        """Get local clock time."""
        return time.time()
    
    def _discovery_loop(self) -> None:
        """Handle discovery requests."""
        while self._discovery_socket:
            try:
                data, addr = self._discovery_socket.recvfrom(1024)
                request = json.loads(data.decode('utf-8'))
                
                if request.get('type') == 'resolve_streams':
                    # Send back available streams
                    streams = self.resolve_streams(
                        request.get('name'),
                        request.get('stream_type')
                    )
                    
                    response = {
                        'type': 'stream_list',
                        'streams': [s.to_dict() for s in streams]
                    }
                    
                    response_data = json.dumps(response).encode('utf-8')
                    self._discovery_socket.sendto(response_data, addr)
            
            except Exception as e:
                if self._discovery_socket:
                    self.logger.error(f"Error in discovery loop: {e}")
                break