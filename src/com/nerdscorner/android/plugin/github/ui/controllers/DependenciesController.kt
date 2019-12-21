package com.nerdscorner.android.plugin.github.ui.controllers


import com.intellij.uiDesigner.core.GridConstraints
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.ui.controllers.BaseRepoListController.LARGE_PAGE_SIZE
import com.nerdscorner.android.plugin.github.ui.dependencies.DependenciesPanel
import com.nerdscorner.android.plugin.github.ui.tablemodels.BaseModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.github.ui.tables.ColumnRenderer
import com.nerdscorner.android.plugin.utils.GithubUtils
import com.nerdscorner.android.plugin.utils.JTableUtils
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleMouseAdapter
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.ThreadUtils
import org.kohsuke.github.GHMyself
import org.kohsuke.github.GHOrganization
import java.util.ArrayList
import java.util.HashMap
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities

class DependenciesController(private val reposTable: JTable, dependenciesGraphPanel: JPanel, private val ghOrganization: GHOrganization, private val myselfGitHub: GHMyself) {
    private var loaderThread: Thread? = null
    private val dependenciesPanel: DependenciesPanel = DependenciesPanel()
    private var selectedRepo: String? = null

    init {
        dependenciesGraphPanel.add(dependenciesPanel, GridConstraints().apply {
            row = 0
            column = 0
        })
    }

    fun cancel() {
        ThreadUtils.cancelThread(loaderThread)
        dependenciesPanel.clear()
    }

    fun init() {
        reposTable.addMouseListener(object : SimpleMouseAdapter() {
            override fun mousePressed(row: Int, column: Int, clickCount: Int) {
                if (clickCount == 1) {
                    updateRepositoryDependenciesTree()
                } else if (clickCount == 2) {
                    val currentRepository = reposTable.getValueAt(row, GHRepoTableModel.COLUMN_NAME) as GHRepositoryWrapper
                    GithubUtils.openWebLink(currentRepository.fullUrl)
                }
            }
        })
        reposTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        if (reposTable.model is BaseModel<*>) {
            (reposTable.model as BaseModel<*>).removeAllRows()
        }
    }

    fun loadRepositories() {
        ThreadUtils.cancelThread(loaderThread)
        loaderThread = Thread {
            val reposTableModel = GHRepoTableModel(ArrayList(), arrayOf(Strings.NAME))
            reposTable.model = reposTableModel
            val tooltips = HashMap<String, String>()
            reposTable
                    .getColumn(Strings.NAME)
                    .cellRenderer = ColumnRenderer(tooltips)
            ghOrganization
                    .listRepositories()
                    .withPageSize(LARGE_PAGE_SIZE)
                    .forEach { repository ->
                        if (repository.isFork || !repository.fullName.startsWith(ghOrganization.login)) {
                            //Ignore forks or projects that doesn't belong to this organization
                            return@forEach
                        }
                        val ghRepositoryWrapper = GHRepositoryWrapper(repository)
                        reposTableModel.addRow(ghRepositoryWrapper)
                        tooltips[ghRepositoryWrapper.name] = ghRepositoryWrapper.description
                    }
            myselfGitHub
                    .listSubscriptions()
                    .withPageSize(LARGE_PAGE_SIZE)
                    .forEach { repository ->
                        if (repository.isFork) {
                            //Ignore forks or projects that doesn't belong to this organization
                            return@forEach
                        }
                        val ghRepositoryWrapper = GHRepositoryWrapper(repository)
                        reposTableModel.addRow(ghRepositoryWrapper)
                    }
            JTableUtils.findAndSelectDefaultRepo(selectedRepo, reposTable)
            SwingUtilities.invokeLater { this.updateRepositoryDependenciesTree() }
        }
        loaderThread?.start()
    }

    private fun updateRepositoryDependenciesTree() {
        dependenciesPanel.setRepository(
                reposTable.getValueAt(reposTable.selectedRow, GHRepoTableModel.COLUMN_NAME) as GHRepositoryWrapper
        )
    }

    fun setSelectedRepo(selectedRepo: String) {
        this.selectedRepo = selectedRepo
    }
}
