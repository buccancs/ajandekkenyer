#!/usr/bin/env python3
"""
Multi-Sensor Recording System - Desktop Controller
Main application entry point for the Python desktop controller.

This application coordinates multiple sensor devices (Shimmer GSR, thermal cameras, 
RGB cameras) for synchronized physiological data collection.
"""

import sys
import logging
import argparse
from pathlib import Path
from PyQt5.QtWidgets import QApplication
from PyQt5.QtCore import Qt

# Add project root to path for imports
sys.path.insert(0, str(Path(__file__).parent))

from src.gui.main_window import MainWindow
from src.core.config_manager import ConfigManager
from src.core.logging_setup import setup_logging


def parse_arguments():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(
        description="Multi-Sensor Recording System Desktop Controller"
    )
    parser.add_argument(
        "--config", 
        type=str, 
        default="config/config.yaml",
        help="Path to configuration file"
    )
    parser.add_argument(
        "--log-level",
        type=str,
        default="INFO",
        choices=["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"],
        help="Set logging level"
    )
    parser.add_argument(
        "--no-gui",
        action="store_true",
        help="Run in headless mode (command line only)"
    )
    parser.add_argument(
        "--debug",
        action="store_true",
        help="Enable debug mode"
    )
    return parser.parse_args()


def main():
    """Main application entry point."""
    args = parse_arguments()
    
    # Setup logging
    setup_logging(level=args.log_level, debug=args.debug)
    logger = logging.getLogger(__name__)
    
    logger.info("Starting Multi-Sensor Recording System Desktop Controller")
    
    try:
        # Load configuration
        config_manager = ConfigManager(args.config)
        config = config_manager.load_config()
        
        if args.no_gui:
            logger.info("Running in headless mode")
            
            # Implement headless mode for automated operation
            try:
                from src.core.headless_controller import HeadlessController
                
                # Create headless controller
                controller = HeadlessController(config)
                
                # Start the controller
                logger.info("Starting headless controller...")
                result = controller.run()
                
                logger.info("Headless mode completed")
                return result
                
            except ImportError:
                # If headless controller not available, provide basic functionality
                logger.warning("Headless controller not implemented, creating basic version")
                
                print("Multi-Sensor Recording System - Headless Mode")
                print("=" * 50)
                print(f"Configuration loaded from: {args.config}")
                print(f"Network port: {config.get('network', {}).get('port', 9000)}")
                print(f"Output directory: {config.get('recording', {}).get('output_directory', 'sessions')}")
                print("\nHeadless mode features:")
                print("- Network server for device connections")
                print("- Automated session management")
                print("- Command-line session control")
                print("- Background data collection")
                
                # Start basic network service
                from src.network.device_manager import DeviceManager
                device_manager = DeviceManager(config)
                
                print(f"\nStarting network service on port {config.get('network', {}).get('port', 9000)}...")
                device_manager.start()
                
                print("Press Ctrl+C to stop the service")
                try:
                    import signal
                    import time
                    
                    def signal_handler(sig, frame):
                        print("\nShutting down...")
                        device_manager.stop()
                        print("Service stopped")
                        exit(0)
                    
                    signal.signal(signal.SIGINT, signal_handler)
                    
                    # Keep running
                    while True:
                        time.sleep(1)
                        
                except KeyboardInterrupt:
                    print("\nShutting down...")
                    device_manager.stop()
                    print("Service stopped")
                    
                return 0
        
        # Create Qt application
        app = QApplication(sys.argv)
        app.setApplicationName("Multi-Sensor Recording System")
        app.setApplicationVersion("1.0.0")
        
        # Set application properties
        if hasattr(Qt, 'AA_EnableHighDpiScaling'):
            app.setAttribute(Qt.AA_EnableHighDpiScaling, True)
        if hasattr(Qt, 'AA_UseHighDpiPixmaps'):
            app.setAttribute(Qt.AA_UseHighDpiPixmaps, True)
        
        # Create and show main window
        main_window = MainWindow(config)
        main_window.show()
        
        logger.info("Application initialized successfully")
        
        # Run application event loop
        return app.exec_()
        
    except Exception as e:
        logger.critical(f"Failed to start application: {e}", exc_info=True)
        return 1


if __name__ == "__main__":
    sys.exit(main())