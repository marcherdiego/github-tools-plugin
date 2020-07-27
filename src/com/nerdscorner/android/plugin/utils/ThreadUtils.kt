package com.nerdscorner.android.plugin.utils

import javax.swing.JLabel
import javax.swing.SwingUtilities

object ThreadUtils {
    fun cancelThreads(vararg threads: Thread?) {
        if (threads.isNullOrEmpty()) {
            return
        }
        for (thread in threads) {
            thread.cancel()
        }
    }

    fun executeDelayed(delay: Long, block: () -> Unit) {
        Thread {
            try {
                Thread.sleep(delay)
            } catch (e: InterruptedException) {
                // Nothing to do here
            }
            block()
        }.start()
    }
}

fun Thread?.cancel() {
    this?.interrupt()
}

fun JLabel.setTextThread(text: String) {
    SwingUtilities.invokeLater {
        setText(text)
    }
}
