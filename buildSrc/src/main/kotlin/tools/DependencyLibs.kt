package tools

import org.gradle.api.Action
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.accessors.runtime.addDependencyTo
import tools.DependencyLibs.Guava


object DependencyLibs {

    val proInstallerLib = mutableListOf<String>().apply {
        add(AndroidX.Benchmark.profileinstaller)
    }


    object AndroidX {



        object Benchmark {
            const val profileinstaller = "androidx.profileinstaller:profileinstaller:1.2.0"
        }
    }



    private const val guavaVersion = "32.1.2-android"

    enum class Guava(val value: String) {
        guava("com.google.guava:guava:$guavaVersion")
    }

}

fun DependencyHandler.implementation(depName: String) {
    add("implementation", depName)
}

fun DependencyHandler.kapt(depName: String) {
    add("kapt", depName)
}

fun DependencyHandler.ksp(depName: String) {
    add("ksp", depName)
}

fun DependencyHandler.compileOnly(depName: String) {
    add("compileOnly", depName)
}

fun DependencyHandler.api(depName: String) {
    add("api", depName)
}

fun DependencyHandler.annotationProcessor(depName: String) {
    add("annotationProcessor", depName)
}

fun DependencyHandler.apiTransitive(depName: String) {
    val dependencyConfiguration = Action<ExternalModuleDependency> {
        isTransitive = true
    }
    addDependencyTo(
        this, "api", depName, dependencyConfiguration
    )
}

val defAction = Action<ExternalModuleDependency> {
}

//region 具体库

fun DependencyHandler.addTest() {
    add("testImplementation", "junit:junit:4.13.2")
    add("androidTestImplementation", "androidx.test.ext:junit:1.1.5")
    add("androidTestImplementation", "androidx.test.espresso:espresso-core:3.5.1")
    add("androidTestImplementation", "androidx.test.espresso:espresso-contrib:3.5.1")
    add("androidTestImplementation", "androidx.test:rules:1.6.0-alpha01")
}

fun DependencyHandler.addProInstaller(dependencyConfiguration: Action<ExternalModuleDependency> = defAction) {
    DependencyLibs.proInstallerLib.forEach { depName ->
        addDependencyTo(
            this, "api", depName, dependencyConfiguration
        )
    }
}

fun DependencyHandler.addGuava(dependencyConfiguration: Action<ExternalModuleDependency> = defAction) {
    Guava.values().forEach { depName ->
        addDependencyTo(
            this, "api", depName.value, dependencyConfiguration
        )
    }
}
//endregion