package com.nerdscorner.android.plugin.github.domain.gh

import org.kohsuke.github.GHRelease

import java.net.URL

class GHReleaseWrapper(val ghRelease: GHRelease) : Wrapper() {

    val url: String
        get() = ghRelease.url.toString()

    val fullUrl: URL
        get() = ghRelease.htmlUrl

    override fun toString(): String {
        return ghRelease.name
    }

    override fun compare(other: Wrapper): Int {
        if (other is GHReleaseWrapper) {
            try {
                return other.ghRelease.updatedAt.compareTo(ghRelease.updatedAt)
            } catch (ignored: Exception) {
            }
        }
        return 0
    }
}
