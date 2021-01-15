package com.nerdscorner.android.plugin.utils

import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

fun Component.onClick(listener: () -> Unit) {
    onClick(listener, 1)
}

private fun Component.onClick(listener: () -> Unit, clickCount: Int) {
    mouseListeners.forEach {
        removeMouseListener(it)
    }
    addMouseListener(object : MouseAdapter() {
        override fun mouseReleased(e: MouseEvent?) {
            if (isEnabled.not()) {
                return
            }
            if (e?.clickCount == clickCount) {
                listener()
            }
        }
    })
}
