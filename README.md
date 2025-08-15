# Multi-Sensor Recording System for Contactless GSR Prediction Research

A comprehensive platform for synchronized physiological data collection, integrating Shimmer3 GSR+ sensors, thermal cameras, and RGB cameras for contactless stress monitoring research.

## Multi-App Project Structure

This project is organized as a **multi-app Gradle project** containing both Android Kotlin and Python desktop applications:

```
ajandekkenyer/                    # Root project
├── AndroidApp/                   # Android Kotlin application module
│   ├── app/                      # Main Android app
│   │   ├── src/main/kotlin/      # Kotlin source code
│   │   ├── src/main/res/         # Android resources
│   │   └── build.gradle.kts      # Android app build configuration
│   └── build.gradle.kts          # Android module configuration
├── PythonApp/                    # Python desktop controller module
│   ├── src/                      # Python source code
│   ├── main.py                   # Python application entry point
│   ├── requirements.txt          # Python dependencies
│   └── build.gradle.kts          # Python tasks configuration
├── build.gradle.kts              # Root project configuration
├── settings.gradle.kts           # Multi-module project settings
└── gradle/                       # Gradle wrapper and configuration
```

## System Overview

This system implements a distributed architecture for multi-modal physiological data collection as described in the academic documentation. It consists of:

- **Python Desktop Controller**: Master coordination and data management
- **Android Mobile Applications**: Sensor nodes for camera and GSR data collection  
- **Network Communication**: JSON-based protocol for device synchronization
- **Time Synchronization**: NTP-based precise timing alignment
- **Data Management**: Session-based recording with metadata tracking

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    PC Desktop Controller                    │
│  ┌─────────────────┐ ┌─────────────────┐ ┌───────────────┐ │
│  │  Session        │ │   Device        │ │   Shimmer     │ │
│  │  Manager        │ │   Manager       │ │   Manager     │ │
│  └─────────────────┘ └─────────────────┘ └───────────────┘ │
│  ┌─────────────────┐ ┌─────────────────┐ ┌───────────────┐ │
│  │  GUI Interface  │ │ Network Server  │ │ Time Sync     │ │
│  │  (PyQt5)        │ │ (TCP/JSON)      │ │ (NTP)         │ │
│  └─────────────────┘ └─────────────────┘ └───────────────┘ │
└─────────────────────────────────────────────────────────────┘
                                │
                                │ Network Communication
                                │ (JSON over TCP)
                                │
┌─────────────────────────────────────────────────────────────┐
│                 Android Mobile Applications                 │
│  ┌─────────────────┐ ┌─────────────────┐ ┌───────────────┐ │
│  │  Recording      │ │   Camera        │ │   Thermal     │ │
│  │  Controller     │ │   Manager       │ │   Camera      │ │
│  └─────────────────┘ └─────────────────┘ └───────────────┘ │
│  ┌─────────────────┐ ┌─────────────────┐ ┌───────────────┐ │
│  │  Network        │ │ File Transfer   │ │   Shimmer     │ │
│  │  Client         │ │ Manager         │ │   Interface   │ │
│  └─────────────────┘ └─────────────────┘ └───────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Quick Start

### Build All Applications
```bash
# Build both Android and Python applications
./gradlew buildAll

# Clean all applications
./gradlew cleanAll

# Run all tests
./gradlew testAll
```

### Android Application
```bash
# Build Android app
./gradlew :AndroidApp:app:build

# Install on connected device
./gradlew :AndroidApp:app:installDebug

# Run Android tests
./gradlew :AndroidApp:app:test
```

### Python Desktop Controller
```bash
# Install Python dependencies
./gradlew :PythonApp:installPythonDeps

# Run desktop controller with GUI
./gradlew runDesktop

# Run in headless mode (command-line only)
./gradlew runDesktopHeadless

# Run Python tests
./gradlew :PythonApp:testPython

# Format Python code
./gradlew :PythonApp:formatPython

# Lint Python code
./gradlew :PythonApp:lintPython
```

## Features

### Core Functionality
- [x] Multi-device sensor integration (Shimmer GSR, thermal cameras, RGB cameras)
- [x] Synchronized data recording across all devices
- [x] Real-time data streaming and monitoring
- [x] Session-based data organization with metadata
- [x] Network communication protocol for device coordination
- [x] Time synchronization service for sub-millisecond alignment
- [x] Fault tolerance and automatic device reconnection

### Data Collection
- [x] Shimmer3 GSR+ sensor integration (128 Hz sampling)
- [x] Thermal camera support (Topdon TC-series, 25 FPS)
- [x] RGB video recording (1920x1080, 30 FPS)
- [x] Multi-modal data fusion with synchronized timestamps
- [x] Real-time data validation and quality monitoring
- [x] Automated file transfer and aggregation

### User Interface
- [x] Desktop GUI for session control and monitoring
- [x] Android app for mobile data collection
- [x] Real-time device status displays
- [x] Session management and progress tracking
- [x] Error reporting and diagnostics

### Research Features
- [x] Calibration utilities for camera alignment
- [x] Data export in research-friendly formats (CSV, HDF5)
- [x] Session metadata for reproducibility
- [x] Configurable sampling rates and recording parameters
- [x] Quality assessment and validation tools

## Quick Start

### Prerequisites

**Desktop Controller (Python):**
- Python 3.8+
- PyQt5 for GUI
- Required packages: `pip install -r PythonApp/requirements.txt`

**Mobile Application (Android):**
- Android SDK (API level 26+)
- Kotlin support
- USB OTG capability for thermal camera

**Hardware:**
- Shimmer3 GSR+ sensor
- Topdon TC-series thermal camera
- Android smartphone/tablet
- Network connectivity (WiFi)

### Installation

1. **Clone the repository:**
```bash
git clone https://github.com/buccancs/ajandekkenyer.git
cd ajandekkenyer
```

2. **Setup Python Desktop Controller:**
```bash
cd PythonApp
pip install -r requirements.txt
```

3. **Build Android Application:**
```bash
cd AndroidApp
./gradlew assembleDebug
```

4. **Configure the system:**
```bash
# Copy and edit configuration file
cp config/config.yaml config/config.local.yaml
# Edit config.local.yaml with your network settings
```

### Basic Usage

1. **Start the Desktop Controller:**
```bash
cd PythonApp
python main.py --config ../config/config.local.yaml
```

2. **Install and launch the Android app** on your mobile device

3. **Connect devices:**
   - Ensure desktop and mobile devices are on the same network
   - Pair Shimmer GSR sensor via Bluetooth
   - Connect thermal camera to Android device via USB

4. **Create a recording session:**
   - Click "Start New Session" in the desktop GUI
   - Verify all devices are connected and showing green status
   - Click "Start Recording" to begin data collection
   - Use "Send Sync Signal" to mark events across all streams

5. **Stop and review:**
   - Click "Stop Recording" when finished
   - Data will be automatically transferred and saved
   - Review session metadata and files in the output directory

## Configuration

The system is configured via YAML files in the `config/` directory:

```yaml
# Network Configuration
network:
  host: "0.0.0.0"
  port: 9000
  max_connections: 10
  timeout: 30

# Sensor Configuration  
sensors:
  shimmer_enabled: true
  shimmer_sampling_rate: 128
  thermal_fps: 25
  rgb_fps: 30

# Recording Configuration
recording:
  output_directory: "sessions"
  max_session_duration: 7200  # 2 hours
  auto_save_interval: 60
```

## Data Format

### Session Structure
```
sessions/
└── session_20240815_143022/
    ├── session_metadata.json
    ├── shimmer_gsr_data.csv
    ├── device_android_001/
    │   ├── rgb_video.mp4
    │   ├── thermal_video.mp4
    │   └── thermal_data.csv
    └── sync_events.csv
```

### Data Files
- **GSR Data**: CSV format with timestamp, GSR, PPG, accelerometer, gyroscope
- **Video Data**: MP4 files with embedded timestamps
- **Thermal Data**: Radiometric temperature arrays in CSV format
- **Metadata**: JSON files with session info, device details, calibration data

## Testing

### Core System Tests
```bash
cd PythonApp
python test_core.py
```

### Android Unit Tests
```bash
cd AndroidApp
./gradlew testDebugUnitTest
```

### Integration Tests
```bash
cd PythonApp
python -m pytest tests/
```

## Development

### Project Structure
```
ajandekkenyer/
├── PythonApp/                  # Desktop controller
│   ├── src/
│   │   ├── core/              # Core system components
│   │   ├── gui/               # PyQt5 user interface
│   │   ├── network/           # Device communication
│   │   ├── sensors/           # Sensor management
│   │   └── data/              # Data processing
│   ├── tests/                 # Unit and integration tests
│   └── requirements.txt       # Python dependencies
├── AndroidApp/                # Mobile application
│   ├── app/src/main/kotlin/   # Kotlin source code
│   ├── app/src/main/res/      # Android resources
│   └── app/build.gradle.kts   # Build configuration
├── config/                    # Configuration files
├── docs/                      # Documentation
└── README.md                  # This file
```

### Adding New Sensors

1. **Create sensor manager** in `PythonApp/src/sensors/`
2. **Implement device interface** in Android app
3. **Add configuration options** in `config.yaml`
4. **Register in device manager** for discovery and connection
5. **Update GUI** to display sensor status

### Extending Data Formats

1. **Define new data structures** in core modules
2. **Update session manager** to handle new file types
3. **Add export functionality** for research formats
4. **Include in metadata schema** for reproducibility

## Troubleshooting

### Common Issues

**Desktop Controller won't start:**
- Check Python version (3.8+ required)
- Install missing dependencies: `pip install -r requirements.txt`
- Verify PyQt5 installation for GUI

**Android app crashes:**
- Check minimum SDK version (API 26+)
- Grant necessary permissions (Camera, Bluetooth, Storage)
- Verify USB OTG support for thermal camera

**Devices won't connect:**
- Ensure all devices on same network
- Check firewall settings (port 9000)
- Verify Bluetooth pairing for Shimmer sensor

**Synchronization issues:**
- Check network latency and stability
- Verify NTP time synchronization
- Review tolerance settings in configuration

**Data quality problems:**
- Check sensor calibration status
- Verify sampling rate configurations
- Review environmental conditions (lighting, temperature)

### Debugging

Enable debug mode in configuration:
```yaml
debug: true
```

Check log files:
```bash
tail -f PythonApp/logs/system.log
```

Use test mode for development:
```bash
python main.py --debug --no-gui
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-sensor`)
3. Make your changes and add tests
4. Ensure all tests pass (`python test_core.py`)
5. Submit a pull request

### Code Style
- Python: Follow PEP 8, use Black formatter
- Kotlin: Follow Android Kotlin style guide
- Documentation: Include docstrings and inline comments
- Testing: Add unit tests for new functionality

## License

This project is developed for academic research purposes. See LICENSE file for details.

## Citation

If you use this system in your research, please cite:

```
@software{multiSensorGSR2024,
  title={Multi-Sensor Recording System for Contactless GSR Prediction Research},
  author={Research Team},
  year={2024},
  url={https://github.com/buccancs/ajandekkenyer}
}
```

## Support

For technical support or research collaboration:
- Create an issue on GitHub
- Check the documentation in `docs/`
- Review the thesis chapters for detailed system design

## Acknowledgments

This work builds upon research in physiological computing, contactless sensing, and multi-modal data fusion. Special thanks to the research community working on affective computing and stress monitoring technologies.