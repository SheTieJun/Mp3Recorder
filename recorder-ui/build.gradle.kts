import tools.addTest
import tools.androidLibrary

plugins {
    id("com.android.library")
    kotlin("android")
    id("maven-publish")
}

androidLibrary("me.shetj.recorder.ui"){
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
    compileOnly(project(":recorder-mix"))
    compileOnly(project(":recorder-core"))
    addTest()
    implementation (libs.androidx.lifecycle.runtime)
    implementation (libs.androidx.constraintlayout)
    implementation (libs.shetj.dialog)
    implementation (libs.androidx.core)
    implementation (libs.androidx.appcompat)
    implementation (libs.androidx.material)
}
apply(from = "uploadLocal.gradle")