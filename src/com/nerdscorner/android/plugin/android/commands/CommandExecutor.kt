package com.nerdscorner.android.plugin.android.commands

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ScriptRunnerUtil
import java.nio.charset.Charset

class CommandExecutor(var basePath: String? = null) {

    fun execute(command: String): String {
        val commandLine = GeneralCommandLine(
                command
                        .split(SPACE)
                        .toList()
        )
        commandLine.charset = Charset.forName(UTF8)
        commandLine.setWorkDirectory(basePath)
        return try {
            ScriptRunnerUtil.getProcessOutput(commandLine)
        } catch (e: ExecutionException) {
            e.printStackTrace()
            BLANK
        }
    }

    companion object {
        const val BLANK = ""
        const val SPACE = " "
        const val UTF8 = "UTF-8"
    }
}