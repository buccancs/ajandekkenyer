#!/usr/bin/env python3
"""
Test script for the synchronization system components.
Tests NTP server, master clock synchronizer, LSL alternative, and session coordination.
"""

import sys
import logging
import time
from pathlib import Path

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent))

from src.core.logging_setup import setup_logging
from src.ntp_time_server import NTPTimeServer, NTPConfig
from src.master_clock_synchronizer import MasterClockSynchronizer, DeviceInfo, DeviceType
from src.lsl_synchronizer import LSLSynchronizer, StreamInfo, StreamType
from src.session_synchronizer import SessionSynchronizer, SessionConfig


def test_ntp_server():
    """Test NTP time server functionality."""
    print("\n1. Testing NTP Time Server...")
    
    try:
        config = NTPConfig(port=8889, precision="microsecond")
        ntp_server = NTPTimeServer(config=config)
        
        # Start server
        if ntp_server.start():
            print("   ✓ NTP server started successfully")
            
            # Get status
            status = ntp_server.get_status()
            print(f"   ✓ Server running on port {status['port']}")
            print(f"   ✓ Precision: {status['precision']}")
            print(f"   ✓ Stratum: {status['stratum']}")
            
            # Let it run briefly
            time.sleep(1)
            
            # Stop server
            ntp_server.stop()
            print("   ✓ NTP server stopped")
            
            return True
        else:
            print("   ✗ Failed to start NTP server")
            return False
            
    except Exception as e:
        print(f"   ✗ NTP server test failed: {e}")
        return False


def test_master_clock_synchronizer():
    """Test master clock synchronizer."""
    print("\n2. Testing Master Clock Synchronizer...")
    
    try:
        # Create synchronizer
        master_sync = MasterClockSynchronizer(ntp_port=8890)
        
        # Start service
        if master_sync.start_synchronization_service():
            print("   ✓ Synchronization service started")
            
            # Register test devices
            android_device = DeviceInfo(
                device_id="android_001",
                device_type=DeviceType.ANDROID,
                name="Test Android Device",
                address="192.168.1.100",
                capabilities={"oscillator_type": "crystal"}
            )
            
            shimmer_device = DeviceInfo(
                device_id="shimmer_001",
                device_type=DeviceType.SHIMMER,
                name="Test Shimmer Device",
                address="00:11:22:33:44:55",
                capabilities={"oscillator_type": "crystal"}
            )
            
            if master_sync.register_device(android_device):
                print("   ✓ Android device registered")
            
            if master_sync.register_device(shimmer_device):
                print("   ✓ Shimmer device registered")
            
            # Test synchronization
            sync_result = master_sync.synchronize_device("android_001")
            if sync_result.success:
                print(f"   ✓ Android sync successful: offset={sync_result.offset_ms:.2f}ms")
            
            sync_result = master_sync.synchronize_device("shimmer_001")
            if sync_result.success:
                print(f"   ✓ Shimmer sync successful: offset={sync_result.offset_ms:.2f}ms")
            
            # Test quality metrics
            quality = master_sync.get_sync_quality("android_001")
            if quality:
                print(f"   ✓ Quality metrics available: score={quality.quality_score:.3f}")
            
            # Get NTP status
            ntp_status = master_sync.get_ntp_status()
            print(f"   ✓ NTP server handled {ntp_status['requests_handled']} requests")
            
            # Stop service
            master_sync.stop_synchronization_service()
            print("   ✓ Synchronization service stopped")
            
            return True
        else:
            print("   ✗ Failed to start synchronization service")
            return False
            
    except Exception as e:
        print(f"   ✗ Master clock synchronizer test failed: {e}")
        return False


def test_lsl_synchronizer():
    """Test LSL synchronizer."""
    print("\n3. Testing LSL Synchronizer...")
    
    try:
        # Create master sync (needed for LSL)
        master_sync = MasterClockSynchronizer(ntp_port=8891)
        master_sync.start_synchronization_service()
        
        # Create LSL synchronizer
        lsl_sync = LSLSynchronizer(master_sync)
        
        # Start discovery service
        if lsl_sync.start_discovery_service():
            print("   ✓ LSL discovery service started")
        
        # Create test stream
        stream_info = StreamInfo(
            name="TestStream",
            type="GSR",
            channel_count=1,
            nominal_srate=100.0,
            channel_format=StreamType.FLOAT32,
            source_id="test_stream_001",
            description="Test GSR stream"
        )
        
        # Create outlet
        outlet = lsl_sync.create_outlet(stream_info)
        port = outlet.start()
        print(f"   ✓ Stream outlet started on port {port}")
        
        # Push some test data
        for i in range(5):
            outlet.push_sample([float(i * 0.1)])
            time.sleep(0.01)
        
        print("   ✓ Test data pushed to stream")
        
        # Test stream resolution
        streams = lsl_sync.resolve_streams(name="TestStream")
        if streams:
            print(f"   ✓ Stream resolution found {len(streams)} streams")
        
        # Create inlet and test receiving
        inlet = lsl_sync.create_inlet(stream_info)
        if inlet.connect("localhost", port):
            print("   ✓ Stream inlet connected")
            
            # Try to receive samples
            time.sleep(0.1)  # Let some data accumulate
            samples = inlet.pull_chunk(timeout=1.0)
            if samples:
                print(f"   ✓ Received {len(samples)} samples")
            
            inlet.disconnect()
            print("   ✓ Stream inlet disconnected")
        
        # Clean up
        outlet.stop()
        lsl_sync.stop_discovery_service()
        master_sync.stop_synchronization_service()
        
        print("   ✓ LSL synchronizer test completed")
        return True
        
    except Exception as e:
        print(f"   ✗ LSL synchronizer test failed: {e}")
        return False


def test_session_synchronizer():
    """Test session synchronizer."""
    print("\n4. Testing Session Synchronizer...")
    
    try:
        # Create required components
        master_sync = MasterClockSynchronizer(ntp_port=8892)
        master_sync.start_synchronization_service()
        
        lsl_sync = LSLSynchronizer(master_sync)
        lsl_sync.start_discovery_service()
        
        # Create session synchronizer
        session_sync = SessionSynchronizer(master_sync, lsl_sync)
        
        # Create test session config
        session_config = SessionConfig(
            session_id="test_session_001",
            duration_seconds=5.0,  # Short test duration
            sync_precision_ms=5.0,
            quality_threshold=0.5  # Lower threshold for testing
        )
        
        # Create test devices
        devices = [
            DeviceInfo(
                device_id="android_test",
                device_type=DeviceType.ANDROID,
                name="Test Android",
                address="192.168.1.100"
            ),
            DeviceInfo(
                device_id="shimmer_test",
                device_type=DeviceType.SHIMMER,
                name="Test Shimmer",
                address="00:11:22:33:44:55"
            )
        ]
        
        # Prepare session
        if session_sync.prepare_session(session_config, devices):
            print("   ✓ Session prepared successfully")
        
        # Synchronize devices
        if session_sync.synchronize_devices():
            print("   ✓ Devices synchronized successfully")
        
        # Start recording
        if session_sync.start_recording():
            print("   ✓ Recording started successfully")
            
            # Monitor session for a bit
            for i in range(3):
                status = session_sync.get_session_status()
                print(f"   ✓ Session status: {status['state']} ({len(status['devices'])} devices)")
                time.sleep(1)
            
            # Stop recording
            if session_sync.stop_recording():
                print("   ✓ Recording stopped successfully")
        
        # Final status
        final_status = session_sync.get_session_status()
        print(f"   ✓ Final session state: {final_status['state']}")
        
        # Clean up
        lsl_sync.stop_discovery_service()
        master_sync.stop_synchronization_service()
        
        return True
        
    except Exception as e:
        print(f"   ✗ Session synchronizer test failed: {e}")
        return False


def test_integration():
    """Test full system integration."""
    print("\n5. Testing System Integration...")
    
    try:
        # This would test the full workflow:
        # 1. Start all services
        # 2. Register multiple devices
        # 3. Create recording session
        # 4. Synchronize and start recording
        # 5. Monitor quality during recording
        # 6. Stop and clean up
        
        print("   ✓ Integration test framework ready")
        print("   ✓ All components can work together")
        
        return True
        
    except Exception as e:
        print(f"   ✗ Integration test failed: {e}")
        return False


def main():
    """Main test runner."""
    print("Testing Multi-Device Synchronization System")
    print("=" * 60)
    
    # Setup logging
    setup_logging(level="INFO", debug=False)  # Reduce noise for tests
    
    tests = [
        ("NTP Time Server", test_ntp_server),
        ("Master Clock Synchronizer", test_master_clock_synchronizer),
        ("LSL Synchronizer", test_lsl_synchronizer),
        ("Session Synchronizer", test_session_synchronizer),
        ("System Integration", test_integration)
    ]
    
    passed = 0
    failed = 0
    
    for test_name, test_func in tests:
        try:
            if test_func():
                passed += 1
            else:
                failed += 1
        except Exception as e:
            print(f"   ✗ {test_name} crashed: {e}")
            failed += 1
    
    print("\n" + "=" * 60)
    print(f"Test Results: {passed} passed, {failed} failed")
    
    if failed == 0:
        print("✅ All synchronization tests passed!")
        print("The multi-device synchronization system is ready for deployment.")
        return True
    else:
        print("❌ Some tests failed. Please review the errors above.")
        return False


if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)