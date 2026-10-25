// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

allprojects {
    // Avoid OneDrive file-locking issues by placing build artifacts outside OneDrive
    val customBuildDir = File(System.getProperty("user.home"), ".android-builds/mediread/${project.name}")
    layout.buildDirectory.set(customBuildDir)
}