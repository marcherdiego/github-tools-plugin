package com.nerdscorner.android.plugin.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.kohsuke.github.PagedIterable

fun <T> Any.asyncFetch(
        fetchFunc: () -> PagedIterable<T>?,
        filterFunc: (T) -> Boolean = { true },
        resultFunc: (T) -> Unit
) {
    runBlocking {
        withContext(Dispatchers.IO) {
            fetchFunc()?.map { it }
        }?.forEach { t ->
            if (filterFunc(t)) {
                resultFunc(t)
            }
        }
    }
}