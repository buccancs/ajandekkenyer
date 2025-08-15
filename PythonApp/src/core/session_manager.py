"""
Session management for the Multi-Sensor Recording System.
Handles creation, coordination, and data management of recording sessions.
"""

import json
import logging
import threading
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional, Any
from dataclasses import dataclass, asdict
from enum import Enum


class SessionStatus(Enum):
    """Session status enumeration."""
    CREATED = "created"
    ACTIVE = "active"
    RECORDING = "recording"
    STOPPED = "stopped"
    ERROR = "error"


@dataclass
class SessionMetadata:
    """Session metadata structure."""
    session_id: str
    created_at: str
    started_at: Optional[str] = None
    stopped_at: Optional[str] = None
    duration: Optional[float] = None  # seconds
    status: str = SessionStatus.CREATED.value
    devices: List[Dict[str, Any]] = None
    files: List[Dict[str, Any]] = None
    recording_started_at: Optional[str] = None
    recording_stopped_at: Optional[str] = None
    recording_duration: Optional[float] = None
    notes: str = ""
    
    def __post_init__(self):
        if self.devices is None:
            self.devices = []
        if self.files is None:
            self.files = []


class SessionManager:
    """Manages recording sessions and associated data."""
    
    def __init__(self, config):
        """
        Initialize session manager.
        
        Args:
            config: System configuration object
        """
        self.config = config
        self.logger = logging.getLogger(__name__)
        
        # Session state
        self.current_session: Optional[SessionMetadata] = None
        self.sessions_directory = Path(config.recording.output_directory)
        self.sessions_directory.mkdir(parents=True, exist_ok=True)
        
        # Thread safety
        self._lock = threading.RLock()
        
        # Callbacks
        self.session_callbacks: Dict[str, List[callable]] = {
            'session_created': [],
            'session_started': [],
            'session_stopped': [],
            'recording_started': [],
            'recording_stopped': [],
            'device_added': [],
            'file_added': []
        }
        
        self.logger.info("Session manager initialized")
    
    def create_session(self, session_id: Optional[str] = None, notes: str = "") -> str:
        """
        Create a new recording session.
        
        Args:
            session_id: Optional custom session ID
            notes: Optional session notes
            
        Returns:
            str: Session ID
            
        Raises:
            RuntimeError: If a session is already active
        """
        with self._lock:
            if self.current_session is not None:
                raise RuntimeError("A session is already active")
            
            # Generate session ID if not provided
            if session_id is None:
                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                session_id = f"session_{timestamp}"
            
            # Create session metadata
            created_at = datetime.now(timezone.utc).isoformat()
            self.current_session = SessionMetadata(
                session_id=session_id,
                created_at=created_at,
                notes=notes
            )
            
            # Create session directory
            session_path = self._get_session_path(session_id)
            session_path.mkdir(parents=True, exist_ok=True)
            
            # Save initial metadata
            self._save_session_metadata()
            
            self.logger.info(f"Session created: {session_id}")
            self._trigger_callbacks('session_created', session_id)
            
            return session_id
    
    def start_session(self) -> None:
        """
        Start the current session.
        
        Raises:
            RuntimeError: If no session is active
        """
        with self._lock:
            if self.current_session is None:
                raise RuntimeError("No active session to start")
            
            self.current_session.started_at = datetime.now(timezone.utc).isoformat()
            self.current_session.status = SessionStatus.ACTIVE.value
            
            self._save_session_metadata()
            
            self.logger.info(f"Session started: {self.current_session.session_id}")
            self._trigger_callbacks('session_started', self.current_session.session_id)
    
    def stop_session(self) -> None:
        """
        Stop the current session.
        
        Raises:
            RuntimeError: If no session is active
        """
        with self._lock:
            if self.current_session is None:
                raise RuntimeError("No active session to stop")
            
            # Stop recording if it's active
            if self.current_session.status == SessionStatus.RECORDING.value:
                self.stop_recording()
            
            # Update session metadata
            self.current_session.stopped_at = datetime.now(timezone.utc).isoformat()
            self.current_session.status = SessionStatus.STOPPED.value
            
            # Calculate total duration
            if self.current_session.started_at:
                start_time = datetime.fromisoformat(self.current_session.started_at.replace('Z', '+00:00'))
                stop_time = datetime.fromisoformat(self.current_session.stopped_at.replace('Z', '+00:00'))
                self.current_session.duration = (stop_time - start_time).total_seconds()
            
            self._save_session_metadata()
            
            session_id = self.current_session.session_id
            self.logger.info(f"Session stopped: {session_id}")
            self._trigger_callbacks('session_stopped', session_id)
            
            # Clear current session
            self.current_session = None
    
    def start_recording(self) -> None:
        """
        Start recording in the current session.
        
        Raises:
            RuntimeError: If no session is active or already recording
        """
        with self._lock:
            if self.current_session is None:
                raise RuntimeError("No active session")
            
            if self.current_session.status == SessionStatus.RECORDING.value:
                raise RuntimeError("Recording is already active")
            
            self.current_session.recording_started_at = datetime.now(timezone.utc).isoformat()
            self.current_session.status = SessionStatus.RECORDING.value
            
            self._save_session_metadata()
            
            self.logger.info(f"Recording started in session: {self.current_session.session_id}")
            self._trigger_callbacks('recording_started', self.current_session.session_id)
    
    def stop_recording(self) -> None:
        """
        Stop recording in the current session.
        
        Raises:
            RuntimeError: If no session is active or not recording
        """
        with self._lock:
            if self.current_session is None:
                raise RuntimeError("No active session")
            
            if self.current_session.status != SessionStatus.RECORDING.value:
                raise RuntimeError("Not currently recording")
            
            self.current_session.recording_stopped_at = datetime.now(timezone.utc).isoformat()
            self.current_session.status = SessionStatus.ACTIVE.value
            
            # Calculate recording duration
            if self.current_session.recording_started_at:
                start_time = datetime.fromisoformat(self.current_session.recording_started_at.replace('Z', '+00:00'))
                stop_time = datetime.fromisoformat(self.current_session.recording_stopped_at.replace('Z', '+00:00'))
                self.current_session.recording_duration = (stop_time - start_time).total_seconds()
            
            self._save_session_metadata()
            
            self.logger.info(f"Recording stopped in session: {self.current_session.session_id}")
            self._trigger_callbacks('recording_stopped', self.current_session.session_id)
    
    def add_device(self, device_id: str, device_type: str, device_info: Dict[str, Any]) -> None:
        """
        Add a device to the current session.
        
        Args:
            device_id: Unique device identifier
            device_type: Type of device (e.g., 'shimmer', 'android', 'thermal_camera')
            device_info: Additional device information
            
        Raises:
            RuntimeError: If no session is active
        """
        with self._lock:
            if self.current_session is None:
                raise RuntimeError("No active session")
            
            device_entry = {
                'device_id': device_id,
                'device_type': device_type,
                'added_at': datetime.now(timezone.utc).isoformat(),
                'info': device_info
            }
            
            # Remove existing entry for this device if present
            self.current_session.devices = [
                d for d in self.current_session.devices 
                if d['device_id'] != device_id
            ]
            
            # Add new entry
            self.current_session.devices.append(device_entry)
            
            self._save_session_metadata()
            
            self.logger.info(f"Device added to session: {device_id} ({device_type})")
            self._trigger_callbacks('device_added', device_id, device_type)
    
    def add_file(self, filename: str, file_type: str, file_size: int, 
                device_id: Optional[str] = None, checksum: Optional[str] = None) -> None:
        """
        Add a file record to the current session.
        
        Args:
            filename: Name of the file
            file_type: Type of file (e.g., 'gsr_data', 'video', 'thermal')
            file_size: Size of file in bytes
            device_id: Optional device that created the file
            checksum: Optional file checksum for integrity verification
            
        Raises:
            RuntimeError: If no session is active
        """
        with self._lock:
            if self.current_session is None:
                raise RuntimeError("No active session")
            
            file_entry = {
                'filename': filename,
                'file_type': file_type,
                'file_size': file_size,
                'device_id': device_id,
                'checksum': checksum,
                'added_at': datetime.now(timezone.utc).isoformat()
            }
            
            self.current_session.files.append(file_entry)
            
            self._save_session_metadata()
            
            self.logger.debug(f"File added to session: {filename} ({file_type})")
            self._trigger_callbacks('file_added', filename, file_type)
    
    def get_session_info(self) -> Optional[Dict[str, Any]]:
        """
        Get information about the current session.
        
        Returns:
            Optional[Dict]: Session information or None if no active session
        """
        with self._lock:
            if self.current_session is None:
                return None
            
            return asdict(self.current_session)
    
    def get_session_path(self, session_id: Optional[str] = None) -> Optional[Path]:
        """
        Get the path to a session directory.
        
        Args:
            session_id: Session ID (uses current session if None)
            
        Returns:
            Optional[Path]: Session directory path or None if session not found
        """
        if session_id is None:
            if self.current_session is None:
                return None
            session_id = self.current_session.session_id
        
        return self._get_session_path(session_id)
    
    def list_sessions(self) -> List[Dict[str, Any]]:
        """
        List all available sessions.
        
        Returns:
            List[Dict]: List of session metadata
        """
        sessions = []
        
        for session_dir in self.sessions_directory.iterdir():
            if session_dir.is_dir():
                metadata_file = session_dir / "session_metadata.json"
                if metadata_file.exists():
                    try:
                        with open(metadata_file, 'r') as f:
                            metadata = json.load(f)
                        sessions.append(metadata)
                    except Exception as e:
                        self.logger.warning(f"Failed to load session metadata for {session_dir.name}: {e}")
        
        # Sort by creation time (newest first)
        sessions.sort(key=lambda x: x.get('created_at', ''), reverse=True)
        
        return sessions
    
    def register_callback(self, event: str, callback: callable) -> None:
        """
        Register a callback for session events.
        
        Args:
            event: Event name (session_created, session_started, etc.)
            callback: Callback function
        """
        if event in self.session_callbacks:
            self.session_callbacks[event].append(callback)
        else:
            self.logger.warning(f"Unknown event type: {event}")
    
    def _get_session_path(self, session_id: str) -> Path:
        """Get the filesystem path for a session."""
        return self.sessions_directory / session_id
    
    def _save_session_metadata(self) -> None:
        """Save current session metadata to file."""
        if self.current_session is None:
            return
        
        session_path = self._get_session_path(self.current_session.session_id)
        metadata_file = session_path / "session_metadata.json"
        
        try:
            with open(metadata_file, 'w') as f:
                json.dump(asdict(self.current_session), f, indent=2)
        except Exception as e:
            self.logger.error(f"Failed to save session metadata: {e}")
    
    def _trigger_callbacks(self, event: str, *args) -> None:
        """Trigger callbacks for an event."""
        for callback in self.session_callbacks.get(event, []):
            try:
                callback(*args)
            except Exception as e:
                self.logger.error(f"Callback error for {event}: {e}")
    
    def __del__(self):
        """Cleanup on object destruction."""
        if self.current_session and self.current_session.status != SessionStatus.STOPPED.value:
            self.logger.warning("Session manager destroyed with active session")
            try:
                self.stop_session()
            except:
                pass