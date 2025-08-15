"""
NTP Time Server implementation for local time synchronization.
Provides high-precision time distribution for multi-device synchronization.
"""

import logging
import socket
import struct
import threading
import time
from typing import Optional, Dict, Any
from dataclasses import dataclass


@dataclass
class NTPConfig:
    """NTP server configuration."""
    port: int = 8889
    precision: str = "microsecond"
    stratum: int = 1
    reference_clock: str = "local_oscillator"
    polling_interval: int = 16
    maximum_distance: int = 100
    maximum_dispersion: int = 50


class NTPTimeServer:
    """Local NTP server for high-precision time distribution."""
    
    # NTP constants
    NTP_PACKET_SIZE = 48
    NTP_EPOCH = 2208988800  # 1900-01-01 00:00:00 - 1970-01-01 00:00:00
    
    def __init__(self, port: int = 8889, config: Optional[NTPConfig] = None):
        """
        Initialize NTP time server.
        
        Args:
            port: UDP port to listen on
            config: NTP server configuration
        """
        self.config = config or NTPConfig(port=port)
        self.logger = logging.getLogger(__name__)
        
        self.socket = None
        self.running = False
        self.server_thread = None
        self._lock = threading.RLock()
        
        # Statistics
        self.requests_handled = 0
        self.start_time = None
        
        self.logger.info(f"NTP time server initialized on port {self.config.port}")
    
    def start(self) -> bool:
        """
        Start the NTP server.
        
        Returns:
            bool: True if started successfully
        """
        with self._lock:
            if self.running:
                self.logger.warning("NTP server already running")
                return True
            
            try:
                # Create UDP socket
                self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                self.socket.bind(('0.0.0.0', self.config.port))
                self.socket.settimeout(1.0)  # Non-blocking with timeout
                
                self.running = True
                self.start_time = time.time()
                
                # Start server thread
                self.server_thread = threading.Thread(target=self._server_loop, daemon=True)
                self.server_thread.start()
                
                self.logger.info(f"NTP server started on port {self.config.port}")
                return True
                
            except Exception as e:
                self.logger.error(f"Failed to start NTP server: {e}")
                self.running = False
                if self.socket:
                    self.socket.close()
                    self.socket = None
                return False
    
    def stop(self) -> None:
        """Stop the NTP server."""
        with self._lock:
            if not self.running:
                return
            
            self.running = False
            
            if self.socket:
                self.socket.close()
                self.socket = None
            
            if self.server_thread and self.server_thread.is_alive():
                self.server_thread.join(timeout=2.0)
            
            self.logger.info("NTP server stopped")
    
    def _server_loop(self) -> None:
        """Main server loop to handle NTP requests."""
        self.logger.debug("NTP server loop started")
        
        try:
            while self.running:
                try:
                    # Receive NTP request
                    data, client_address = self.socket.recvfrom(self.NTP_PACKET_SIZE)
                    receive_time = self._get_ntp_timestamp()
                    
                    # Process request in separate method to avoid blocking
                    response = self._handle_ntp_request(data, receive_time)
                    
                    if response:
                        self.socket.sendto(response, client_address)
                        self.requests_handled += 1
                        
                        self.logger.debug(f"Handled NTP request from {client_address}")
                
                except socket.timeout:
                    # Normal timeout, continue loop
                    continue
                except OSError:
                    # Socket closed
                    break
                except Exception as e:
                    if self.running:
                        self.logger.error(f"Error in NTP server loop: {e}")
        
        except Exception as e:
            self.logger.error(f"Fatal error in NTP server loop: {e}")
        finally:
            self.logger.debug("NTP server loop ended")
    
    def _handle_ntp_request(self, request_data: bytes, receive_time: float) -> Optional[bytes]:
        """
        Handle an NTP request and generate response.
        
        Args:
            request_data: Raw NTP request packet
            receive_time: Time when request was received
            
        Returns:
            bytes: NTP response packet or None if invalid
        """
        try:
            if len(request_data) < self.NTP_PACKET_SIZE:
                return None
            
            # Parse request packet
            request = struct.unpack("!12I", request_data)
            
            # Get current time for response
            transmit_time = self._get_ntp_timestamp()
            
            # Extract client transmit time from request
            client_transmit_int = request[10]
            client_transmit_frac = request[11]
            client_transmit_time = client_transmit_int + client_transmit_frac / (2**32)
            
            # Build response packet
            response = bytearray(self.NTP_PACKET_SIZE)
            
            # Leap indicator (0), Version (4), Mode (4 = server)
            response[0] = 0x24
            
            # Stratum
            response[1] = self.config.stratum
            
            # Poll interval
            response[2] = self.config.polling_interval
            
            # Precision (log2 of precision in seconds)
            if self.config.precision == "microsecond":
                response[3] = -20  # 2^-20 ≈ 1 microsecond
            else:
                response[3] = -6   # 2^-6 ≈ 15 milliseconds
            
            # Root delay and dispersion (4 bytes each)
            struct.pack_into("!I", response, 4, 0)  # Root delay
            struct.pack_into("!I", response, 8, 0)  # Root dispersion
            
            # Reference identifier (4 bytes) - "LOCL" for local clock
            response[12:16] = b"LOCL"
            
            # Reference timestamp (last time clock was set)
            ref_time = self._get_ntp_timestamp()
            ref_int = int(ref_time)
            ref_frac = int((ref_time - ref_int) * (2**32))
            struct.pack_into("!II", response, 16, ref_int, ref_frac)
            
            # Origin timestamp (client transmit time from request)
            struct.pack_into("!II", response, 24, client_transmit_int, client_transmit_frac)
            
            # Receive timestamp
            recv_int = int(receive_time)
            recv_frac = int((receive_time - recv_int) * (2**32))
            struct.pack_into("!II", response, 32, recv_int, recv_frac)
            
            # Transmit timestamp
            trans_int = int(transmit_time)
            trans_frac = int((transmit_time - trans_int) * (2**32))
            struct.pack_into("!II", response, 40, trans_int, trans_frac)
            
            return bytes(response)
            
        except Exception as e:
            self.logger.error(f"Error handling NTP request: {e}")
            return None
    
    def _get_ntp_timestamp(self) -> float:
        """
        Get current time as NTP timestamp.
        
        Returns:
            float: NTP timestamp (seconds since 1900-01-01)
        """
        return time.time() + self.NTP_EPOCH
    
    def get_status(self) -> Dict[str, Any]:
        """
        Get server status information.
        
        Returns:
            dict: Status information
        """
        with self._lock:
            uptime = time.time() - self.start_time if self.start_time else 0
            
            return {
                'running': self.running,
                'port': self.config.port,
                'uptime_seconds': uptime,
                'requests_handled': self.requests_handled,
                'stratum': self.config.stratum,
                'precision': self.config.precision
            }