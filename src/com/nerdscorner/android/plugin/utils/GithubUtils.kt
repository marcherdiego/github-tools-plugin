package com.nerdscorner.android.plugin.utils

import java.awt.Desktop
import java.net.URI
import java.net.URL

object GithubUtils {

    fun openWebLink(uri: URI?) {
        try {
            with(Desktop.getDesktop()) {
                if (isSupported(Desktop.Action.BROWSE)) {
                    browse(uri)
                }
            }
        } catch (ignored: Exception) {
        }
    }

    fun openWebLink(url: URL?) {
        try {
            openWebLink(url?.toURI())
        } catch (ignored: Exception) {
        }
    }

    fun openWebLink(url: String?) {
        try {
            openWebLink(URI.create(url ?: return))
        } catch (ignored: Exception) {
        }
    }
}
