package com.nerdscorner.android.plugin.github.ui.tablemodels

import com.nerdscorner.android.plugin.github.domain.gh.GHPullRequestWrapper
import com.nerdscorner.android.plugin.utils.Strings
import org.kohsuke.github.GHPullRequestReviewState
import java.io.Serializable
import java.text.SimpleDateFormat

class GHOpenPullRequestTableModel(pullRequests: MutableList<GHPullRequestWrapper>, colNames: Array<String>) :
        BaseModel<GHPullRequestWrapper>(pullRequests, colNames), Serializable {

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        if (rowIndex < 0 || rowIndex >= items.size) {
            return null
        }
        val pullRequestWrapper = items[rowIndex]
        val pullRequest = pullRequestWrapper.ghPullRequest
        return when (columnIndex) {
            COLUMN_TITLE -> pullRequest.title
            COLUMN_AUTHOR -> try {
                pullRequest.user.login
            } catch (e: Exception) {
                null
            }
            COLUMN_DATE -> try {
                SimpleDateFormat(Strings.DATE_FORMAT).format(pullRequest.updatedAt)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            COLUMN_PR_STATUS -> {
                val reviews = pullRequest.listReviews().toList()
                when {
                    reviews.any { it.state == GHPullRequestReviewState.APPROVED } -> GHPullRequestReviewState.APPROVED
                    reviews.any { it.state == GHPullRequestReviewState.CHANGES_REQUESTED } -> GHPullRequestReviewState.CHANGES_REQUESTED
                    reviews.any { it.state == GHPullRequestReviewState.COMMENTED } -> GHPullRequestReviewState.COMMENTED
                    else -> GHPullRequestReviewState.PENDING
                }
            }
            COLUMN_CI_STATUS -> pullRequestWrapper.buildStatus?.capitalize()
            else -> null
        }
    }

    companion object {
        private const val COLUMN_TITLE = 0
        const val COLUMN_AUTHOR = 1
        const val COLUMN_DATE = 2
        const val COLUMN_PR_STATUS = 3
        const val COLUMN_CI_STATUS = 4
    }
}
