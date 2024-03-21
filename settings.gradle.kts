pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven("https://jitpack.io")
    }
}
rootProject.name="Mp3Recorder"
include (":recorder-sim")
include (":recorder-mix")
include (":recorder-core")
include (":recorder-st")
include (":app")

//apply from : "https://gist.githubusercontent.com/SheTieJun/f4cb1bd33997c2b46d9e3df40b95a02e/raw/f2aede11c36b56e92d33f96589daa9f357f31443/maven-shetj.gradle"