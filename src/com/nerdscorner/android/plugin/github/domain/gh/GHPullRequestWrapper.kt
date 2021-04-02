package com.nerdscorner.android.plugin.github.domain.gh

import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.startThread
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHPullRequestReviewState
import java.net.URL
import java.text.SimpleDateFormat
import javax.swing.table.AbstractTableModel

class GHPullRequestWrapper(private val tableModel: AbstractTableModel, val ghPullRequest: GHPullRequest) : Wrapper() {

    val fullUrl: URL
        get() = ghPullRequest.htmlUrl

    var buildStatus: String? = null
    var buildStatusUrl: String? = null
    var prStatus = "FETCHING..."
    var author = try {
        ghPullRequest.user.login
    } catch (e: Exception) {
        null
    }
    var updatedAt = try {
        SimpleDateFormat(Strings.DATE_FORMAT).format(ghPullRequest.updatedAt)
    } catch (e: Exception) {
        null
    }

    init {
        startThread {
            ghPullRequest
                    .repository
                    ?.getCheckRuns(ghPullRequest.head.sha)
                    ?.forEach {
                        if (it.detailsUrl.toString().contains("travis-ci") || it.detailsUrl.toString().contains("circleci")) {
                            buildStatus = it.status
                            buildStatusUrl = it.detailsUrl.toString()
                            tableModel.fireTableDataChanged()
                            return@forEach
                        }
                    }
        }

        startThread {
            val reviews = ghPullRequest.listReviews().toList()
            prStatus = when {
                reviews.any { it.state == GHPullRequestReviewState.APPROVED } -> GHPullRequestReviewState.APPROVED
                reviews.any { it.state == GHPullRequestReviewState.CHANGES_REQUESTED } -> GHPullRequestReviewState.CHANGES_REQUESTED
                reviews.any { it.state == GHPullRequestReviewState.COMMENTED } -> GHPullRequestReviewState.COMMENTED
                else -> GHPullRequestReviewState.PENDING
            }.toString()
            tableModel.fireTableDataChanged()
        }
    }

    override fun toString(): String {
        return ghPullRequest.title
    }

    override fun compare(other: Wrapper): Int {
        if (other is GHPullRequestWrapper) {
            try {
                return other.ghPullRequest.updatedAt.compareTo(ghPullRequest.updatedAt)
            } catch (_: Exception) {
            }
        }
        return 0
    }
}
