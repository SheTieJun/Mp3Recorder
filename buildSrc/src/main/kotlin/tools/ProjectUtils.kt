package tools

import kotlin.math.pow
import org.gradle.api.Project


val Project.minSdk: Int
    get() = intProperty("android.minSdk")

val Project.targetSdk: Int
    get() = intProperty("android.targetSdk")

val Project.jvmTarget: String
    get() = stringProperty("kotlin.jvmTarget")

val Project.minCompileSdk: Int
    get() = intProperty("android.minCompileSdk")

val Project.compileSdk: Int
    get() = intProperty("android.compileSdk")

val Project.versionName: String
    get() = stringProperty("appVersionName")

val Project.composeCompilerVer: String
    get() = stringProperty("compose.compiler")

val Project.versionCode: Int
    get() = versionName
        .takeWhile { it.isDigit() || it == '.' }
        .split('.')
        .map { it.toInt() }
        .reversed()
        .sumByIndexed { index, unit ->
            // 1.2.3 -> 102030
            (unit * 10.0.pow(2 * index + 1)).toInt()
        }

// ./gradlew coil-compose:assemble -PenableComposeMetrics=true
val Project.enableComposeMetrics: Boolean
    get() = booleanProperty("enableComposeMetrics") { false }

val Project.enableWasm: Boolean
    get() = booleanProperty("enableWasm") { false }

private fun Project.intProperty(
    name: String,
    default: () -> Int = { error("unknown property: $name") },
): Int = (properties[name] as String?)?.toInt() ?: default()

private fun Project.stringProperty(
    name: String,
    default: () -> String = { error("unknown property: $name") },
): String = (properties[name] as String?) ?: default()

private fun Project.booleanProperty(
    name: String,
    default: () -> Boolean = { error("unknown property: $name") },
): Boolean = (properties[name] as String?)?.toBooleanStrict() ?: default()

private inline fun <T> List<T>.sumByIndexed(selector: (Int, T) -> Int): Int {
    var index = 0
    var sum = 0
    for (element in this) {
        sum += selector(index++, element)
    }
    return sum
}