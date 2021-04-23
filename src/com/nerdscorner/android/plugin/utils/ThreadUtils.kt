package com.nerdscorner.android.plugin.utils

fun Any.cancelThreads(vararg threads: Thread?) {
    if (threads.isNullOrEmpty()) {
        return
    }
    for (thread in threads) {
        thread.cancel()
    }
}

fun Any.executeDelayed(delay: Long = 0, block: () -> Unit): Thread {
    val thread = Thread {
        if (delay > 0) {
            try {
                Thread.sleep(delay)
                block()
            } catch (e: InterruptedException) {
                // Nothing to do here
            }
        } else {
            block()
        }
    }
    thread.start()
    return thread
}

fun Thread?.cancel() {
    this?.interrupt()
}

fun Any.startThread(block: () -> Unit) = executeDelayed(block = block)
