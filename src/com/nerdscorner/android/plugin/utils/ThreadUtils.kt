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
}

fun Thread?.cancel() {
    this?.interrupt()
}

fun JLabel.setTextThread(text: String) {
    SwingUtilities.invokeLater {
        setText(text)
    }
}
