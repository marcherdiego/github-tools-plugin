package com.nerdscorner.android.plugin.github.ui.view

import com.nerdscorner.android.plugin.github.domain.gh.GHBranchWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHPullRequestWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHReleaseWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.ui.tablemodels.BaseModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHBranchTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHPullRequestTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHReleaseTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.github.ui.tables.ColumnRenderer
import com.nerdscorner.android.plugin.utils.JTableUtils
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleDoubleClickAdapter
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleMouseAdapter
import com.nerdscorner.android.plugin.utils.Strings
import org.greenrobot.eventbus.EventBus
import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.ListSelectionModel

class ReposView(
        private val reposTable: JTable,
        private val releasesTable: JTable,
        private val branchesTable: JTable,
        private val openPullRequestsTable: JTable,
        private val closedPullRequestsTable: JTable,
        private val repoComments: JLabel,
        val dataColumn: Int
) {
    lateinit var bus: EventBus

    var selectedRepo: String? = null
    private var currentRepository: GHRepositoryWrapper? = null

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
                bus.post(ReleaseClickedEvent(release))
            }
        })
        openPullRequestsTable.addMouseListener(object : SimpleDoubleClickAdapter() {
            override fun onDoubleClick(row: Int, column: Int) {
                val pullRequest = (openPullRequestsTable.model as GHPullRequestTableModel).getRow(row)
                bus.post(PullRequestClickedEvent(column, pullRequest))
            }
        })
        closedPullRequestsTable.addMouseListener(object : SimpleDoubleClickAdapter() {
            override fun onDoubleClick(row: Int, column: Int) {
                val pullRequest = (closedPullRequestsTable.model as GHPullRequestTableModel).getRow(row)
                bus.post(PullRequestClickedEvent(column, pullRequest))
            }
        })
        branchesTable.addMouseListener(object : SimpleDoubleClickAdapter() {
            override fun onDoubleClick(row: Int, column: Int) {
                val branch = (branchesTable.model as GHBranchTableModel).getRow(row) ?: return
                bus.post(BranchClickedEvent(column, branch))
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

        clearTables()
    }

    fun updateRepositoryInfoTables(tableModel: GHRepoTableModel, tooltips: HashMap<String, String>) {
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

    fun setReleasesTableModel(repoReleasesModel: GHReleaseTableModel) {
        releasesTable.model = repoReleasesModel
        JTableUtils.centerColumns(releasesTable, GHReleaseTableModel.COLUMN_DATE)
    }

    fun setBranchesTableModel(repoBranchesModel: GHBranchTableModel) {
        branchesTable.model = repoBranchesModel
        JTableUtils.centerColumns(branchesTable, GHBranchTableModel.COLUMN_STATUS, GHBranchTableModel.COLUMN_TRIGGER_BUILD)
    }

    fun setPullRequestTableModels() {
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

    fun setRepoComments(text: String?) {
        repoComments.text = text
    }

    fun addOpenPullRequest(pullRequest: GHPullRequestWrapper) {
        (openPullRequestsTable.model as GHPullRequestTableModel).addRow(pullRequest)
    }

    fun addClosedPullRequest(pullRequest: GHPullRequestWrapper) {
        (closedPullRequestsTable.model as GHPullRequestTableModel).addRow(pullRequest)
    }

    fun clearTables() {
        if (branchesTable.model is BaseModel<*>) {
            (branchesTable.model as BaseModel<*>).removeAllRows()
        }
        if (releasesTable.model is BaseModel<*>) {
            (releasesTable.model as BaseModel<*>).removeAllRows()
        }
        if (openPullRequestsTable.model is BaseModel<*>) {
            (openPullRequestsTable.model as BaseModel<*>).removeAllRows()
        }
        if (closedPullRequestsTable.model is BaseModel<*>) {
            (closedPullRequestsTable.model as BaseModel<*>).removeAllRows()
        }
    }

    fun getRepoAt(row: Int, column: Int): Any = reposTable.getValueAt(row, column)

    class RepoClickedEvent(val row: Int, val column: Int, val clickCount: Int)
    class ReleaseClickedEvent(val release: GHReleaseWrapper?)
    class PullRequestClickedEvent(val column: Int, val pullRequest: GHPullRequestWrapper?)
    class BranchClickedEvent(val column: Int, val branch: GHBranchWrapper)
}
