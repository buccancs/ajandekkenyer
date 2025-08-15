// Multi-App Project: Android Kotlin + Python Desktop Controller
// Root build configuration for the Multi-Sensor GSR Recording System

plugins {
    id("java")
    // Android plugins are applied in the AndroidApp module
    id("com.android.application") version "8.1.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.dagger.hilt.android") version "2.48.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20" apply false
    kotlin("kapt") version "1.9.20" apply false
}

group = "com.gsr.recording"
version = "1.0.0"

// Common configurations that apply to all subprojects
allprojects {
    group = rootProject.group
    version = rootProject.version
}

// Root project tasks for coordinating both apps
tasks.register("buildAll") {
    group = "build"
    description = "Build both Android and Python applications"
    
    dependsOn(":AndroidApp:app:build")
    dependsOn(":PythonApp:build")
}

tasks.register("testAll") {
    group = "verification"
    description = "Run tests for both Android and Python applications"
    
    dependsOn(":AndroidApp:app:test")
    dependsOn(":PythonApp:check")
}

tasks.register("cleanAll") {
    group = "build"
    description = "Clean both Android and Python applications"
    
    dependsOn(":AndroidApp:app:clean")
    dependsOn(":PythonApp:clean")
}

// Task to run the desktop Python controller
tasks.register("runDesktop") {
    group = "application"
    description = "Run the Python desktop controller application"
    
    dependsOn(":PythonApp:runPython")
}

// Task to run the desktop controller in headless mode
tasks.register("runDesktopHeadless") {
    group = "application"
    description = "Run the Python desktop controller in headless mode"
    
    dependsOn(":PythonApp:runPythonHeadless")
}

// Default Java test configuration for any Java-based tests
dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}