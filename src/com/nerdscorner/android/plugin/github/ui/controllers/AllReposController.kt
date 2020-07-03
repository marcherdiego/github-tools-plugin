package com.nerdscorner.android.plugin.github.ui.controllers

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.ui.tablemodels.BaseModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.github.ui.tables.ColumnRenderer
import com.nerdscorner.android.plugin.utils.JTableUtils
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.cancel
import org.kohsuke.github.GHOrganization
import java.util.ArrayList
import java.util.HashMap
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.SwingUtilities

class AllReposController(
        reposTable: JTable,
        releases: JTable,
        branches: JTable,
        pullRequestsTable: JTable,
        closedPullRequestsTable: JTable,
        comments: JLabel,
        ghOrganization: GHOrganization
) : BaseRepoListController(reposTable, releases, branches, pullRequestsTable, closedPullRequestsTable, comments,
                           ghOrganization, BaseModel.COLUMN_NAME) {

    override fun loadRepositories() {
        loaderThread.cancel()
        loaderThread = Thread {
            val reposTableModel = GHRepoTableModel(ArrayList(), arrayOf(Strings.NAME))
            reposTable.model = reposTableModel
            val column = reposTable.getColumn(Strings.NAME)
            val tooltips = HashMap<String, String>()
            column.cellRenderer = ColumnRenderer(tooltips)
            ghOrganization
                    .listRepositories()
                    .withPageSize(LARGE_PAGE_SIZE)
                    .forEach { repository ->
                        if (repository.isFork.not() && repository.fullName.startsWith(organizationName)) {
                            val ghRepositoryWrapper = GHRepositoryWrapper(repository)
                            reposTableModel.addRow(ghRepositoryWrapper)
                            tooltips[ghRepositoryWrapper.name] = ghRepositoryWrapper.description
                        }
                    }
            JTableUtils.findAndSelectDefaultRepo(selectedRepo, reposTable)
            SwingUtilities.invokeLater { this.updateRepositoryInfoTables() }
        }
        loaderThread?.start()
    }
}
