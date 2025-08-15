"""
Core configuration management for the Multi-Sensor Recording System.
Handles loading and validation of system configuration.
"""

import yaml
import logging
from pathlib import Path
from typing import Dict, Any, Optional
from dataclasses import dataclass, field


@dataclass
class NetworkConfig:
    """Network configuration settings."""
    host: str = "0.0.0.0"
    port: int = 9000
    max_connections: int = 10
    timeout: int = 30
    use_tls: bool = False
    auth_token_length: int = 32


@dataclass
class SensorConfig:
    """Sensor configuration settings."""
    shimmer_enabled: bool = True
    shimmer_sampling_rate: int = 128
    thermal_camera_enabled: bool = True
    thermal_fps: int = 25
    rgb_camera_enabled: bool = True
    rgb_fps: int = 30


@dataclass
class SyncConfig:
    """Time synchronization configuration."""
    ntp_server_port: int = 8889
    sync_interval: int = 60
    tolerance_ms: int = 5
    max_drift_ms: int = 100


@dataclass
class RecordingConfig:
    """Recording session configuration."""
    output_directory: str = "sessions"
    max_session_duration: int = 7200  # 2 hours in seconds
    auto_save_interval: int = 60
    compression_enabled: bool = True


@dataclass
class SystemConfig:
    """Complete system configuration."""
    network: NetworkConfig = field(default_factory=NetworkConfig)
    sensors: SensorConfig = field(default_factory=SensorConfig)
    sync: SyncConfig = field(default_factory=SyncConfig)
    recording: RecordingConfig = field(default_factory=RecordingConfig)
    debug: bool = False


class ConfigManager:
    """Manages system configuration loading and validation."""
    
    def __init__(self, config_path: str = "config/config.yaml"):
        """
        Initialize configuration manager.
        
        Args:
            config_path: Path to the configuration file
        """
        self.config_path = Path(config_path)
        self.logger = logging.getLogger(__name__)
        
    def load_config(self) -> SystemConfig:
        """
        Load configuration from file or create default.
        
        Returns:
            SystemConfig: Loaded or default configuration
        """
        if self.config_path.exists():
            try:
                with open(self.config_path, 'r') as f:
                    config_data = yaml.safe_load(f)
                
                config = self._create_config_from_dict(config_data or {})
                self.logger.info(f"Configuration loaded from {self.config_path}")
                return config
                
            except Exception as e:
                self.logger.error(f"Failed to load config from {self.config_path}: {e}")
                self.logger.info("Using default configuration")
                return SystemConfig()
        else:
            self.logger.info(f"Config file {self.config_path} not found, using defaults")
            config = SystemConfig()
            self._save_default_config(config)
            return config
    
    def _create_config_from_dict(self, config_data: Dict[str, Any]) -> SystemConfig:
        """
        Create SystemConfig from dictionary data.
        
        Args:
            config_data: Dictionary containing configuration data
            
        Returns:
            SystemConfig: Created configuration object
        """
        network_data = config_data.get('network', {})
        sensors_data = config_data.get('sensors', {})
        sync_data = config_data.get('sync', {})
        recording_data = config_data.get('recording', {})
        
        return SystemConfig(
            network=NetworkConfig(**network_data),
            sensors=SensorConfig(**sensors_data),
            sync=SyncConfig(**sync_data),
            recording=RecordingConfig(**recording_data),
            debug=config_data.get('debug', False)
        )
    
    def _save_default_config(self, config: SystemConfig) -> None:
        """
        Save default configuration to file.
        
        Args:
            config: Configuration to save
        """
        try:
            self.config_path.parent.mkdir(parents=True, exist_ok=True)
            
            config_dict = {
                'network': {
                    'host': config.network.host,
                    'port': config.network.port,
                    'max_connections': config.network.max_connections,
                    'timeout': config.network.timeout,
                    'use_tls': config.network.use_tls,
                    'auth_token_length': config.network.auth_token_length
                },
                'sensors': {
                    'shimmer_enabled': config.sensors.shimmer_enabled,
                    'shimmer_sampling_rate': config.sensors.shimmer_sampling_rate,
                    'thermal_camera_enabled': config.sensors.thermal_camera_enabled,
                    'thermal_fps': config.sensors.thermal_fps,
                    'rgb_camera_enabled': config.sensors.rgb_camera_enabled,
                    'rgb_fps': config.sensors.rgb_fps
                },
                'sync': {
                    'ntp_server_port': config.sync.ntp_server_port,
                    'sync_interval': config.sync.sync_interval,
                    'tolerance_ms': config.sync.tolerance_ms,
                    'max_drift_ms': config.sync.max_drift_ms
                },
                'recording': {
                    'output_directory': config.recording.output_directory,
                    'max_session_duration': config.recording.max_session_duration,
                    'auto_save_interval': config.recording.auto_save_interval,
                    'compression_enabled': config.recording.compression_enabled
                },
                'debug': config.debug
            }
            
            with open(self.config_path, 'w') as f:
                yaml.dump(config_dict, f, default_flow_style=False, indent=2)
                
            self.logger.info(f"Default configuration saved to {self.config_path}")
            
        except Exception as e:
            self.logger.error(f"Failed to save default config: {e}")
    
    def validate_config(self, config: SystemConfig) -> bool:
        """
        Validate configuration settings.
        
        Args:
            config: Configuration to validate
            
        Returns:
            bool: True if configuration is valid
        """
        try:
            # Validate network settings
            if not (1 <= config.network.port <= 65535):
                self.logger.error(f"Invalid port number: {config.network.port}")
                return False
                
            # Validate sensor settings
            if config.sensors.shimmer_sampling_rate <= 0:
                self.logger.error("Shimmer sampling rate must be positive")
                return False
                
            # Validate recording settings  
            if config.recording.max_session_duration <= 0:
                self.logger.error("Max session duration must be positive")
                return False
                
            return True
            
        except Exception as e:
            self.logger.error(f"Configuration validation failed: {e}")
            return False