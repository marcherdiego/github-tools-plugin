package com.nerdscorner.android.plugin.github.ui.view

import com.nerdscorner.android.plugin.github.domain.gh.GHPullRequestWrapper
import com.nerdscorner.android.plugin.github.ui.tablemodels.BaseModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHBranchTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHPullRequestTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHReleaseTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.utils.GithubUtils
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleDoubleClickAdapter
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleMouseAdapter
import org.greenrobot.eventbus.EventBus
import java.util.Date
import java.util.HashMap
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.ListSelectionModel

abstract class BaseReposView(
        protected val bus: EventBus,
        protected val reposTable: JTable,
        protected val releasesTable: JTable,
        protected val branchesTable: JTable,
        protected val openPullRequestsTable: JTable,
        protected val closedPullRequestsTable: JTable,
        protected val repoComments: JLabel,
        protected val dataColumn: Int
) {

    val latestReleaseDate: Date?
        get() {
            return (releasesTable.model as? GHReleaseTableModel)
                    ?.getRow(0)
                    ?.ghRelease
                    ?.published_at
        }

    fun init() {
        reposTable.addMouseListener(object : SimpleMouseAdapter() {
            override fun mousePressed(row: Int, column: Int, clickCount: Int) {
                bus.post(RepoClickedEvent(row, column, clickCount))
            }
        })
        releasesTable.addMouseListener(object : SimpleDoubleClickAdapter() {
            override fun onDoubleClick(row: Int, column: Int) {
                val release = (releasesTable.model as GHReleaseTableModel).getRow(row)
                GithubUtils.openWebLink(release?.fullUrl)
            }
        })
        openPullRequestsTable.addMouseListener(object : SimpleDoubleClickAdapter() {
            override fun onDoubleClick(row: Int, column: Int) {
                val pullRequest = (openPullRequestsTable.model as GHPullRequestTableModel).getRow(row)
                if (column == GHPullRequestTableModel.COLUMN_CI_STATUS) {
                    GithubUtils.openWebLink(pullRequest?.buildStatusUrl)
                } else {
                    GithubUtils.openWebLink(pullRequest?.fullUrl)
                }
            }
        })
        closedPullRequestsTable.addMouseListener(object : SimpleDoubleClickAdapter() {
            override fun onDoubleClick(row: Int, column: Int) {
                val pullRequest = (closedPullRequestsTable.model as GHPullRequestTableModel).getRow(row)
                if (column == GHPullRequestTableModel.COLUMN_CI_STATUS) {
                    GithubUtils.openWebLink(pullRequest?.buildStatusUrl)
                } else {
                    GithubUtils.openWebLink(pullRequest?.fullUrl)
                }
            }
        })
        branchesTable.addMouseListener(object : SimpleDoubleClickAdapter() {
            override fun onDoubleClick(row: Int, column: Int) {
                val branch = (branchesTable.model as GHBranchTableModel).getRow(row) ?: return
                when (column) {
                    GHBranchTableModel.COLUMN_NAME -> GithubUtils.openWebLink(branch.url)
                    GHBranchTableModel.COLUMN_STATUS -> branch.openBuildInBrowser()
                    GHBranchTableModel.COLUMN_TRIGGER_BUILD -> branch.triggerBuild()
                }
            }
        })
        reposTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        branchesTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        releasesTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        openPullRequestsTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        closedPullRequestsTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION

        if (reposTable.model is BaseModel<*>) {
            (reposTable.model as BaseModel<*>).removeAllRows()
        }
    }

    abstract fun updateRepositoryInfoTables(tableModel: GHRepoTableModel, tooltips: HashMap<String, String>)

    abstract fun setReleasesTableModel(repoReleasesModel: GHReleaseTableModel)

    abstract fun setBranchesTableModel(repoBranchesModel: GHBranchTableModel)

    abstract fun setPullRequestTableModels()

    abstract fun setRepoComments(text: String)

    abstract fun addOpenPullRequest(pullRequest: GHPullRequestWrapper)

    abstract fun addClosedPullRequest(pullRequest: GHPullRequestWrapper)

    class RepoClickedEvent(val row: Int, val column: Int, val clickCount: Int)
}
