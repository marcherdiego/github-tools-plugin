package com.nerdscorner.android.plugin.github.ui.controllers

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.BranchesLoadedEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.NewClosedPullRequestsEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.NewOpenPullRequestsEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.ReleasesLoadedEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.RepoDoesNotNeedReleaseEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.RepoNeedsReleaseEvent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel.RepoNoReleasesYetEvent
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHBranchTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHPullRequestTableModel
import com.nerdscorner.android.plugin.github.ui.view.BaseReposView
import com.nerdscorner.android.plugin.github.ui.view.BaseReposView.BranchClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.BaseReposView.PullRequestClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.BaseReposView.ReleaseClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.BaseReposView.RepoClickedEvent
import com.nerdscorner.android.plugin.utils.GithubUtils
import com.nerdscorner.android.plugin.utils.Strings
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

abstract class BaseRepoListController<V : BaseReposView, M : BaseReposModel> internal constructor(private val dataColumn: Int) {
    protected val bus: EventBus = EventBus()

    lateinit var view: V
    lateinit var model: M

    fun init() {
        bus.register(this)
        view.init()
    }

    fun cancel() {
        bus.unregister(this)
        model.cancel()
    }


    fun loadRepositories() {
        model.loadRepositories()
    }

    fun setSelectedRepo(selectedRepo: String) {
        view.selectedRepo = selectedRepo
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
            model.currentRepository = view.getRepoAt(event.row, dataColumn) as GHRepositoryWrapper
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
            GHBranchTableModel.COLUMN_STATUS -> event.branch.openBuildInBrowser()
            GHBranchTableModel.COLUMN_TRIGGER_BUILD -> event.branch.triggerBuild()
        }
    }
}
