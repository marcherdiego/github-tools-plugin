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
import com.nerdscorner.android.plugin.utils.GithubUtils
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.ThreadUtils
import com.nerdscorner.android.plugin.ci.TravisCi
import com.nerdscorner.android.plugin.utils.cancel
import org.greenrobot.eventbus.EventBus
import org.kohsuke.github.GHDirection
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHPullRequestQueryBuilder.Sort
import java.io.IOException
import java.util.ArrayList
import java.util.Date
import java.util.HashMap

abstract class BaseReposModel(val ghOrganization: GHOrganization) {
    lateinit var bus: EventBus

    //Loader threads
    protected var loaderThread: Thread? = null
    private var releasesLoaderThread: Thread? = null
    private var prsLoaderThread: Thread? = null
    private var branchesLoaderThread: Thread? = null

    val organizationName: String
        get() = ghOrganization.login

    var commentsUpdated: Boolean = false
    var currentRepository: GHRepositoryWrapper? = null
    var selectedRepoRow = -1

    private var currentBranchBuild: GHBranchWrapper? = null
    private var currentCiEnvironment: CiEnvironment? = null

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

    fun init() {
        commentsUpdated = false
        currentRepository = null
        selectedRepoRow = -1
        currentBranchBuild = null
        currentCiEnvironment = null
        ThreadUtils.cancelThreads(loaderThread, releasesLoaderThread, prsLoaderThread, branchesLoaderThread)
    }

    fun getCurrentRepoUrl() = currentRepository?.fullUrl

    @Throws(MissingTravisCiTokenException::class, MissingCircleCiTokenException::class)
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
            else -> {
                return
            }
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

    fun triggerPendingRebuild() {
        requestRebuild(currentBranchBuild ?: return)
        currentBranchBuild = null
    }

    fun cancelRebuildRequest() {
        currentCiEnvironment?.cancelRequest()
    }

    fun saveTravisToken(token: String?) {
        propertiesComponent.setValue(Strings.TRAVIS_CI_TOKEN_PROPERTY, token)
    }

    fun saveCircleToken(token: String?) {
        propertiesComponent.setValue(Strings.CIRCLE_CI_TOKEN_PROPERTY, token)
    }

    fun openBuildInBrowser(branch: GHBranchWrapper? = null) {
        GithubUtils.openWebLink((branch ?: currentBranchBuild)?.buildStatusUrl)
    }

    fun clearCiToken() {
        propertiesComponent.setValue(
                when {
                    currentBranchBuild?.circleBuild == true -> Strings.TRAVIS_CI_TOKEN_PROPERTY
                    currentBranchBuild?.travisBuild == true -> Strings.TRAVIS_CI_TOKEN_PROPERTY
                    else -> return
                },
                Strings.BLANK
        )
    }

    //Posted events
    class UpdateRepositoryInfoTablesEvent(val tableModel: GHRepoTableModel, val tooltips: HashMap<String, String>)

    class BranchesLoadedEvent(val repoBranchesModel: GHBranchTableModel)
    class ReleasesLoadedEvent(val repoReleasesModel: GHReleaseTableModel)
    class NewOpenPullRequestsEvent(val pullRequest: GHPullRequestWrapper)
    class NewClosedPullRequestsEvent(val pullRequest: GHPullRequestWrapper)

    class RepoNoReleasesYetEvent
    class RepoNeedsReleaseEvent
    class RepoDoesNotNeedReleaseEvent

    class BuildSucceededEventEvent
    class BuildFailedEventEvent(val message: String?)

    companion object {
        private val propertiesComponent = PropertiesComponent.getInstance()

        const val LARGE_PAGE_SIZE = 500
        const val SMALL_PAGE_SIZE = 40

        const val RE_RUNNING_BUILD = "Re-running build..."

        const val CANCEL = "Cancel"
        const val RETRY = "Retry"
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
        const val REQUEST_CODE_BUILD_FAILED = 5
    }
}
