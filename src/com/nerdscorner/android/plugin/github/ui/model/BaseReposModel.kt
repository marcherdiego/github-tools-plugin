package com.nerdscorner.android.plugin.github.ui.model

import com.nerdscorner.android.plugin.github.domain.gh.GHBranchWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHPullRequestWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHReleaseWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHBranchTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHReleaseTableModel
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

abstract class BaseReposModel(val bus: EventBus, val ghOrganization: GHOrganization) {
    protected var loaderThread: Thread? = null
    private var releasesLoaderThread: Thread? = null
    private var prsLoaderThread: Thread? = null
    private var branchesLoaderThread: Thread? = null

    val organizationName: String
        get() = ghOrganization.login

    var commentsUpdated: Boolean = false

    var currentRepository: GHRepositoryWrapper? = null
    var selectedRepoRow = -1

    abstract fun loadRepositories()

    @Throws(IOException::class)
    protected fun loadReleases() {
        val repoReleasesModel = GHReleaseTableModel(ArrayList(), arrayOf(Strings.TAG, Strings.DATE))
        currentRepository
                ?.ghRepository
                ?.listReleases()
                ?.withPageSize(SMALL_PAGE_SIZE)
                ?.toList()
                ?.forEach { ghRelease ->
                    repoReleasesModel.addRow(GHReleaseWrapper(ghRelease))
                }
        bus.post(ReleasesLoadedEvent(repoReleasesModel))
    }

    @Throws(IOException::class)
    protected fun loadBranches() {
        val repoBranchesModel = GHBranchTableModel(ArrayList(), arrayOf(Strings.NAME, Strings.STATUS, Strings.CI_ACTIONS))
        currentRepository
                ?.ghRepository
                ?.branches
                ?.values
                ?.forEach { branch ->
                    repoBranchesModel.addRow(GHBranchWrapper(branch))
                }
        bus.post(BranchesLoadedEvent(repoBranchesModel))
    }

    private fun loadPRs(latestReleaseDate: Date?) {
        var commentsUpdated = false
        if (latestReleaseDate == null) {
            commentsUpdated = true
            bus.post(RepoNoReleasesYetEvent())
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
                        bus.post(NewOpenPullRequestsEvent(GHPullRequestWrapper(pullRequest)))
                    } else if (pullRequest.state == GHIssueState.CLOSED) {
                        if (pullRequest.mergedAt?.after(latestReleaseDate ?: return@forEach) == true) {
                            bus.post(NewClosedPullRequestsEvent(GHPullRequestWrapper(pullRequest)))
                            if (!commentsUpdated) {
                                commentsUpdated = true
                                bus.post(RepoNeedsReleaseEvent())
                            }
                        }
                    }
                }
        // If we haven't found any closed PR after the latest release, then this repo does not need to be released
        if (!commentsUpdated) {
            commentsUpdated = true
            bus.post(RepoDoesNotNeedReleaseEvent())
        }
    }

    fun loadRepoReleasesAndBranches() {
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

    fun loadPullRequests(latestReleaseDate: Date?) {
        prsLoaderThread.cancel()
        prsLoaderThread = Thread {
            try {
                loadPRs(latestReleaseDate)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        prsLoaderThread?.start()
    }


    fun getCurrentRepoName(): String? {
        return currentRepository?.ghRepository?.name
    }

    fun cancel() {
        ThreadUtils.cancelThreads(loaderThread, releasesLoaderThread, prsLoaderThread, branchesLoaderThread)
    }

    fun getCurrentRepoUrl() = currentRepository?.fullUrl

    class BranchesLoadedEvent(val repoBranchesModel: GHBranchTableModel)
    class ReleasesLoadedEvent(val repoReleasesModel: GHReleaseTableModel)
    class NewOpenPullRequestsEvent(val pullRequest: GHPullRequestWrapper)
    class NewClosedPullRequestsEvent(val pullRequest: GHPullRequestWrapper)

    class RepoNoReleasesYetEvent
    class RepoNeedsReleaseEvent
    class RepoDoesNotNeedReleaseEvent

    companion object {
        const val LARGE_PAGE_SIZE = 500
        const val SMALL_PAGE_SIZE = 40
    }
}
