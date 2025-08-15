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
            # TODO: Implement headless mode for automated operation
            print("Headless mode not yet implemented")
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