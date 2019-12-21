package com.nerdscorner.android.plugin.github.domain.gh

abstract class Wrapper {
    abstract fun compare(other: Wrapper): Int
}
