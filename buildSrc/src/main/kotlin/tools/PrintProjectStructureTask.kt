package tools

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class PrintProjectStructureTask : DefaultTask() {

    @TaskAction
    fun printProjectStructure() {
        val projectDir = project.projectDir
        val rootDirName = projectDir.name
        println(rootDirName)
        printSubDirs(projectDir, 1)
    }

    private fun printSubDirs(dir: File, indentLevel: Int) {
        dir.listFiles()?.filter { it.isDirectory }
            ?.filter {
                it.name != ("build")
                        && !it.name.startsWith(".")
                        && it.name != ("res")
                        && it.name!="androidTest"
            }
            ?.also {
                val size = it.size
                it.forEachIndexed { index, subDir ->
                    if (size - 1 != index) {
                        println("│" + "  ".repeat(indentLevel) + "├──" + subDir.name)
                    } else {
                        println("│" + "  ".repeat(indentLevel) + "└──" + subDir.name)
                    }
                    printSubDirs(subDir, indentLevel + 1)
                }
            }
    }
}
