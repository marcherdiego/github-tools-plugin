package com.nerdscorner.android.plugin.github.ui.view

import com.nerdscorner.android.plugin.github.domain.gh.GHPullRequestWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.ui.tablemodels.BaseModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHBranchTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHPullRequestTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHReleaseTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.github.ui.tables.ColumnRenderer
import com.nerdscorner.android.plugin.utils.GithubUtils
import com.nerdscorner.android.plugin.utils.JTableUtils
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleDoubleClickAdapter
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleMouseAdapter
import com.nerdscorner.android.plugin.utils.Strings
import org.greenrobot.eventbus.EventBus
import java.util.ArrayList
import java.util.HashMap
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.ListSelectionModel

class AllReposView(
        bus: EventBus,
        reposTable: JTable,
        releasesTable: JTable,
        branchesTable: JTable,
        openPullRequestsTable: JTable,
        closedPullRequestsTable: JTable,
        repoComments: JLabel,
        dataColumn: Int
) : BaseReposView(bus, reposTable, releasesTable, branchesTable, openPullRequestsTable, closedPullRequestsTable, repoComments, dataColumn) {
    var selectedRepo: String? = null
    private var currentRepository: GHRepositoryWrapper? = null

    override fun updateRepositoryInfoTables(tableModel: GHRepoTableModel, tooltips: HashMap<String, String>) {
        JTableUtils.findAndSelectDefaultRepo(selectedRepo, reposTable)

        reposTable.model = tableModel
        val column = reposTable.getColumn(Strings.NAME)
        column.cellRenderer = ColumnRenderer(tooltips)

        val selectedRow = reposTable.selectedRow
        if (selectedRow == -1) {
            return
        }
        currentRepository = reposTable.getValueAt(selectedRow, dataColumn) as GHRepositoryWrapper
        repoComments.text = null
    }

    override fun setReleasesTableModel(repoReleasesModel: GHReleaseTableModel) {
        releasesTable.model = repoReleasesModel
        JTableUtils.centerColumns(releasesTable, GHReleaseTableModel.COLUMN_DATE)
    }

    override fun setBranchesTableModel(repoBranchesModel: GHBranchTableModel) {
        branchesTable.model = repoBranchesModel
        JTableUtils.centerColumns(branchesTable, GHBranchTableModel.COLUMN_STATUS, GHBranchTableModel.COLUMN_TRIGGER_BUILD)
    }

    override fun setPullRequestTableModels() {
        openPullRequestsTable.model = GHPullRequestTableModel(
                ArrayList(),
                arrayOf(Strings.TITLE, Strings.AUTHOR, Strings.DATE, Strings.BUILD_STATUS)
        )
        closedPullRequestsTable.model = GHPullRequestTableModel(
                ArrayList(),
                arrayOf(Strings.TITLE, Strings.AUTHOR, Strings.DATE, Strings.BUILD_STATUS)
        )
        JTableUtils.centerColumns(openPullRequestsTable, GHPullRequestTableModel.COLUMN_DATE, GHPullRequestTableModel.COLUMN_CI_STATUS)
        JTableUtils.centerColumns(closedPullRequestsTable, GHPullRequestTableModel.COLUMN_DATE, GHPullRequestTableModel.COLUMN_CI_STATUS)
    }

    override fun setRepoComments(text: String) {
        repoComments.text = text
    }

    override fun addOpenPullRequest(pullRequest: GHPullRequestWrapper) {
        (openPullRequestsTable.model as GHPullRequestTableModel).addRow(pullRequest)
    }

    override fun addClosedPullRequest(pullRequest: GHPullRequestWrapper) {
        (closedPullRequestsTable.model as GHPullRequestTableModel).addRow(pullRequest)
    }

}
