package com.nerdscorner.android.plugin.utils

fun <T> MutableList<T>.addIfNotPresent(element: T): Boolean {
    return if (contains(element).not()) {
        add(element)
    } else {
        false
    }
}
