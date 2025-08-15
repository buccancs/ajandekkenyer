pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack for GitHub packages
        maven { url = uri("https://jitpack.io") }
        // Shimmer Research repository
        maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
        // Additional repository for specialized SDKs
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    }
}

rootProject.name = "ajandekkenyer"

// Include AndroidApp as a subproject
include(":AndroidApp:app")