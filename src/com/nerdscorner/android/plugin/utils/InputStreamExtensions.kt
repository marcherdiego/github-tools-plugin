package com.nerdscorner.android.plugin.utils

import org.apache.commons.io.IOUtils
import java.io.InputStream
import java.io.StringWriter
import java.nio.charset.Charset

fun InputStream.getFileContent(): String {
    val writer = StringWriter()
    IOUtils.copy(this, writer, Charset.defaultCharset())
    return writer.toString()
}
