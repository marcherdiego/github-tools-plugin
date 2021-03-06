package com.nerdscorner.android.plugin.github.ui.presenter

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.exceptions.MissingCircleCiTokenException
import com.nerdscorner.android.plugin.github.exceptions.MissingTravisCiTokenException
import com.nerdscorner.android.plugin.github.exceptions.UndefinedCiEnvironmentException
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
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.Companion.OK
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.Companion.REQUEST_CODE_BUILD_FAILED
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.Companion.REQUEST_CODE_BUILD_FAILED_UNDEFINED_CI_ENVIRONMENT
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
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.UpdateRepositoryInfoTablesEvent
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHBranchTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHClosedPullRequestTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHOpenPullRequestTableModel
import com.nerdscorner.android.plugin.github.ui.view.ReposView
import com.nerdscorner.android.plugin.github.ui.view.ReposView.BranchClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.ReposView.ClosedPullRequestClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.ReposView.OpenPullRequestClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.ReposView.ReleaseClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.ReposView.RepoClickedEvent
import com.nerdscorner.android.plugin.github.ui.windows.ResultDialog.PrimaryButtonClickedEvent
import com.nerdscorner.android.plugin.github.ui.windows.ResultDialog.SecondaryButtonClickedEvent
import com.nerdscorner.android.plugin.github.ui.windows.SimpleInputDialog.InputEnteredEvent
import com.nerdscorner.android.plugin.utils.BrowserUtils
import com.nerdscorner.android.plugin.utils.Strings
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN

class RepoListPresenter(private val view: ReposView, private val model: BaseReposModel, bus: EventBus) {
    init {
        view.bus = bus
        model.bus = bus
        bus.register(this)
    }

    fun init() {
        model.init()
        view.init()
    }

    fun loadRepositories() {
        model.loadRepositories()
    }

    @Subscribe(threadMode = MAIN)
    fun onUpdateRepositoryInfoTables(event: UpdateRepositoryInfoTablesEvent) {
        view.setPullRequestTableModels()
        view.updateRepositoryInfoTables(model.selectedRepo, event.tableModel)
        model.loadRepoTablesData(view.getOpenPullRequestTableModel(), view.getClosedPullRequestTableModel())
    }

    @Subscribe(threadMode = MAIN)
    fun onReleasesLoaded(event: ReleasesLoadedEvent) {
        view.setReleasesTableModel(event.repoReleasesModel)
    }

    @Subscribe(threadMode = MAIN)
    fun onBranchesLoaded(event: BranchesLoadedEvent) {
        view.setBranchesTableModel(event.repoBranchesModel)
    }

    @Subscribe(threadMode = MAIN)
    fun onNewOpenPullRequests(event: NewOpenPullRequestsEvent) {
        view.addOpenPullRequest(event.pullRequest)
    }

    @Subscribe(threadMode = MAIN)
    fun onNewClosedPullRequests(event: NewClosedPullRequestsEvent) {
        view.addClosedPullRequest(event.pullRequest)
    }

    @Subscribe(threadMode = MAIN)
    fun onRepoClicked(event: RepoClickedEvent) {
        if (event.clickCount == 1) {
            if (model.selectedRepoRow == event.row || event.row == -1) {
                return
            }
            model.currentRepository = view.getRepoAt(event.row, view.dataColumn) as GHRepositoryWrapper
            model.loadRepoTablesData(view.getOpenPullRequestTableModel(), view.getClosedPullRequestTableModel())
            view.clearTables()
        } else if (event.clickCount == 2) {
            BrowserUtils.openWebLink(model.getCurrentRepoUrl())
        }
        model.selectedRepoRow = event.row
    }

    @Subscribe(threadMode = MAIN)
    fun onReleaseClicked(event: ReleaseClickedEvent) {
        BrowserUtils.openWebLink(event.release?.fullUrl)
    }

    @Subscribe(threadMode = MAIN)
    fun onOpenPullRequestClicked(event: OpenPullRequestClickedEvent) {
        when (event.column) {
            GHOpenPullRequestTableModel.COLUMN_CI_STATUS -> BrowserUtils.openWebLink(event.pullRequest?.buildStatusUrl)
            GHOpenPullRequestTableModel.COLUMN_AUTHOR -> BrowserUtils.openWebLink(model.getGithubProfileUrl(event.pullRequest?.ghPullRequest))
            else -> BrowserUtils.openWebLink(event.pullRequest?.fullUrl)
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onClosedPullRequestClicked(event: ClosedPullRequestClickedEvent) {
        when (event.column) {
            GHClosedPullRequestTableModel.COLUMN_CI_STATUS -> BrowserUtils.openWebLink(event.pullRequest?.buildStatusUrl)
            GHClosedPullRequestTableModel.COLUMN_AUTHOR -> BrowserUtils.openWebLink(model.getGithubProfileUrl(event.pullRequest?.ghPullRequest))
            else -> BrowserUtils.openWebLink(event.pullRequest?.fullUrl)
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onBranchClicked(event: BranchClickedEvent) {
        when (event.column) {
            GHBranchTableModel.COLUMN_NAME -> BrowserUtils.openWebLink(event.branch.url)
            GHBranchTableModel.COLUMN_STATUS -> model.openBuildInBrowser(event.branch)
            GHBranchTableModel.COLUMN_TRIGGER_BUILD -> {
                try {
                    model.requestRebuild(event.branch)
                    view.showResultDialog(BRANCH_BUILD, RE_RUNNING_BUILD, CANCEL, REQUEST_CODE_LAUNCH_BUILD)
                } catch (e: MissingCircleCiTokenException) {
                    view.requestTravisToken(INPUT_REQUIRED, Strings.ENTER_CIRCLE_CI_TOKEN, REQUEST_CODE_MISSING_CIRCLE_TOKEN)
                } catch (e: MissingTravisCiTokenException) {
                    view.requestTravisToken(INPUT_REQUIRED, Strings.ENTER_TRAVIS_CI_TOKEN, REQUEST_CODE_MISSING_TRAVIS_TOKEN)
                } catch (e: UndefinedCiEnvironmentException) {
                    view.showResultDialog(
                            title = BRANCH_BUILD,
                            message = "$BUILD_TRIGGER_FAILED_MESSAGE Undefined CI Environment",
                            primaryActionText = OK,
                            requestCode = REQUEST_CODE_BUILD_FAILED_UNDEFINED_CI_ENVIRONMENT
                    )
                }
            }
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onPrimaryButtonClicked(event: PrimaryButtonClickedEvent) {
        when (event.requestCode) {
            REQUEST_CODE_LAUNCH_BUILD -> model.cancelRebuildRequest()
            REQUEST_CODE_BUILD_SUCCEEDED -> {
                model.openBuildInBrowser()
                model.clearCurrentBranchBuild()
            }
            REQUEST_CODE_BUILD_FAILED -> {
                view.updateResultDialog(
                        message = RE_RUNNING_BUILD,
                        primaryActionText = CANCEL,
                        requestCode = REQUEST_CODE_LAUNCH_BUILD
                )
                model.triggerPendingRebuild()
            }
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onSecondaryButtonClicked(event: SecondaryButtonClickedEvent) {
        when (event.requestCode) {
            REQUEST_CODE_BUILD_FAILED -> model.clearCiToken()
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onBuildSucceededEvent(event: BuildSucceededEventEvent) {
        view.updateResultDialog(
                title = BUILD_TRIGGERED,
                message = BUILD_TRIGGERED,
                primaryActionText = VIEW_BUILD,
                requestCode = REQUEST_CODE_BUILD_SUCCEEDED
        )
    }

    @Subscribe(threadMode = MAIN)
    fun onBuildFailedEvent(event: BuildFailedEventEvent) {
        view.updateResultDialog(
                message = "$BUILD_TRIGGER_FAILED_MESSAGE${event.message}",
                primaryActionText = RETRY,
                secondaryActionText = CLEAR_TOKEN,
                requestCode = REQUEST_CODE_BUILD_FAILED
        )
    }

    @Subscribe(threadMode = MAIN)
    fun onInputEntered(event: InputEnteredEvent) {
        when (event.requestCode) {
            REQUEST_CODE_MISSING_TRAVIS_TOKEN -> model.saveTravisToken(event.text)
            REQUEST_CODE_MISSING_CIRCLE_TOKEN -> model.saveCircleToken(event.text)
            else -> return
        }
        model.triggerPendingRebuild()
        view.showResultDialog(BRANCH_BUILD, RE_RUNNING_BUILD, CANCEL, REQUEST_CODE_LAUNCH_BUILD)
    }
}
