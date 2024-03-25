import tools.androidApplication
import tools.addTest

plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
}

androidApplication("me.shetj.mp3recorder"){
    defaultConfig {
        minSdk = 24
        ndk {
            this.abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64", "x86"))
        }
    }
    signingConfigs {
        create("release") {
            keyAlias = "shetj"
            keyPassword = "123456"
            storeFile = file("test.jks")
            storePassword = "123456"
        }
    }
    sourceSets {
        getByName("main"){
            jniLibs.setSrcDirs(listOf("libs"))
        }
    }
}

dependencies {
    implementation(fileTree("libs") {
        include("*.jar", "*.aar")
    })
    addTest()
    implementation(project(":recorder-mix"))
    implementation(project(":recorder-sim"))
    implementation(project(":recorder-core"))
    implementation(project(":recorder-st"))
    implementation("com.github.SheTieJun:BaseKit:fcc505b8b7")
    ksp(libs.androidx.room.compiler)
}