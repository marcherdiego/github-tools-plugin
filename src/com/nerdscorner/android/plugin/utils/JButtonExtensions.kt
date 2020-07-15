package com.nerdscorner.android.plugin.utils

import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton

fun JButton.onClick(listener: () -> Unit) {
    addMouseListener(object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent?) {
            listener()
        }
    })
}