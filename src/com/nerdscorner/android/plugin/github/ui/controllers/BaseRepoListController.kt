package com.nerdscorner.android.plugin.github.ui.controllers

import com.nerdscorner.android.plugin.github.domain.gh.GHPullRequestWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHReleaseWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.ui.tablemodels.BaseModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHPullRequestTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHReleaseTableModel
import com.nerdscorner.android.plugin.utils.GithubUtils
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleMouseAdapter
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.ThreadUtils
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

abstract class BaseRepoListController internal constructor(var reposTable: JTable, val repoReleasesTable: JTable,
                                                           val repoOpenPullRequestsTable: JTable, val repoClosedPullRequestsTable: JTable,
                                                           val repoComments: JLabel,  var ghOrganization: GHOrganization, val dataColumn: Int) {
    private var currentRepository: GHRepositoryWrapper? = null

    var loaderThread: Thread? = null
    var repoDataLoaderThread: Thread? = null

    var commentsUpdated: Boolean = false
    var selectedRepo: String? = null

    val latestReleaseDate: Date?
        get() {
            val latestRelease = (repoReleasesTable.model as GHReleaseTableModel).getRow(0)
            return latestRelease?.ghRelease?.published_at
        }

    val organizationName: String
        get() = ghOrganization.login

    private fun startListeningBus() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    fun init() {
        startListeningBus()

        reposTable.addMouseListener(object : SimpleMouseAdapter() {
            override fun mousePressed(row: Int, column: Int, clickCount: Int) {
                if (clickCount == 1) {
                    updateRepositoryInfoTables()
                } else if (clickCount == 2) {
                    GithubUtils.openWebLink(currentRepository!!.fullUrl)
                }
            }
        })
        repoReleasesTable.addMouseListener(object : SimpleMouseAdapter() {
            override fun mousePressed(row: Int, column: Int, clickCount: Int) {
                if (clickCount == 2) {
                    val release = (repoReleasesTable.model as GHReleaseTableModel).getRow(row)
                    GithubUtils.openWebLink(release!!.fullUrl)
                }
            }
        })
        repoOpenPullRequestsTable.addMouseListener(object : SimpleMouseAdapter() {
            override fun mousePressed(row: Int, column: Int, clickCount: Int) {
                if (clickCount == 2) {
                    val pullRequest = (repoOpenPullRequestsTable.model as GHPullRequestTableModel).getRow(row)
                    GithubUtils.openWebLink(pullRequest!!.fullUrl)
                }
            }
        })
        repoClosedPullRequestsTable.addMouseListener(object : SimpleMouseAdapter() {
            override fun mousePressed(row: Int, column: Int, clickCount: Int) {
                if (clickCount == 2) {
                    val pullRequest = (repoClosedPullRequestsTable.model as GHPullRequestTableModel).getRow(row)
                    GithubUtils.openWebLink(pullRequest!!.fullUrl)
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
        currentRepository!!
                .ghRepository
                .listReleases()
                .withPageSize(SMALL_PAGE_SIZE)
                .forEach { ghRelease -> repoReleasesModel.addRow(GHReleaseWrapper(ghRelease)) }
    }

    private fun loadPullRequests() {
        val openPrsModel = GHPullRequestTableModel(
                ArrayList(),
                arrayOf(Strings.TITLE, Strings.AUTHOR, Strings.DATE)
        )
        val closedPrsModel = GHPullRequestTableModel(
                ArrayList(),
                arrayOf(Strings.TITLE, Strings.AUTHOR, Strings.DATE)
        )
        repoOpenPullRequestsTable.model = openPrsModel
        repoClosedPullRequestsTable.model = closedPrsModel
        val repoName = currentRepository!!.ghRepository.name
        val latestReleaseDate = latestReleaseDate
        repoComments.text = "Analyzing releases and merged pull requests..."
        currentRepository!!
                .ghRepository
                .queryPullRequests()
                .state(GHIssueState.ALL)
                .sort(Sort.UPDATED)
                .direction(GHDirection.DESC)
                .list()
                .withPageSize(SMALL_PAGE_SIZE)
                .forEach { pullRequest ->
                    if (pullRequest.state == GHIssueState.OPEN) {
                        openPrsModel.addRow(GHPullRequestWrapper(pullRequest))
                    } else if (pullRequest.state == GHIssueState.CLOSED) {
                        if (latestReleaseDate == null) {
                            commentsUpdated = true
                            repoComments.text = String.format(Strings.REPO_NO_RELEASES_YET, repoName)
                            return
                        }
                        val mergedAt = pullRequest.mergedAt
                        if (mergedAt != null) {
                            if (mergedAt.after(latestReleaseDate)) {
                                closedPrsModel.addRow(GHPullRequestWrapper(pullRequest))
                                if (!commentsUpdated) {
                                    commentsUpdated = true
                                    repoComments.text = String.format(Strings.REPO_NEEDS_RELEASE, repoName)
                                }
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
            ThreadUtils.cancelThread(repoDataLoaderThread)
            repoDataLoaderThread = Thread {
                try {
                    commentsUpdated = false
                    loadReleases()
                    loadPullRequests()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            repoDataLoaderThread!!.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    abstract fun loadRepositories()

    companion object {
        val LARGE_PAGE_SIZE = 500
        val SMALL_PAGE_SIZE = 40
    }
}
