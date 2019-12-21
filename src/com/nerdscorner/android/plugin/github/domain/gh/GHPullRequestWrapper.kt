package com.nerdscorner.android.plugin.github.domain.gh

import org.kohsuke.github.GHPullRequest
import java.net.URL

class GHPullRequestWrapper(val ghPullRequest: GHPullRequest) : Wrapper() {

    val fullUrl: URL
        get() = ghPullRequest.htmlUrl

    override fun toString(): String {
        return ghPullRequest.title
    }

    override fun compare(other: Wrapper): Int {
        if (other is GHPullRequestWrapper) {
            try {
                return other.ghPullRequest.updatedAt.compareTo(ghPullRequest.updatedAt)
            } catch (ignored: Exception) {
            }
        }
        return 0
    }
}
