package com.nerdscorner.android.plugin.utils

import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

class MouseUtils {

    abstract class DoubleClickAdapter : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            if (e.clickCount == 2) {
                onDoubleClick()
            }
        }

        abstract fun onDoubleClick()
    }
}
