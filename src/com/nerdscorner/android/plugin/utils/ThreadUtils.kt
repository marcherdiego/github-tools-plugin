package com.nerdscorner.android.plugin.utils

object ThreadUtils {
    fun cancelThread(thread: Thread?) {
        thread?.interrupt()
    }

    fun cancelThreads(vararg threads: Thread?) {
        if (threads.isNullOrEmpty()) {
            return
        }
        for (thread in threads) {
            cancelThread(thread)
        }
    }
}
