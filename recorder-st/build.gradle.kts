import tools.androidLibrary

plugins {
    id("com.android.library")
    kotlin("android")
}

androidLibrary("me.shetj.recorder.soundtouch"){
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
    compileOnly(project(":recorder-core"))
}

apply(from = "uploadLocal.gradle")
