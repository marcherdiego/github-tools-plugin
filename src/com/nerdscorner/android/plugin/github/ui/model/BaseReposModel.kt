package com.nerdscorner.android.plugin.github.ui.model

import com.intellij.ide.util.PropertiesComponent
import com.nerdscorner.android.plugin.github.domain.gh.GHBranchWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHPullRequestWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHReleaseWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.exceptions.MissingCircleCiTokenException
import com.nerdscorner.android.plugin.github.exceptions.MissingTravisCiTokenException
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHBranchTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHReleaseTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.ci.CiEnvironment
import com.nerdscorner.android.plugin.ci.CircleCi
import com.nerdscorner.android.plugin.utils.BrowserUtils
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.ci.TravisCi
import com.nerdscorner.android.plugin.github.events.ParameterUpdatedEvent
import com.nerdscorner.android.plugin.github.exceptions.UndefinedCiEnvironmentException
import com.nerdscorner.android.plugin.github.managers.GitHubManager
import com.nerdscorner.android.plugin.utils.cancel
import com.nerdscorner.android.plugin.utils.cancelThreads
import com.nerdscorner.android.plugin.utils.startThread
import org.greenrobot.eventbus.EventBus
import org.kohsuke.github.GHDirection
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHMyself
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHPullRequestQueryBuilder.Sort
import org.kohsuke.github.GHRepository
import java.io.IOException
import java.util.ArrayList
import java.util.Date

abstract class BaseReposModel(val selectedRepo: String) {
    lateinit var bus: EventBus

    //Loader threads
    protected var loaderThread: Thread? = null
    private var releasesLoaderThread: Thread? = null
    private var prsLoaderThread: Thread? = null
    private var branchesLoaderThread: Thread? = null

    val organizationName: String?
        get() = GitHubManager.ghOrganization?.login

    var commentsUpdated: Boolean = false
    var currentRepository: GHRepositoryWrapper? = null
    var selectedRepoRow = -1

    private var currentBranchBuild: GHBranchWrapper? = null
    private var currentCiEnvironment: CiEnvironment? = null

    abstract fun loadRepositories()

    fun loadMyRepos(myselfGitHub: GHMyself?, tableModel: GHRepoTableModel) {
        myselfGitHub
                ?.listSubscriptions()
                ?.withPageSize(LARGE_PAGE_SIZE)
                ?.forEach { repository ->
                    if (shouldAddRepository(repository)) {
                        tableModel.addRow(GHRepositoryWrapper(repository))
                    }
                }
    }

    fun loadOrganizationRepos(ghOrganization: GHOrganization?, tableModel: GHRepoTableModel) {
        ghOrganization
                ?.listRepositories()
                ?.withPageSize(LARGE_PAGE_SIZE)
                ?.forEach { repository ->
                    if (shouldAddRepository(repository)) {
                        tableModel.addRow(GHRepositoryWrapper(repository))
                    }
                }
    }

    fun allowNonOrganizationRepos() = propertiesComponent.isTrueValue(Strings.SHOW_REPOS_FROM_OUTSIDE_ORGANIZATION)

    private fun shouldAddRepository(repository: GHRepository): Boolean {
        val validSource = allowNonOrganizationRepos() || repository.fullName.startsWith(organizationName ?: Strings.BLANK)
        return validSource && repository.isFork.not()
    }

    fun init() {
        commentsUpdated = false
        currentRepository = null
        selectedRepoRow = -1
        currentBranchBuild = null
        currentCiEnvironment = null
        cancelThreads(loaderThread, releasesLoaderThread, prsLoaderThread, branchesLoaderThread)
    }

    @Throws(IOException::class)
    protected fun loadReleases() {
        val repoReleasesModel = GHReleaseTableModel(ArrayList(), arrayOf(Strings.TAG, Strings.DATE))
        currentRepository
                ?.ghRepository
                ?.listReleases()
                ?.withPageSize(SMALL_PAGE_SIZE)
                ?.toList()
                ?.forEach { ghRelease ->
                    if (Thread.interrupted()) {
                        return
                    }
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
                    if (Thread.interrupted()) {
                        return
                    }
                    repoBranchesModel.addRow(GHBranchWrapper(branch))
                }
        bus.post(BranchesLoadedEvent(repoBranchesModel))
    }

    private fun loadPRs(latestReleaseDate: Date?) {
        val closedPrs = mutableListOf<GHPullRequestWrapper>()
        currentRepository
                ?.ghRepository
                ?.queryPullRequests()
                ?.state(GHIssueState.ALL)
                ?.sort(Sort.UPDATED)
                ?.direction(GHDirection.DESC)
                ?.list()
                ?.withPageSize(SMALL_PAGE_SIZE)
                ?.forEach { pullRequest ->
                    if (Thread.interrupted()) {
                        return
                    }
                    if (pullRequest.state == GHIssueState.OPEN) {
                        bus.post(NewOpenPullRequestsEvent(GHPullRequestWrapper(pullRequest)))
                    } else if (pullRequest.state == GHIssueState.CLOSED) {
                        if (latestReleaseDate == null || pullRequest.mergedAt?.after(latestReleaseDate) == true) {
                            val pullRequestWrapper = GHPullRequestWrapper(pullRequest)
                            closedPrs.add(pullRequestWrapper)
                            bus.post(NewClosedPullRequestsEvent(pullRequestWrapper))
                        }
                    }
                }
    }

    fun loadRepoReleasesAndBranches() {
        try {
            commentsUpdated = false
            cancelThreads(releasesLoaderThread, branchesLoaderThread, prsLoaderThread)

            branchesLoaderThread = startThread {
                try {
                    loadBranches()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            releasesLoaderThread = startThread {
                try {
                    loadReleases()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadPullRequests(latestReleaseDate: Date?) {
        prsLoaderThread.cancel()
        prsLoaderThread = startThread {
            try {
                loadPRs(latestReleaseDate)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getCurrentRepoUrl() = currentRepository?.fullUrl

    @Throws(MissingTravisCiTokenException::class, MissingCircleCiTokenException::class, UndefinedCiEnvironmentException::class)
    fun requestRebuild(branch: GHBranchWrapper) {
        currentBranchBuild = branch
        val ciEnvironment = when {
            branch.travisBuild -> {
                val travisCiToken = propertiesComponent.getValue(Strings.TRAVIS_CI_TOKEN_PROPERTY, Strings.BLANK)
                if (travisCiToken.isEmpty()) {
                    throw MissingTravisCiTokenException()
                }
                TravisCi
            }
            branch.circleBuild -> {
                val circleCiToken = propertiesComponent.getValue(Strings.CIRCLE_CI_TOKEN_PROPERTY, Strings.BLANK)
                if (circleCiToken.isEmpty()) {
                    throw MissingCircleCiTokenException()
                }
                CircleCi
            }
            else -> throw UndefinedCiEnvironmentException()
        }
        currentCiEnvironment = ciEnvironment.triggerRebuild(
                externalId = branch.externalBuildId,
                success = {
                    bus.post(BuildSucceededEventEvent())
                },
                fail = { message ->
                    bus.post(BuildFailedEventEvent(message))
                }
        )
    }

    fun clearCurrentBranchBuild() {
        currentBranchBuild = null
    }

    fun triggerPendingRebuild() {
        requestRebuild(currentBranchBuild ?: return)
    }

    fun cancelRebuildRequest() {
        currentCiEnvironment?.cancelRequest()
    }

    fun saveTravisToken(token: String?) {
        propertiesComponent.setValue(Strings.TRAVIS_CI_TOKEN_PROPERTY, token)
        EventBus.getDefault()
                .post(ParameterUpdatedEvent())
    }

    fun saveCircleToken(token: String?) {
        propertiesComponent.setValue(Strings.CIRCLE_CI_TOKEN_PROPERTY, token)
        EventBus.getDefault()
                .post(ParameterUpdatedEvent())
    }

    fun openBuildInBrowser(branch: GHBranchWrapper? = null) {
        BrowserUtils.openWebLink((branch ?: currentBranchBuild)?.buildStatusUrl)
    }

    fun clearCiToken() {
        propertiesComponent.setValue(
                when {
                    currentBranchBuild?.circleBuild == true -> Strings.CIRCLE_CI_TOKEN_PROPERTY
                    currentBranchBuild?.travisBuild == true -> Strings.TRAVIS_CI_TOKEN_PROPERTY
                    else -> return
                },
                Strings.BLANK
        )
        EventBus.getDefault()
                .post(ParameterUpdatedEvent())
    }

    fun getGithubProfileUrl(pullRequest: GHPullRequest?): String? {
        return try {
            "$GITHUB_PROFILE_PREFIX${pullRequest?.user?.login}"
        } catch (e: Exception) {
            null
        }
    }

    //Posted events
    class UpdateRepositoryInfoTablesEvent(val tableModel: GHRepoTableModel)

    class BranchesLoadedEvent(val repoBranchesModel: GHBranchTableModel)
    class ReleasesLoadedEvent(val repoReleasesModel: GHReleaseTableModel)
    class NewOpenPullRequestsEvent(val pullRequest: GHPullRequestWrapper)
    class NewClosedPullRequestsEvent(val pullRequest: GHPullRequestWrapper)

    class BuildSucceededEventEvent
    class BuildFailedEventEvent(val message: String?)

    companion object {
        private val propertiesComponent = PropertiesComponent.getInstance()

        private const val GITHUB_PROFILE_PREFIX = "https://github.com/"

        const val LARGE_PAGE_SIZE = 500
        const val SMALL_PAGE_SIZE = 40

        const val RE_RUNNING_BUILD = "Re-running build..."

        const val CANCEL = "Cancel"
        const val RETRY = "Retry"
        const val OK = "OK"
        const val VIEW_BUILD = "View build"
        const val CLEAR_TOKEN = "Clear token"
        const val BRANCH_BUILD = "Branch build"
        const val BUILD_TRIGGERED = "Build triggered!"
        const val BUILD_TRIGGER_FAILED_MESSAGE = "Build trigger failed with message: "

        const val INPUT_REQUIRED = "Input required"

        const val REQUEST_CODE_LAUNCH_BUILD = 1
        const val REQUEST_CODE_MISSING_TRAVIS_TOKEN = 2
        const val REQUEST_CODE_MISSING_CIRCLE_TOKEN = 3
        const val REQUEST_CODE_BUILD_SUCCEEDED = 4
        const val REQUEST_CODE_BUILD_FAILED_UNDEFINED_CI_ENVIRONMENT = 5
        const val REQUEST_CODE_BUILD_FAILED = 6
    }
}
