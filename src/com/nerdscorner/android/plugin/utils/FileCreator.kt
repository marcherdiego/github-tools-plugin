package com.nerdscorner.android.plugin.utils

import java.io.File
import java.io.FileWriter
import java.io.IOException

object FileCreator {

    @Throws(IOException::class)
    fun createFile(basePath: String, fileName: String, content: String) {
        val file = File(basePath, fileName)
        if (!file.exists()) {
            val fileWriter = FileWriter(file)
            fileWriter.write(content)
            fileWriter.close()
        }
    }
}