package com.nerdscorner.android.plugin.github.ui.tablemodels

import com.nerdscorner.android.plugin.github.domain.gh.GHPullRequestWrapper
import java.io.Serializable

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
            COLUMN_AUTHOR -> pullRequestWrapper.author
            COLUMN_DATE -> pullRequestWrapper.updatedAt
            COLUMN_PR_STATUS -> pullRequestWrapper.prStatus
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
