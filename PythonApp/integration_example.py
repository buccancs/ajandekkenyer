#!/usr/bin/env python3
"""
Complete integration example showing PyShimmer + NTP synchronization + LSL streaming.
Demonstrates the full synchronized multi-device recording workflow.
"""

import sys
import logging
import time
from pathlib import Path

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent))

from src.core.logging_setup import setup_logging
from src.core.config_manager import ConfigManager
from src.sensors.shimmer_manager import ShimmerManager
from src.master_clock_synchronizer import MasterClockSynchronizer, DeviceInfo, DeviceType
from src.lsl_synchronizer import LSLSynchronizer
from src.session_synchronizer import SessionSynchronizer, SessionConfig


def main():
    """Demonstrate complete integrated workflow."""
    print("Multi-Device Synchronization Integration Example")
    print("=" * 60)
    
    # Setup logging
    setup_logging(level="INFO", debug=False)
    logger = logging.getLogger(__name__)
    
    try:
        # 1. Load configuration
        print("\n1. Loading system configuration...")
        config_manager = ConfigManager("config/config.yaml")
        config = config_manager.load_config()
        print("   ✓ Configuration loaded")
        
        # 2. Initialize synchronization components
        print("\n2. Initializing synchronization system...")
        
        # Master clock synchronizer with NTP server
        master_sync = MasterClockSynchronizer(ntp_port=config.sync.ntp_server_port)
        if master_sync.start_synchronization_service():
            print("   ✓ Master clock synchronizer started")
        
        # LSL synchronizer for streaming
        lsl_sync = LSLSynchronizer(master_sync)
        if lsl_sync.start_discovery_service():
            print("   ✓ LSL synchronizer started")
        
        # Session synchronizer for coordinated recording
        session_sync = SessionSynchronizer(master_sync, lsl_sync)
        print("   ✓ Session synchronizer initialized")
        
        # 3. Initialize Shimmer manager with LSL integration
        print("\n3. Initializing Shimmer manager...")
        shimmer_manager = ShimmerManager(config, lsl_synchronizer=lsl_sync)
        shimmer_manager.start()
        print("   ✓ Shimmer manager started")
        
        # 4. Discover and connect to devices
        print("\n4. Discovering devices...")
        shimmer_devices = shimmer_manager.scan_devices()
        print(f"   ✓ Found {len(shimmer_devices)} Shimmer devices")
        
        connected_devices = []
        for device in shimmer_devices:
            if shimmer_manager.connect_device(device.device_id):
                print(f"   ✓ Connected to {device.name}")
                
                # Create LSL streams for the device
                if shimmer_manager.create_lsl_streams(device.device_id):
                    print(f"   ✓ Created LSL streams for {device.name}")
                
                # Register with synchronization system
                device_info = DeviceInfo(
                    device_id=device.device_id,
                    device_type=DeviceType.SHIMMER,
                    name=device.name,
                    address=device.mac_address or "simulation",
                    capabilities={
                        "oscillator_type": "crystal",
                        "sampling_rate": device.sampling_rate,
                        "sensors": device.enabled_sensors
                    }
                )
                
                master_sync.register_device(device_info)
                connected_devices.append(device_info)
                print(f"   ✓ Registered {device.name} with synchronization system")
        
        if not connected_devices:
            print("   ⚠ No devices connected - demo will use simulation")
        
        # 5. Create and execute synchronized recording session
        print("\n5. Creating synchronized recording session...")
        
        session_config = SessionConfig(
            session_id=f"demo_session_{int(time.time())}",
            duration_seconds=10.0,  # 10 second demo
            sync_precision_ms=5.0,
            quality_threshold=0.6  # Lower for demo
        )
        
        if session_sync.prepare_session(session_config, connected_devices):
            print("   ✓ Session prepared")
            
            # Synchronize all devices
            if session_sync.synchronize_devices():
                print("   ✓ Devices synchronized")
                
                # Start coordinated recording
                if session_sync.start_recording():
                    print("   ✓ Recording started")
                    
                    # Monitor session progress
                    print("\n6. Monitoring recording session...")
                    start_time = time.time()
                    
                    while time.time() - start_time < session_config.duration_seconds:
                        status = session_sync.get_session_status()
                        elapsed = time.time() - start_time
                        print(f"   Recording: {elapsed:.1f}s / {session_config.duration_seconds}s "
                              f"({len(status['devices'])} devices)")
                        
                        # Show sync quality for each device
                        for device_id, device_status in status['devices'].items():
                            quality = device_status['sync_quality']
                            streams = device_status['stream_count']
                            print(f"     {device_id}: quality={quality:.3f}, streams={streams}")
                        
                        time.sleep(2.0)
                    
                    # Stop recording
                    if session_sync.stop_recording():
                        print("   ✓ Recording stopped")
        
        # 7. Display final statistics
        print("\n7. Final system status...")
        
        # NTP server stats
        ntp_stats = master_sync.get_ntp_status()
        print(f"   NTP server handled {ntp_stats['requests_handled']} requests")
        print(f"   Uptime: {ntp_stats['uptime_seconds']:.1f} seconds")
        
        # LSL stream stats
        print(f"   LSL outlets created: {len(lsl_sync.outlets)}")
        
        # Device sync quality
        for device_info in connected_devices:
            quality = master_sync.get_sync_quality(device_info.device_id)
            if quality:
                print(f"   {device_info.name}: avg_offset={quality.average_offset_ms:.2f}ms, "
                      f"quality={quality.quality_score:.3f}")
        
        print("\n8. Cleaning up...")
        
        # Stop streaming and disconnect devices
        for device in shimmer_devices:
            if device.is_connected:
                shimmer_manager.stop_lsl_streams(device.device_id)
                shimmer_manager.disconnect_device(device.device_id)
        
        # Stop all services
        shimmer_manager.stop()
        lsl_sync.stop_discovery_service()
        master_sync.stop_synchronization_service()
        
        print("   ✓ All services stopped")
        
        print("\n" + "=" * 60)
        print("✅ Integration example completed successfully!")
        print()
        print("Key Features Demonstrated:")
        print("  • PyShimmer integration with automatic library detection")
        print("  • NTP-like time synchronization with drift compensation")
        print("  • LSL-compatible streaming with precise timestamps")
        print("  • Coordinated multi-device recording sessions")
        print("  • Real-time quality monitoring and re-synchronization")
        print()
        print("The system is ready for:")
        print("  • Real PyShimmer hardware (when libraries are installed)")
        print("  • Multi-device synchronized data collection")
        print("  • High-precision timing research applications")
        
        return True
        
    except Exception as e:
        logger.error(f"Integration example failed: {e}", exc_info=True)
        return False


if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)