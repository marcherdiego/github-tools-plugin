package com.nerdscorner.android.plugin.github.ui.view

import com.nerdscorner.android.plugin.github.domain.gh.GHPullRequestWrapper
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHBranchTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHReleaseTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import org.greenrobot.eventbus.EventBus
import java.util.HashMap
import javax.swing.JLabel
import javax.swing.JTable

class MyReposView(
        bus: EventBus,
        reposTable: JTable,
        releasesTable: JTable,
        branchesTable: JTable,
        openPullRequestsTable: JTable,
        closedPullRequestsTable: JTable,
        repoComments: JLabel,
        dataColumn: Int
) : BaseReposView(bus, reposTable, releasesTable, branchesTable, openPullRequestsTable, closedPullRequestsTable, repoComments, dataColumn) {
    override fun updateRepositoryInfoTables(tableModel: GHRepoTableModel, tooltips: HashMap<String, String>) {
    }

    override fun setReleasesTableModel(repoReleasesModel: GHReleaseTableModel) {
    }

    override fun setBranchesTableModel(repoBranchesModel: GHBranchTableModel) {
    }

    override fun setPullRequestTableModels() {
    }

    override fun setRepoComments(text: String) {
    }

    override fun addOpenPullRequest(pullRequest: GHPullRequestWrapper) {
    }

    override fun addClosedPullRequest(pullRequest: GHPullRequestWrapper) {
    }
}
