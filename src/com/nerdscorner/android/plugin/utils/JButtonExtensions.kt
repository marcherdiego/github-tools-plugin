package com.nerdscorner.android.plugin.utils

import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton

fun JButton.onClick(listener: () -> Unit) {
    mouseListeners.forEach {
        removeMouseListener(it)
    }
    addMouseListener(object : MouseAdapter() {
        override fun mouseReleased(e: MouseEvent?) {
            if (isEnabled.not()) {
                return
            }
            if (e?.clickCount == 1) {
                listener()
            }
        }
    })
}