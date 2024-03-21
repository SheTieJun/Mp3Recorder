import tools.androidLibrary

plugins {
    id("com.android.library")
    kotlin("android")
}

androidLibrary("me.shetj.recorder.core"){
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
}

apply(from = "uploadLocal.gradle")