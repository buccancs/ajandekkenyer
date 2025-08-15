"""
Main window for the Multi-Sensor Recording System Desktop Controller.
Provides the primary user interface for managing recording sessions and devices.
"""

import logging
from typing import Optional
from PyQt5.QtWidgets import (
    QMainWindow, QWidget, QVBoxLayout, QHBoxLayout, QPushButton,
    QLabel, QTextEdit, QGroupBox, QListWidget, QProgressBar,
    QStatusBar, QMenuBar, QMenu, QAction, QSplitter, QTabWidget,
    QMessageBox, QFileDialog
)
from PyQt5.QtCore import Qt, QTimer, pyqtSignal, QThread
from PyQt5.QtGui import QIcon, QFont

from ..core.config_manager import SystemConfig
from ..core.session_manager import SessionManager
from ..network.device_manager import DeviceManager
from ..sensors.shimmer_manager import ShimmerManager


class MainWindow(QMainWindow):
    """Main application window for the desktop controller."""
    
    # Signals
    session_started = pyqtSignal(str)  # session_id
    session_stopped = pyqtSignal(str)  # session_id
    device_connected = pyqtSignal(str)  # device_id
    device_disconnected = pyqtSignal(str)  # device_id
    
    def __init__(self, config: SystemConfig):
        """
        Initialize the main window.
        
        Args:
            config: System configuration
        """
        super().__init__()
        self.config = config
        self.logger = logging.getLogger(__name__)
        
        # Core managers
        self.session_manager: Optional[SessionManager] = None
        self.device_manager: Optional[DeviceManager] = None
        self.shimmer_manager: Optional[ShimmerManager] = None
        
        # Current session state
        self.current_session_id: Optional[str] = None
        self.is_recording = False
        
        # UI components
        self.status_label: Optional[QLabel] = None
        self.device_list: Optional[QListWidget] = None
        self.log_display: Optional[QTextEdit] = None
        self.progress_bar: Optional[QProgressBar] = None
        
        # Timer for UI updates
        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self._update_ui)
        self.update_timer.start(1000)  # Update every second
        
        self._init_ui()
        self._init_managers()
        self._connect_signals()
        
        self.logger.info("Main window initialized")
    
    def _init_ui(self):
        """Initialize the user interface."""
        self.setWindowTitle("Multi-Sensor Recording System - Desktop Controller")
        self.setMinimumSize(1200, 800)
        
        # Create central widget
        central_widget = QWidget()
        self.setCentralWidget(central_widget)
        
        # Create main layout
        main_layout = QHBoxLayout(central_widget)
        
        # Create splitter for resizable panes
        splitter = QSplitter(Qt.Horizontal)
        main_layout.addWidget(splitter)
        
        # Left pane - Control panel
        control_panel = self._create_control_panel()
        splitter.addWidget(control_panel)
        
        # Right pane - Information panel
        info_panel = self._create_info_panel()
        splitter.addWidget(info_panel)
        
        # Set splitter proportions
        splitter.setSizes([400, 800])
        
        # Create menu bar
        self._create_menu_bar()
        
        # Create status bar
        self._create_status_bar()
        
        self.logger.debug("UI initialization completed")
    
    def _create_control_panel(self) -> QWidget:
        """Create the left control panel."""
        panel = QWidget()
        layout = QVBoxLayout(panel)
        
        # Session control group
        session_group = QGroupBox("Session Control")
        session_layout = QVBoxLayout(session_group)
        
        # Session buttons
        self.start_session_btn = QPushButton("Start New Session")
        self.start_session_btn.setMinimumHeight(40)
        self.start_session_btn.clicked.connect(self._start_session)
        session_layout.addWidget(self.start_session_btn)
        
        self.stop_session_btn = QPushButton("Stop Session")
        self.stop_session_btn.setMinimumHeight(40)
        self.stop_session_btn.setEnabled(False)
        self.stop_session_btn.clicked.connect(self._stop_session)
        session_layout.addWidget(self.stop_session_btn)
        
        # Recording control
        self.start_recording_btn = QPushButton("Start Recording")
        self.start_recording_btn.setMinimumHeight(40)
        self.start_recording_btn.setEnabled(False)
        self.start_recording_btn.clicked.connect(self._start_recording)
        session_layout.addWidget(self.start_recording_btn)
        
        self.stop_recording_btn = QPushButton("Stop Recording")
        self.stop_recording_btn.setMinimumHeight(40)
        self.stop_recording_btn.setEnabled(False)
        self.stop_recording_btn.clicked.connect(self._stop_recording)
        session_layout.addWidget(self.stop_recording_btn)
        
        # Sync signal button
        self.sync_signal_btn = QPushButton("Send Sync Signal")
        self.sync_signal_btn.setMinimumHeight(30)
        self.sync_signal_btn.setEnabled(False)
        self.sync_signal_btn.clicked.connect(self._send_sync_signal)
        session_layout.addWidget(self.sync_signal_btn)
        
        layout.addWidget(session_group)
        
        # Device management group
        device_group = QGroupBox("Connected Devices")
        device_layout = QVBoxLayout(device_group)
        
        self.device_list = QListWidget()
        device_layout.addWidget(self.device_list)
        
        # Device control buttons
        device_btn_layout = QHBoxLayout()
        
        scan_devices_btn = QPushButton("Scan Devices")
        scan_devices_btn.clicked.connect(self._scan_devices)
        device_btn_layout.addWidget(scan_devices_btn)
        
        refresh_btn = QPushButton("Refresh")
        refresh_btn.clicked.connect(self._refresh_devices)
        device_btn_layout.addWidget(refresh_btn)
        
        device_layout.addLayout(device_btn_layout)
        layout.addWidget(device_group)
        
        # Status information
        status_group = QGroupBox("Status")
        status_layout = QVBoxLayout(status_group)
        
        self.status_label = QLabel("System Ready")
        status_layout.addWidget(self.status_label)
        
        self.progress_bar = QProgressBar()
        self.progress_bar.setVisible(False)
        status_layout.addWidget(self.progress_bar)
        
        layout.addWidget(status_group)
        
        # Add stretch to push everything to the top
        layout.addStretch()
        
        return panel
    
    def _create_info_panel(self) -> QWidget:
        """Create the right information panel."""
        panel = QWidget()
        layout = QVBoxLayout(panel)
        
        # Create tab widget for different information views
        tab_widget = QTabWidget()
        
        # Log tab
        log_tab = QWidget()
        log_layout = QVBoxLayout(log_tab)
        
        self.log_display = QTextEdit()
        self.log_display.setReadOnly(True)
        self.log_display.setFont(QFont("Courier", 9))
        log_layout.addWidget(self.log_display)
        
        tab_widget.addTab(log_tab, "System Log")
        
        # Session info tab (placeholder)
        session_info_tab = QWidget()
        session_info_layout = QVBoxLayout(session_info_tab)
        session_info_layout.addWidget(QLabel("Session information will be displayed here"))
        tab_widget.addTab(session_info_tab, "Session Info")
        
        # Device details tab (placeholder)
        device_details_tab = QWidget()
        device_details_layout = QVBoxLayout(device_details_tab)
        device_details_layout.addWidget(QLabel("Device details will be displayed here"))
        tab_widget.addTab(device_details_tab, "Device Details")
        
        layout.addWidget(tab_widget)
        
        return panel
    
    def _create_menu_bar(self):
        """Create the application menu bar."""
        menubar = self.menuBar()
        
        # File menu
        file_menu = menubar.addMenu("File")
        
        new_session_action = QAction("New Session", self)
        new_session_action.setShortcut("Ctrl+N")
        new_session_action.triggered.connect(self._start_session)
        file_menu.addAction(new_session_action)
        
        open_session_action = QAction("Open Session Folder", self)
        open_session_action.setShortcut("Ctrl+O")
        open_session_action.triggered.connect(self._open_session_folder)
        file_menu.addAction(open_session_action)
        
        file_menu.addSeparator()
        
        exit_action = QAction("Exit", self)
        exit_action.setShortcut("Ctrl+Q")
        exit_action.triggered.connect(self.close)
        file_menu.addAction(exit_action)
        
        # Tools menu
        tools_menu = menubar.addMenu("Tools")
        
        calibration_action = QAction("Camera Calibration", self)
        calibration_action.triggered.connect(self._open_calibration)
        tools_menu.addAction(calibration_action)
        
        settings_action = QAction("Settings", self)
        settings_action.triggered.connect(self._open_settings)
        tools_menu.addAction(settings_action)
        
        # Help menu
        help_menu = menubar.addMenu("Help")
        
        about_action = QAction("About", self)
        about_action.triggered.connect(self._show_about)
        help_menu.addAction(about_action)
    
    def _create_status_bar(self):
        """Create the status bar."""
        self.status_bar = QStatusBar()
        self.setStatusBar(self.status_bar)
        
        # Add permanent status indicators
        self.connection_status = QLabel("Disconnected")
        self.status_bar.addPermanentWidget(self.connection_status)
        
        self.status_bar.showMessage("Ready")
    
    def _init_managers(self):
        """Initialize core system managers."""
        try:
            # Initialize session manager
            # self.session_manager = SessionManager(self.config)
            
            # Initialize device manager
            # self.device_manager = DeviceManager(self.config)
            
            # Initialize Shimmer manager
            # self.shimmer_manager = ShimmerManager(self.config)
            
            self.logger.info("Core managers initialized")
            
        except Exception as e:
            self.logger.error(f"Failed to initialize managers: {e}")
            QMessageBox.critical(self, "Initialization Error", 
                               f"Failed to initialize system managers:\n{e}")
    
    def _connect_signals(self):
        """Connect internal signals and slots."""
        # Connect manager signals when they're implemented
        pass
    
    def _update_ui(self):
        """Update UI elements periodically."""
        try:
            # Update device list
            self._update_device_list()
            
            # Update status
            self._update_status()
            
        except Exception as e:
            self.logger.error(f"UI update error: {e}")
    
    def _update_device_list(self):
        """Update the device list display."""
        # TODO: Implement device list update
        pass
    
    def _update_status(self):
        """Update status displays."""
        if self.current_session_id:
            if self.is_recording:
                self.status_label.setText(f"Recording - Session: {self.current_session_id}")
            else:
                self.status_label.setText(f"Session Active: {self.current_session_id}")
        else:
            self.status_label.setText("System Ready")
    
    # Session control methods
    def _start_session(self):
        """Start a new recording session."""
        try:
            # TODO: Implement session start logic
            self.current_session_id = "test_session_001"
            
            # Update UI state
            self.start_session_btn.setEnabled(False)
            self.stop_session_btn.setEnabled(True)
            self.start_recording_btn.setEnabled(True)
            
            self.status_bar.showMessage(f"Session started: {self.current_session_id}")
            self.logger.info(f"Session started: {self.current_session_id}")
            
            self.session_started.emit(self.current_session_id)
            
        except Exception as e:
            self.logger.error(f"Failed to start session: {e}")
            QMessageBox.critical(self, "Session Error", f"Failed to start session:\n{e}")
    
    def _stop_session(self):
        """Stop the current recording session."""
        try:
            if self.is_recording:
                self._stop_recording()
            
            # TODO: Implement session stop logic
            session_id = self.current_session_id
            self.current_session_id = None
            
            # Update UI state
            self.start_session_btn.setEnabled(True)
            self.stop_session_btn.setEnabled(False)
            self.start_recording_btn.setEnabled(False)
            self.stop_recording_btn.setEnabled(False)
            self.sync_signal_btn.setEnabled(False)
            
            self.status_bar.showMessage("Session stopped")
            self.logger.info(f"Session stopped: {session_id}")
            
            if session_id:
                self.session_stopped.emit(session_id)
            
        except Exception as e:
            self.logger.error(f"Failed to stop session: {e}")
            QMessageBox.critical(self, "Session Error", f"Failed to stop session:\n{e}")
    
    def _start_recording(self):
        """Start recording in the current session."""
        try:
            # TODO: Implement recording start logic
            self.is_recording = True
            
            # Update UI state
            self.start_recording_btn.setEnabled(False)
            self.stop_recording_btn.setEnabled(True)
            self.sync_signal_btn.setEnabled(True)
            
            self.status_bar.showMessage("Recording started")
            self.logger.info("Recording started")
            
        except Exception as e:
            self.logger.error(f"Failed to start recording: {e}")
            QMessageBox.critical(self, "Recording Error", f"Failed to start recording:\n{e}")
    
    def _stop_recording(self):
        """Stop recording in the current session."""
        try:
            # TODO: Implement recording stop logic
            self.is_recording = False
            
            # Update UI state
            self.start_recording_btn.setEnabled(True)
            self.stop_recording_btn.setEnabled(False)
            self.sync_signal_btn.setEnabled(False)
            
            self.status_bar.showMessage("Recording stopped")
            self.logger.info("Recording stopped")
            
        except Exception as e:
            self.logger.error(f"Failed to stop recording: {e}")
            QMessageBox.critical(self, "Recording Error", f"Failed to stop recording:\n{e}")
    
    def _send_sync_signal(self):
        """Send synchronization signal to all devices."""
        try:
            # TODO: Implement sync signal
            self.logger.info("Sync signal sent")
            self.status_bar.showMessage("Sync signal sent", 2000)
            
        except Exception as e:
            self.logger.error(f"Failed to send sync signal: {e}")
            QMessageBox.warning(self, "Sync Error", f"Failed to send sync signal:\n{e}")
    
    # Device management methods
    def _scan_devices(self):
        """Scan for available devices."""
        try:
            # TODO: Implement device scanning
            self.logger.info("Scanning for devices...")
            self.status_bar.showMessage("Scanning for devices...", 3000)
            
        except Exception as e:
            self.logger.error(f"Device scan failed: {e}")
            QMessageBox.warning(self, "Scan Error", f"Device scan failed:\n{e}")
    
    def _refresh_devices(self):
        """Refresh device list."""
        try:
            # TODO: Implement device refresh
            self.logger.info("Refreshing device list")
            
        except Exception as e:
            self.logger.error(f"Failed to refresh devices: {e}")
    
    # Menu action methods
    def _open_session_folder(self):
        """Open the session output folder."""
        # TODO: Implement session folder opening
        pass
    
    def _open_calibration(self):
        """Open camera calibration tool."""
        # TODO: Implement calibration tool
        QMessageBox.information(self, "Calibration", "Calibration tool not yet implemented")
    
    def _open_settings(self):
        """Open settings dialog."""
        # TODO: Implement settings dialog
        QMessageBox.information(self, "Settings", "Settings dialog not yet implemented")
    
    def _show_about(self):
        """Show about dialog."""
        QMessageBox.about(self, "About",
                         "Multi-Sensor Recording System v1.0\\n\\n"
                         "A platform for synchronized physiological data collection\\n"
                         "for contactless GSR prediction research.")
    
    def closeEvent(self, event):
        """Handle application close event."""
        if self.current_session_id:
            reply = QMessageBox.question(self, "Exit Confirmation",
                                       "A session is currently active. Stop session and exit?",
                                       QMessageBox.Yes | QMessageBox.No,
                                       QMessageBox.No)
            if reply == QMessageBox.Yes:
                self._stop_session()
                event.accept()
            else:
                event.ignore()
        else:
            event.accept()
        
        self.logger.info("Application closing")