package com.nerdscorner.android.plugin.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.kohsuke.github.PagedIterable

fun <T> Any.asyncFetch(
        fetchFunc: () -> PagedIterable<T>?,
        filterFunc: (T) -> Boolean = { true },
        resultFunc: (T) -> Unit
) {
    val deferredFetch = GlobalScope.async(Dispatchers.IO) {
        fetchFunc()?.map { it }
    }
    runBlocking {
        deferredFetch.await()?.forEach { t ->
            if (filterFunc(t)) {
                resultFunc(t)
            }
        }
    }
}