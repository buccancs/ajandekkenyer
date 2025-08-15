// PythonApp module build configuration
// This module contains the Python desktop controller application

plugins {
    id("base") // Base plugin for non-JVM projects
}

description = "Python Desktop Controller for Multi-Sensor GSR Recording System"

// Python-specific tasks
tasks.register<Exec>("installPythonDeps") {
    group = "python"
    description = "Install Python dependencies from requirements.txt"
    
    commandLine("python3", "-m", "pip", "install", "-r", "requirements.txt")
    workingDir = projectDir
}

tasks.register<Exec>("runPython") {
    group = "python"
    description = "Run the Python desktop controller application"
    dependsOn("installPythonDeps")
    
    commandLine("python3", "main.py")
    workingDir = projectDir
}

tasks.register<Exec>("runPythonHeadless") {
    group = "python"
    description = "Run the Python application in headless mode"
    dependsOn("installPythonDeps")
    
    commandLine("python3", "main.py", "--no-gui")
    workingDir = projectDir
}

tasks.register<Exec>("testPython") {
    group = "python"
    description = "Run Python tests using pytest"
    dependsOn("installPythonDeps")
    
    commandLine("python3", "-m", "pytest", "test_core.py", "-v")
    workingDir = projectDir
}

tasks.register<Exec>("lintPython") {
    group = "python"
    description = "Run Python linting with flake8"
    dependsOn("installPythonDeps")
    
    commandLine("python3", "-m", "flake8", "src/", "main.py", "--max-line-length=88")
    workingDir = projectDir
}

tasks.register<Exec>("formatPython") {
    group = "python"
    description = "Format Python code with black"
    dependsOn("installPythonDeps")
    
    commandLine("python3", "-m", "black", "src/", "main.py")
    workingDir = projectDir
}

// Make the build task depend on Python setup
tasks.named("build") {
    dependsOn("installPythonDeps")
}

// Make check task run Python tests
tasks.named("check") {
    dependsOn("testPython", "lintPython")
}