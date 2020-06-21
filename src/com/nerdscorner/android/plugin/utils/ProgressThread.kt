package com.nerdscorner.android.plugin.utils

import java.util.Timer
import javax.swing.JLabel
import javax.swing.SwingUtilities
import kotlin.concurrent.timer

class ProgressThread {
    var target: JLabel? = null
    var message: String? = null
        set(value) {
            field = value
            step = 0
        }
    var step: Int = 0
    var timer: Timer? = null

    fun setup(target: JLabel, message: String) {
        this.target = target
        this.message = message
        step = 0
    }

    fun start() {
        timer = timer(
                period = 300L,
                action = {
                    step = (step + 1) % MAX_STEPS
                    SwingUtilities.invokeLater {
                        target?.text = "$message${getEllipsis()}"
                    }
                }
        )
    }

    private fun getEllipsis(): String {
        val result = StringBuilder()
        for (i in 1..step) {
            result.append(Constants.DOT)
        }
        return result.toString()
    }

    fun stop() {
        timer?.cancel()
        step = 0
        message = null
    }

    companion object {
        private const val MAX_STEPS = 4
    }
}