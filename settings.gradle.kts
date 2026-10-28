pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "driver-demand-radar"

// Single-module project: the root project IS the app module (com.android.application
// is applied in the root build.gradle.kts). No app/ subfolder.
