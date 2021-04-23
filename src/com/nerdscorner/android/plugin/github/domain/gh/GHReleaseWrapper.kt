package com.nerdscorner.android.plugin.github.domain.gh

import com.nerdscorner.android.plugin.utils.Strings
import org.kohsuke.github.GHRelease

import java.net.URL
import java.text.SimpleDateFormat

class GHReleaseWrapper(val ghRelease: GHRelease) : Wrapper() {

    val url: String
        get() = ghRelease.url.toString()

    val fullUrl: URL
        get() = ghRelease.htmlUrl

    val publishedAt = try {
        SimpleDateFormat(Strings.DATE_FORMAT).format(ghRelease.published_at)
    } catch (e: Exception) {
        null
    }

    override fun toString(): String {
        return ghRelease.name
    }

    override fun compare(other: Wrapper): Int {
        if (other is GHReleaseWrapper) {
            try {
                return other.ghRelease.updatedAt.compareTo(ghRelease.updatedAt)
            } catch (_: Exception) {
            }
        }
        return 0
    }
}
