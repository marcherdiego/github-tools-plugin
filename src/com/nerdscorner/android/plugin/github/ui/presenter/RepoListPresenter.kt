package com.nerdscorner.android.plugin.github.ui.presenter

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.exceptions.MissingCircleCiTokenException
import com.nerdscorner.android.plugin.github.exceptions.MissingTravisCiTokenException
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.BranchesLoadedEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.BuildFailedEventEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.BuildSucceededEventEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.Companion.BRANCH_BUILD
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.Companion.BUILD_TRIGGERED
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.Companion.BUILD_TRIGGER_FAILED_MESSAGE
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.Companion.CANCEL
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.Companion.CLEAR_TOKEN
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.Companion.INPUT_REQUIRED
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.Companion.REQUEST_CODE_BUILD_FAILED
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.Companion.REQUEST_CODE_BUILD_SUCCEEDED
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.Companion.REQUEST_CODE_LAUNCH_BUILD
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.Companion.REQUEST_CODE_MISSING_CIRCLE_TOKEN
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.Companion.REQUEST_CODE_MISSING_TRAVIS_TOKEN
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.Companion.RETRY
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.Companion.RE_RUNNING_BUILD
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.Companion.VIEW_BUILD
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.NewClosedPullRequestsEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.NewOpenPullRequestsEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.ReleasesLoadedEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.RepoDoesNotNeedReleaseEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.RepoNeedsReleaseEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.RepoNoReleasesYetEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.UpdateRepositoryInfoTablesEvent
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHBranchTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHPullRequestTableModel
import com.nerdscorner.android.plugin.github.ui.view.ReposView
import com.nerdscorner.android.plugin.github.ui.view.ReposView.BranchClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.ReposView.PullRequestClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.ReposView.ReleaseClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.ReposView.RepoClickedEvent
import com.nerdscorner.android.plugin.github.ui.windows.ResultDialog.PrimaryButtonClickedEvent
import com.nerdscorner.android.plugin.github.ui.windows.ResultDialog.SecondaryButtonClickedEvent
import com.nerdscorner.android.plugin.github.ui.windows.SimpleInputDialog.InputEnteredEvent
import com.nerdscorner.android.plugin.utils.GithubUtils
import com.nerdscorner.android.plugin.utils.Strings
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN

class RepoListPresenter(
        private val view: ReposView,
        private val model: BaseReposModel,
        bus: EventBus
) {
    init {
        view.bus = bus
        model.bus = bus
        bus.register(this)
    }

    fun init() {
        model.cancel()
        view.init()
    }

    fun loadRepositories() {
        model.loadRepositories()
    }

    fun setSelectedRepo(selectedRepo: String) {
        view.selectedRepo = selectedRepo
    }

    @Subscribe(threadMode = MAIN)
    fun onUpdateRepositoryInfoTables(event: UpdateRepositoryInfoTablesEvent) {
        view.setPullRequestTableModels()
        view.updateRepositoryInfoTables(event.tableModel, event.tooltips)
        model.loadRepoReleasesAndBranches()
    }

    @Subscribe
    fun onReleasesLoaded(event: ReleasesLoadedEvent) {
        view.setReleasesTableModel(event.repoReleasesModel)
        model.loadPullRequests(view.latestReleaseDate)
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
            if (model.selectedRepoRow == event.row || event.row == -1) {
                return
            }
            view.clearTables()
            view.setRepoComments(null)
            model.currentRepository = view.getRepoAt(event.row, view.dataColumn) as GHRepositoryWrapper
            model.loadRepoReleasesAndBranches()
        } else if (event.clickCount == 2) {
            GithubUtils.openWebLink(model.getCurrentRepoUrl())
        }
        model.selectedRepoRow = event.row
    }

    @Subscribe
    fun onReleaseClicked(event: ReleaseClickedEvent) {
        GithubUtils.openWebLink(event.release?.fullUrl)
    }

    @Subscribe
    fun onPullRequestClicked(event: PullRequestClickedEvent) {
        if (event.column == GHPullRequestTableModel.COLUMN_CI_STATUS) {
            GithubUtils.openWebLink(event.pullRequest?.buildStatusUrl)
        } else {
            GithubUtils.openWebLink(event.pullRequest?.fullUrl)
        }
    }

    @Subscribe
    fun onBranchClicked(event: BranchClickedEvent) {
        when (event.column) {
            GHBranchTableModel.COLUMN_NAME -> GithubUtils.openWebLink(event.branch.url)
            GHBranchTableModel.COLUMN_STATUS -> model.openBuildInBrowser(event.branch)
            GHBranchTableModel.COLUMN_TRIGGER_BUILD -> {
                try {
                    model.requestRebuild(event.branch)
                    view.showTriggeringBuildLoadingDialog(BRANCH_BUILD, RE_RUNNING_BUILD, CANCEL, REQUEST_CODE_LAUNCH_BUILD)
                } catch (e: MissingCircleCiTokenException) {
                    view.requestTravisToken(INPUT_REQUIRED, Strings.ENTER_CIRCLE_CI_TOKEN, REQUEST_CODE_MISSING_CIRCLE_TOKEN)
                } catch (e: MissingTravisCiTokenException) {
                    view.requestTravisToken(INPUT_REQUIRED, Strings.ENTER_TRAVIS_CI_TOKEN, REQUEST_CODE_MISSING_TRAVIS_TOKEN)
                }
            }
        }
    }

    @Subscribe
    fun onPrimaryButtonClicked(event: PrimaryButtonClickedEvent) {
        when (event.requestCode) {
            REQUEST_CODE_LAUNCH_BUILD -> model.cancelRebuildRequest()
            REQUEST_CODE_BUILD_SUCCEEDED -> model.openBuildInBrowser()
            REQUEST_CODE_BUILD_FAILED -> {
                view.updateLoadingDialog(
                        message = RE_RUNNING_BUILD,
                        primaryActionText = CANCEL,
                        requestCode = REQUEST_CODE_LAUNCH_BUILD
                )
                model.triggerPendingRebuild()
            }
        }
    }

    @Subscribe
    fun onSecondaryButtonClicked(event: SecondaryButtonClickedEvent) {
        when (event.requestCode) {
            REQUEST_CODE_BUILD_FAILED -> model.clearCiToken()
        }
    }

    @Subscribe
    fun onBuildSucceededEvent(event: BuildSucceededEventEvent) {
        view.updateLoadingDialog(
                title = BUILD_TRIGGERED,
                message = BUILD_TRIGGERED,
                primaryActionText = VIEW_BUILD,
                requestCode = REQUEST_CODE_BUILD_SUCCEEDED
        )
    }

    @Subscribe
    fun onBuildFailedEvent(event: BuildFailedEventEvent) {
        view.updateLoadingDialog(
                message = "$BUILD_TRIGGER_FAILED_MESSAGE${event.message}",
                primaryActionText = RETRY,
                secondaryActionText = CLEAR_TOKEN,
                requestCode = REQUEST_CODE_BUILD_FAILED
        )
    }

    @Subscribe
    fun onInputEntered(event: InputEnteredEvent) {
        when (event.requestCode) {
            REQUEST_CODE_MISSING_TRAVIS_TOKEN -> {
                model.saveTravisToken(event.text)
                model.triggerPendingRebuild()
                view.showTriggeringBuildLoadingDialog(BRANCH_BUILD, RE_RUNNING_BUILD, CANCEL, REQUEST_CODE_LAUNCH_BUILD)
            }
            REQUEST_CODE_MISSING_CIRCLE_TOKEN -> {
                model.saveCircleToken(event.text)
                model.triggerPendingRebuild()
                view.showTriggeringBuildLoadingDialog(BRANCH_BUILD, RE_RUNNING_BUILD, CANCEL, REQUEST_CODE_LAUNCH_BUILD)
            }
        }
    }
}
