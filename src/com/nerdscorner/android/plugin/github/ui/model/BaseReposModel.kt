package com.nerdscorner.android.plugin.github.ui.model

import com.nerdscorner.android.plugin.github.domain.gh.GHBranchWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHPullRequestWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHReleaseWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHBranchTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHReleaseTableModel
import com.nerdscorner.android.plugin.utils.Strings
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
    protected var releasesLoaderThread: Thread? = null
    protected var branchesLoaderThread: Thread? = null

    val organizationName: String
        get() = ghOrganization.login

    private var currentRepository: GHRepositoryWrapper? = null

    abstract fun loadRepositories()

    abstract fun loadRepoReleasesAndBranches(latestReleaseDate: Date?)

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

    protected fun loadPullRequests(latestReleaseDate: Date?) {
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

    fun getCurrentRepoName(): String? {
        return currentRepository?.ghRepository?.name
    }

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
