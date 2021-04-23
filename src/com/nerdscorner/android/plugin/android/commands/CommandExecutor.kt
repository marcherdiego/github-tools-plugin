package com.nerdscorner.android.plugin.android.commands

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ScriptRunnerUtil
import com.nerdscorner.android.plugin.utils.Constants
import java.nio.charset.Charset

class CommandExecutor(private var basePath: String? = null) {

    fun execute(command: String): String {
        val commandLine = GeneralCommandLine(
                command
                        .split(Constants.SPACE)
                        .toList()
        )
        commandLine.charset = Charset.forName(Constants.UTF8)
        commandLine.setWorkDirectory(basePath)
        return try {
            ScriptRunnerUtil.getProcessOutput(commandLine)
        } catch (e: ExecutionException) {
            e.printStackTrace()
            Constants.BLANK
        }
    }
}