"""
Headless controller for automated operation of the Multi-Sensor Recording System.
Provides command-line interface and automated session management without GUI.
"""

import logging
import time
import signal
import sys
from typing import Optional, Dict, Any
from pathlib import Path

from .session_manager import SessionManager
from .config_manager import SystemConfig
from ..network.device_manager import DeviceManager
from ..sensors.shimmer_manager import ShimmerManager


class HeadlessController:
    """Headless controller for automated system operation."""
    
    def __init__(self, config: SystemConfig):
        """
        Initialize headless controller.
        
        Args:
            config: System configuration
        """
        self.config = config
        self.logger = logging.getLogger(__name__)
        
        # Core managers
        self.session_manager: Optional[SessionManager] = None
        self.device_manager: Optional[DeviceManager] = None
        self.shimmer_manager: Optional[ShimmerManager] = None
        
        # State
        self.running = False
        self.current_session_id: Optional[str] = None
        
        # Signal handling
        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)
        
        self.logger.info("Headless controller initialized")
    
    def _signal_handler(self, sig, frame):
        """Handle shutdown signals."""
        self.logger.info(f"Received signal {sig}, shutting down...")
        self.stop()
        sys.exit(0)
    
    def run(self) -> int:
        """
        Run the headless controller.
        
        Returns:
            int: Exit code
        """
        try:
            self.logger.info("Starting headless controller")
            
            # Initialize managers
            self._init_managers()
            
            # Start services
            self._start_services()
            
            # Print status
            self._print_status()
            
            # Run main loop
            self.running = True
            self._main_loop()
            
            return 0
            
        except Exception as e:
            self.logger.error(f"Headless controller failed: {e}", exc_info=True)
            return 1
        finally:
            self.stop()
    
    def _init_managers(self):
        """Initialize core system managers."""
        self.logger.info("Initializing managers...")
        
        # Initialize session manager
        self.session_manager = SessionManager(self.config)
        
        # Initialize device manager
        self.device_manager = DeviceManager(self.config)
        
        # Initialize Shimmer manager
        self.shimmer_manager = ShimmerManager(self.config)
        
        self.logger.info("Managers initialized")
    
    def _start_services(self):
        """Start system services."""
        self.logger.info("Starting services...")
        
        # Start device manager (network service)
        if self.device_manager:
            self.device_manager.start()
            
        # Start Shimmer manager
        if self.shimmer_manager:
            self.shimmer_manager.start()
            
        self.logger.info("Services started")
    
    def _print_status(self):
        """Print system status."""
        print("\nMulti-Sensor Recording System - Headless Mode")
        print("=" * 60)
        print(f"Network Port: {self.config.get('network', {}).get('port', 9000)}")
        print(f"Output Directory: {self.config.get('recording', {}).get('output_directory', 'sessions')}")
        print(f"Shimmer Enabled: {self.config.get('sensors', {}).get('shimmer_enabled', True)}")
        print(f"Debug Mode: {self.config.get('debug', False)}")
        print("\nServices:")
        print("  ✓ Network server running")
        print("  ✓ Device manager active")
        print("  ✓ Shimmer manager ready")
        print("\nCommands available:")
        print("  Ctrl+C - Stop system")
        print("  Check logs for device connections and session activity")
        print("=" * 60)
    
    def _main_loop(self):
        """Main execution loop."""
        self.logger.info("Entering main loop")
        
        try:
            while self.running:
                # Check for connected devices
                self._check_device_status()
                
                # Auto-manage sessions if devices are connected
                self._auto_manage_sessions()
                
                # Sleep for a short interval
                time.sleep(5)
                
        except KeyboardInterrupt:
            self.logger.info("Received keyboard interrupt")
        except Exception as e:
            self.logger.error(f"Main loop error: {e}", exc_info=True)
    
    def _check_device_status(self):
        """Check and log device connection status."""
        if not self.device_manager or not self.shimmer_manager:
            return
            
        # Check network devices
        connected_devices = self.device_manager.get_connected_devices()
        
        # Check Shimmer devices  
        shimmer_devices = self.shimmer_manager.get_devices()
        connected_shimmers = [d for d in shimmer_devices.values() if d.is_connected]
        
        total_connected = len(connected_devices) + len(connected_shimmers)
        
        if total_connected > 0:
            if not hasattr(self, '_last_device_count') or self._last_device_count != total_connected:
                self.logger.info(f"Connected devices: {len(connected_devices)} network, {len(connected_shimmers)} Shimmer")
                self._last_device_count = total_connected
        else:
            if not hasattr(self, '_last_device_count') or self._last_device_count != 0:
                self.logger.info("No devices connected - waiting for connections...")
                self._last_device_count = 0
    
    def _auto_manage_sessions(self):
        """Automatically manage recording sessions based on device availability."""
        if not self.session_manager:
            return
            
        # Get connected devices
        connected_devices = []
        if self.device_manager:
            connected_devices.extend(self.device_manager.get_connected_devices())
        
        connected_shimmers = []
        if self.shimmer_manager:
            shimmer_devices = self.shimmer_manager.get_devices()
            connected_shimmers = [d for d in shimmer_devices.values() if d.is_connected]
        
        total_connected = len(connected_devices) + len(connected_shimmers)
        
        # Auto-start session if devices are connected and no session is active
        if total_connected > 0 and not self.current_session_id:
            self.logger.info("Auto-starting session - devices detected")
            self.current_session_id = self.session_manager.create_session()
            
            if self.session_manager.start_session(self.current_session_id):
                self.logger.info(f"Auto-started session: {self.current_session_id}")
                
                # Start recording automatically
                if self.session_manager.start_recording(self.current_session_id):
                    self.logger.info("Auto-started recording")
            else:
                self.logger.error("Failed to auto-start session")
                self.current_session_id = None
        
        # Auto-stop session if all devices disconnect
        elif total_connected == 0 and self.current_session_id:
            self.logger.info("Auto-stopping session - no devices connected")
            
            # Stop recording first
            self.session_manager.stop_recording(self.current_session_id)
            
            # Stop session
            if self.session_manager.stop_session(self.current_session_id):
                self.logger.info(f"Auto-stopped session: {self.current_session_id}")
                self.current_session_id = None
            else:
                self.logger.error("Failed to auto-stop session")
    
    def stop(self):
        """Stop the headless controller."""
        if not self.running:
            return
            
        self.logger.info("Stopping headless controller...")
        self.running = False
        
        # Stop current session if active
        if self.current_session_id and self.session_manager:
            self.session_manager.stop_recording(self.current_session_id)
            self.session_manager.stop_session(self.current_session_id)
        
        # Stop managers
        if self.device_manager:
            self.device_manager.stop()
            
        if self.shimmer_manager:
            self.shimmer_manager.stop()
            
        self.logger.info("Headless controller stopped")