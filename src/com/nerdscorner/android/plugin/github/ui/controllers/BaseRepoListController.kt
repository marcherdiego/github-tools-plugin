package com.nerdscorner.android.plugin.github.ui.controllers

import com.nerdscorner.android.plugin.github.domain.gh.GHBranchWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHPullRequestWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHReleaseWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.BranchesLoadedEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.NewClosedPullRequestsEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.NewOpenPullRequestsEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.ReleasesLoadedEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.RepoDoesNotNeedReleaseEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.RepoNeedsReleaseEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.RepoNoReleasesYetEvent
import com.nerdscorner.android.plugin.github.ui.tablemodels.BaseModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHBranchTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHPullRequestTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHReleaseTableModel
import com.nerdscorner.android.plugin.github.ui.view.BaseReposView
import com.nerdscorner.android.plugin.github.ui.view.BaseReposView.RepoClickedEvent
import com.nerdscorner.android.plugin.utils.GithubUtils
import com.nerdscorner.android.plugin.utils.JTableUtils
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.ThreadUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.kohsuke.github.GHDirection
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHPullRequestQueryBuilder.Sort
import java.io.IOException
import java.util.ArrayList
import java.util.Date
import javax.swing.JLabel
import javax.swing.JTable

abstract class BaseRepoListController<V: BaseReposView, M: BaseReposModel> internal constructor(
        val reposTable: JTable,
        val releasesTable: JTable,
        val branchesTable: JTable,
        val openPullRequestsTable: JTable,
        val closedPullRequestsTable: JTable,
        private val repoComments: JLabel,
        val ghOrganization: GHOrganization,
        private val dataColumn: Int
) {

    protected val bus = EventBus.builder().build()
    private var currentRepository: GHRepositoryWrapper? = null

    lateinit var view: V
    lateinit var model: M

    var loaderThread: Thread? = null
    private var releasesLoaderThread: Thread? = null
    private var branchesLoaderThread: Thread? = null
    private var selectedRepoRow = -1

    var commentsUpdated: Boolean = false
    var selectedRepo: String? = null

    private val latestReleaseDate: Date?
        get() {
            val latestRelease = (releasesTable.model as GHReleaseTableModel).getRow(0)
            return latestRelease?.ghRelease?.published_at
        }

    val organizationName: String
        get() = ghOrganization.login

    fun init() {
        bus.register(this)
        view.init()
        clearTables()
    }

    private fun clearTables() {
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

    fun cancel() {
        bus.unregister(this)
        ThreadUtils.cancelThreads(loaderThread, releasesLoaderThread, branchesLoaderThread)
    }

    @Throws(IOException::class)
    private fun loadReleases() {
        val repoReleasesModel = GHReleaseTableModel(ArrayList(), arrayOf(Strings.TAG, Strings.DATE))
        releasesTable.model = repoReleasesModel
        JTableUtils.centerColumns(releasesTable, GHReleaseTableModel.COLUMN_DATE)
        currentRepository
                ?.ghRepository
                ?.listReleases()
                ?.withPageSize(SMALL_PAGE_SIZE)
                ?.toList()
                ?.forEach { ghRelease ->
                    repoReleasesModel.addRow(GHReleaseWrapper(ghRelease))
                }
    }

    @Throws(IOException::class)
    private fun loadBranches() {
        val repoBranchesModel = GHBranchTableModel(ArrayList(), arrayOf(Strings.NAME, Strings.STATUS, Strings.CI_ACTIONS))
        branchesTable.model = repoBranchesModel
        JTableUtils.centerColumns(branchesTable, GHBranchTableModel.COLUMN_STATUS, GHBranchTableModel.COLUMN_TRIGGER_BUILD)
        currentRepository
                ?.ghRepository
                ?.branches
                ?.values
                ?.forEach { branch ->
                    repoBranchesModel.addRow(GHBranchWrapper(branch))
                }
    }

    private fun loadPullRequests() {
        val openPrsModel = GHPullRequestTableModel(
                ArrayList(),
                arrayOf(Strings.TITLE, Strings.AUTHOR, Strings.DATE, Strings.BUILD_STATUS)
        )
        val closedPrsModel = GHPullRequestTableModel(
                ArrayList(),
                arrayOf(Strings.TITLE, Strings.AUTHOR, Strings.DATE, Strings.BUILD_STATUS)
        )
        openPullRequestsTable.model = openPrsModel
        closedPullRequestsTable.model = closedPrsModel

        JTableUtils.centerColumns(openPullRequestsTable, GHPullRequestTableModel.COLUMN_DATE, GHPullRequestTableModel.COLUMN_CI_STATUS)
        JTableUtils.centerColumns(closedPullRequestsTable, GHPullRequestTableModel.COLUMN_DATE,
                                  GHPullRequestTableModel.COLUMN_CI_STATUS)
        val repoName = currentRepository?.ghRepository?.name
        if (latestReleaseDate == null) {
            commentsUpdated = true
            repoComments.text = String.format(Strings.REPO_NO_RELEASES_YET, repoName)
        }
        currentRepository
                ?.ghRepository
                ?.queryPullRequests()
                ?.state(GHIssueState.ALL)
                ?.sort(Sort.UPDATED)
                ?.direction(GHDirection.DESC)
                ?.list()
                ?.withPageSize(SMALL_PAGE_SIZE)
                ?.forEach { pullRequest ->
                    if (pullRequest.state == GHIssueState.OPEN) {
                        openPrsModel.addRow(GHPullRequestWrapper(pullRequest))
                    } else if (pullRequest.state == GHIssueState.CLOSED) {
                        if (pullRequest.mergedAt?.after(latestReleaseDate ?: return@forEach) == true) {
                            closedPrsModel.addRow(GHPullRequestWrapper(pullRequest))
                            if (!commentsUpdated) {
                                commentsUpdated = true
                                repoComments.text = String.format(Strings.REPO_NEEDS_RELEASE, repoName)
                            }
                        }
                    }
                }
        // If we haven't found any closed PR after the latest release, then this repo does not need to be released
        if (!commentsUpdated) {
            commentsUpdated = true
            repoComments.text = String.format(Strings.REPO_DOES_NOT_NEED_RELEASE, repoName)
        }
    }

    fun updateRepositoryInfoTables() {
        val selectedRow = reposTable.selectedRow
        if (selectedRow == -1) {
            return
        }
        currentRepository = reposTable.getValueAt(selectedRow, dataColumn) as GHRepositoryWrapper
        repoComments.text = null
        try {
            commentsUpdated = false
            ThreadUtils.cancelThreads(releasesLoaderThread, branchesLoaderThread)

            branchesLoaderThread = Thread {
                try {
                    loadBranches()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            releasesLoaderThread = Thread {
                try {
                    loadReleases()
                    loadPullRequests()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            branchesLoaderThread?.start()
            releasesLoaderThread?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadRepositories() {
        model.loadRepositories()
    }

    @Subscribe
    fun onReleasesLoaded(event: ReleasesLoadedEvent) {
        view.setReleasesTableModel(event.repoReleasesModel)
    }

    @Subscribe
    fun onBranchesLoaded(event: BranchesLoadedEvent) {
        view.setBranchesTableModel(event.repoBranchesModel)
    }

    @Subscribe
    fun onNewOpenPullRequests(event: NewOpenPullRequestsEvent) {
        view.addOpenPullRequest(event.pullRequest)
    }

    @Subscribe
    fun onNewClosedPullRequests(event: NewClosedPullRequestsEvent) {
        view.addClosedPullRequest(event.pullRequest)
    }

    @Subscribe
    fun onRepoNoReleasesYet(event: RepoNoReleasesYetEvent) {
        view.setRepoComments(String.format(Strings.REPO_NO_RELEASES_YET, model.getCurrentRepoName()))
    }

    @Subscribe
    fun onRepoNeedsRelease(event: RepoNeedsReleaseEvent) {
        view.setRepoComments(String.format(Strings.REPO_NEEDS_RELEASE, model.getCurrentRepoName()))
    }

    @Subscribe
    fun onRepoDoesNotNeedRelease(event: RepoDoesNotNeedReleaseEvent) {
        view.setRepoComments(String.format(Strings.REPO_DOES_NOT_NEED_RELEASE, model.getCurrentRepoName()))
    }

    @Subscribe
    fun onRepoClicked(event: RepoClickedEvent) {
        if (event.clickCount == 1) {
            if (selectedRepoRow == event.row) {
                return
            }
            clearTables()
            updateRepositoryInfoTables()
        } else if (event.clickCount == 2) {
            GithubUtils.openWebLink(currentRepository?.fullUrl)
        }
        selectedRepoRow = event.row
    }

    companion object {
        const val LARGE_PAGE_SIZE = 500
        const val SMALL_PAGE_SIZE = 40
    }
}
