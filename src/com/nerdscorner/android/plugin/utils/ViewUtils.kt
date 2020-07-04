package com.nerdscorner.android.plugin.utils

import javax.swing.JComponent

object ViewUtils {
    fun hide(vararg views: JComponent) {
        if (views.isNullOrEmpty()) {
            return
        }
        for (view in views) {
            view.isVisible = false
        }
    }

    fun show(vararg views: JComponent) {
        if (views.isNullOrEmpty()) {
            return
        }
        for (view in views) {
            view.isVisible = true
        }
    }
}
