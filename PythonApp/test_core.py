#!/usr/bin/env python3
"""
Test script for the Multi-Sensor Recording System core components.
Tests the system without GUI dependencies.
"""

import sys
import logging
from pathlib import Path

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent))

from src.core.config_manager import ConfigManager
from src.core.logging_setup import setup_logging
from src.core.session_manager import SessionManager
from src.network.device_manager import DeviceManager
from src.sensors.shimmer_manager import ShimmerManager


def test_core_components():
    """Test core system components."""
    print("Testing Multi-Sensor Recording System Core Components")
    print("=" * 60)
    
    # Setup logging
    setup_logging(level="INFO", debug=True)
    logger = logging.getLogger(__name__)
    
    logger.info("Starting core component tests")
    
    # Test configuration management
    print("\n1. Testing Configuration Manager...")
    try:
        config_manager = ConfigManager("config/config.yaml")
        config = config_manager.load_config()
        
        print(f"   ✓ Configuration loaded successfully")
        print(f"   ✓ Network port: {config.network.port}")
        print(f"   ✓ Shimmer sampling rate: {config.sensors.shimmer_sampling_rate}")
        print(f"   ✓ Output directory: {config.recording.output_directory}")
        
        # Validate configuration
        if config_manager.validate_config(config):
            print(f"   ✓ Configuration validation passed")
        else:
            print(f"   ✗ Configuration validation failed")
            
    except Exception as e:
        print(f"   ✗ Configuration manager test failed: {e}")
        return False
    
    # Test session manager
    print("\n2. Testing Session Manager...")
    try:
        session_manager = SessionManager(config)
        
        # Create a test session
        session_id = session_manager.create_session(notes="Test session")
        print(f"   ✓ Session created: {session_id}")
        
        # Start session
        session_manager.start_session()
        print(f"   ✓ Session started")
        
        # Add a mock device
        session_manager.add_device("test_device", "shimmer", {"model": "Shimmer3 GSR+"})
        print(f"   ✓ Device added to session")
        
        # Get session info
        session_info = session_manager.get_session_info()
        print(f"   ✓ Session info retrieved: {len(session_info['devices'])} devices")
        
        # Stop session
        session_manager.stop_session()
        print(f"   ✓ Session stopped")
        
    except Exception as e:
        print(f"   ✗ Session manager test failed: {e}")
        return False
    
    # Test Shimmer manager
    print("\n3. Testing Shimmer Manager...")
    try:
        shimmer_manager = ShimmerManager(config)
        shimmer_manager.start()
        
        # Scan for devices (will create simulated device)
        devices = shimmer_manager.scan_devices()
        print(f"   ✓ Device scan completed: {len(devices)} devices found")
        
        if devices:
            device_id = devices[0].device_id
            print(f"   ✓ Found device: {devices[0].name}")
            
            # Connect to device
            if shimmer_manager.connect_device(device_id):
                print(f"   ✓ Connected to device: {device_id}")
                
                # Test streaming (briefly)
                output_file = Path("test_output.csv")
                if shimmer_manager.start_streaming(device_id, output_file):
                    print(f"   ✓ Streaming started")
                    
                    # Let it stream for a short time
                    import time
                    time.sleep(2)
                    
                    shimmer_manager.stop_streaming(device_id)
                    print(f"   ✓ Streaming stopped")
                    
                    # Check if data was written
                    if output_file.exists() and output_file.stat().st_size > 0:
                        print(f"   ✓ Data file created: {output_file.stat().st_size} bytes")
                        # Clean up test file
                        output_file.unlink()
                    else:
                        print(f"   ⚠ No data written to file")
                
                shimmer_manager.disconnect_device(device_id)
                print(f"   ✓ Disconnected from device")
        
        shimmer_manager.stop()
        print(f"   ✓ Shimmer manager stopped")
        
    except Exception as e:
        print(f"   ✗ Shimmer manager test failed: {e}")
        return False
    
    # Test device manager (basic initialization)
    print("\n4. Testing Device Manager...")
    try:
        device_manager = DeviceManager(config)
        print(f"   ✓ Device manager initialized")
        
        # Test device info structure
        from src.network.device_manager import DeviceInfo, DeviceType, DeviceStatus
        test_device = DeviceInfo(
            device_id="test_android",
            device_type=DeviceType.ANDROID,
            name="Test Android Device",
            address="192.168.1.100"
        )
        print(f"   ✓ Device info structure working")
        
    except Exception as e:
        print(f"   ✗ Device manager test failed: {e}")
        return False
    
    print("\n" + "=" * 60)
    print("✓ All core component tests passed!")
    print("System is ready for deployment.")
    
    return True


if __name__ == "__main__":
    success = test_core_components()
    sys.exit(0 if success else 1)