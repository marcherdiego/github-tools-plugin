package com.nerdscorner.android.plugin.github.domain.gh

import org.kohsuke.github.GHPullRequest
import java.net.URL

class GHPullRequestWrapper(val ghPullRequest: GHPullRequest) : Wrapper() {

    init {
        ghPullRequest
                .repository
                ?.getCheckRuns(ghPullRequest.head.sha)
                ?.forEach {
                    if (it.detailsUrl.toString().contains("travis-ci") || it.detailsUrl.toString().contains("circleci")) {
                        buildStatus = it.status
                        buildStatusUrl = it.detailsUrl.toString()
                        return@forEach
                    }
                }
    }

    val fullUrl: URL
        get() = ghPullRequest.htmlUrl

    var buildStatus: String? = null
    var buildStatusUrl: String? = null

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
