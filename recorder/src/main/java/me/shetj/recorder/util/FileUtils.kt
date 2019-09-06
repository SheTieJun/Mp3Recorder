package me.shetj.recorder.util

import java.io.File

object FileUtils {

    fun deleteFile(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            if (file.isFile) {
                file.delete()
            } else {
                val filePaths = file.list()
                for (path in filePaths!!) {
                    deleteFile(filePath + File.separator + path)
                }
                file.delete()
            }
        }
    }
}
