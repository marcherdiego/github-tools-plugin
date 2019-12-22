package com.nerdscorner.android.plugin.github.ui.controllers


import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.events.FavoriteRepositoryUpdatedEvent
import com.nerdscorner.android.plugin.github.ui.tablemodels.BaseModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.github.ui.tables.ColumnRenderer
import com.nerdscorner.android.plugin.utils.JTableUtils
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.ThreadUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.kohsuke.github.GHMyself
import org.kohsuke.github.GHOrganization
import java.util.ArrayList
import java.util.HashMap
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel

class MyReposController(reposTable: JTable, repoReleases: JTable, repoPullRequestsTable: JTable, repoClosedPullRequestsTable: JTable,
                        repoComments: JLabel, private val myselfGitHub: GHMyself, ghOrganization: GHOrganization) :
        BaseRepoListController(reposTable, repoReleases, repoPullRequestsTable, repoClosedPullRequestsTable, repoComments,
                ghOrganization, BaseModel.COLUMN_NAME) {

    override fun loadRepositories() {
        ThreadUtils.cancelThread(loaderThread)
        loaderThread = Thread {
            val myReposTableModel = GHRepoTableModel(ArrayList(), arrayOf(Strings.NAME))
            reposTable.model = myReposTableModel
            val column = reposTable.getColumn(Strings.NAME)
            val tooltips = HashMap<String, String>()
            column.cellRenderer = ColumnRenderer(tooltips)
            myselfGitHub
                    .listSubscriptions()
                    .withPageSize(LARGE_PAGE_SIZE)
                    .forEach { repository ->
                        if (repository.isFork.not() && repository.fullName.startsWith(organizationName)) {
                            myReposTableModel.addRow(GHRepositoryWrapper(repository))
                        }
                    }
            JTableUtils.findAndSelectDefaultRepo(selectedRepo, reposTable)
            SwingUtilities.invokeLater { this.updateRepositoryInfoTables() }
        }
        loaderThread?.start()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFavoriteRepositoryUpdated(event: FavoriteRepositoryUpdatedEvent) {
        (reposTable.model as AbstractTableModel).fireTableDataChanged()
    }
}
