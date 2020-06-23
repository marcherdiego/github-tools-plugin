package com.nerdscorner.android.plugin.github.ui.controllers

import com.nerdscorner.android.plugin.github.domain.gh.GHPullRequestWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHReleaseWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.ui.tablemodels.BaseModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHPullRequestTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHReleaseTableModel
import com.nerdscorner.android.plugin.utils.GithubUtils
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleDoubleClickAdapter
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleMouseAdapter
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.ThreadUtils
import com.nerdscorner.android.plugin.utils.cancel
import org.greenrobot.eventbus.EventBus
import org.kohsuke.github.GHDirection
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHPullRequestQueryBuilder.Sort
import java.io.IOException
import java.util.ArrayList
import java.util.Date
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.ListSelectionModel

abstract class BaseRepoListController internal constructor(
        val reposTable: JTable,
        val repoReleasesTable: JTable,
        val repoOpenPullRequestsTable: JTable,
        val repoClosedPullRequestsTable: JTable,
        private val repoComments: JLabel,
        val ghOrganization: GHOrganization,
        private val dataColumn: Int) {
    private var currentRepository: GHRepositoryWrapper? = null

    var loaderThread: Thread? = null
    var repoDataLoaderThread: Thread? = null

    var commentsUpdated: Boolean = false
    var selectedRepo: String? = null

    private val latestReleaseDate: Date?
        get() {
            val latestRelease = (repoReleasesTable.model as GHReleaseTableModel).getRow(0)
            return latestRelease?.ghRelease?.published_at
        }

    val organizationName: String
        get() = ghOrganization.login

    private fun startListeningBus() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus
                    .getDefault()
                    .register(this)
        }
    }

    fun init() {
        startListeningBus()

        reposTable.addMouseListener(object : SimpleMouseAdapter() {
            override fun mousePressed(row: Int, column: Int, clickCount: Int) {
                if (clickCount == 1) {
                    clearTables()
                    updateRepositoryInfoTables()
                } else if (clickCount == 2) {
                    GithubUtils.openWebLink(currentRepository?.fullUrl)
                }
            }
        })
        repoReleasesTable.addMouseListener(object : SimpleDoubleClickAdapter() {
            override fun onDoubleClick(row: Int, column: Int) {
                val release = (repoReleasesTable.model as GHReleaseTableModel).getRow(row)
                GithubUtils.openWebLink(release?.fullUrl)
            }
        })
        repoOpenPullRequestsTable.addMouseListener(object : SimpleDoubleClickAdapter() {
            override fun onDoubleClick(row: Int, column: Int) {
                val pullRequest = (repoOpenPullRequestsTable.model as GHPullRequestTableModel).getRow(row)
                if (column == GHPullRequestTableModel.COLUMN_CI_URL) {
                    GithubUtils.openWebLink(pullRequest?.buildStatusUrl)
                } else {
                    GithubUtils.openWebLink(pullRequest?.fullUrl)
                }
            }
        })
        repoClosedPullRequestsTable.addMouseListener(object : SimpleDoubleClickAdapter() {
            override fun onDoubleClick(row: Int, column: Int) {
                val pullRequest = (repoClosedPullRequestsTable.model as GHPullRequestTableModel).getRow(row)
                if (column == GHPullRequestTableModel.COLUMN_CI_URL) {
                    GithubUtils.openWebLink(pullRequest?.buildStatusUrl)
                } else {
                    GithubUtils.openWebLink(pullRequest?.fullUrl)
                }
            }
        })
        reposTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        repoReleasesTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        repoOpenPullRequestsTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        repoClosedPullRequestsTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION

        if (reposTable.model is BaseModel<*>) {
            (reposTable.model as BaseModel<*>).removeAllRows()
        }

        clearTables()
    }

    private fun clearTables() {
        if (repoReleasesTable.model is BaseModel<*>) {
            (repoReleasesTable.model as BaseModel<*>).removeAllRows()
        }
        if (repoOpenPullRequestsTable.model is BaseModel<*>) {
            (repoOpenPullRequestsTable.model as BaseModel<*>).removeAllRows()
        }
        if (repoClosedPullRequestsTable.model is BaseModel<*>) {
            (repoClosedPullRequestsTable.model as BaseModel<*>).removeAllRows()
        }
    }

    fun cancel() {
        ThreadUtils.cancelThreads(loaderThread, repoDataLoaderThread)
    }

    @Throws(IOException::class)
    private fun loadReleases() {
        val repoReleasesModel = GHReleaseTableModel(ArrayList(), arrayOf(Strings.TAG, Strings.DATE))
        repoReleasesTable.model = repoReleasesModel
        currentRepository
                ?.ghRepository
                ?.listReleases()
                ?.withPageSize(SMALL_PAGE_SIZE)
                ?.toList()
                ?.forEach { ghRelease -> repoReleasesModel.addRow(GHReleaseWrapper(ghRelease)) }
    }

    private fun loadPullRequests() {
        val openPrsModel = GHPullRequestTableModel(
                ArrayList(),
                arrayOf(Strings.TITLE, Strings.AUTHOR, Strings.DATE, Strings.BUILD_URL)
        )
        val closedPrsModel = GHPullRequestTableModel(
                ArrayList(),
                arrayOf(Strings.TITLE, Strings.AUTHOR, Strings.DATE, Strings.BUILD_URL)
        )
        repoOpenPullRequestsTable.model = openPrsModel
        repoClosedPullRequestsTable.model = closedPrsModel
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
                        if (pullRequest.mergedAt?.after(latestReleaseDate) == true) {
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
            repoDataLoaderThread.cancel()
            repoDataLoaderThread = Thread {
                try {
                    commentsUpdated = false
                    loadReleases()
                    loadPullRequests()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            repoDataLoaderThread?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    abstract fun loadRepositories()

    companion object {
        const val LARGE_PAGE_SIZE = 500
        const val SMALL_PAGE_SIZE = 40
    }
}
