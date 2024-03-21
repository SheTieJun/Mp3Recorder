// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        gradlePluginPortal()
        google()
        maven {
            url = uri("https://storage.googleapis.com/r8-releases/raw")
        }
    }
    dependencies {
        classpath("com.android.tools:r8:8.2.47") //FIX com.android.tools.r8.internal.nc: Sealed classes are not supported as program classes( Java 17 导致的问题)
    }
}


plugins {
    id(libs.plugins.android.application.get().pluginId) apply false
    id(libs.plugins.android.library.get().pluginId) apply false
    id(libs.plugins.android.test.get().pluginId) apply false
    id(libs.plugins.kotlin.android.get().pluginId) apply false
    id(libs.plugins.spotless.get().pluginId) version (libs.versions.spotless)
    id("maven-publish")
    id("com.google.devtools.ksp") version ("1.9.22-1.0.17")
}
