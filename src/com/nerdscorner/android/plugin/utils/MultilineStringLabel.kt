package com.nerdscorner.android.plugin.utils

class MultilineStringLabel(value: String?) {
    private val stringBuilder = StringBuilder()

    init {
        stringBuilder
                .append("<html>")
                .append(value)
    }

    fun addLine(value: String): MultilineStringLabel {
        stringBuilder
                .append("<br>")
                .append(value)
        return this
    }

    fun build(): String {
        return stringBuilder
                .append("</html>")
                .toString()
    }
}
