package com.nerdscorner.android.plugin.github.ui.controllers

import com.nerdscorner.android.plugin.github.ui.model.MyReposModel.UpdateRepositoryInfoTablesEvent
import com.nerdscorner.android.plugin.github.ui.model.MyReposModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.BaseModel
import com.nerdscorner.android.plugin.github.ui.view.MyReposView
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.kohsuke.github.GHMyself
import org.kohsuke.github.GHOrganization
import javax.swing.JLabel
import javax.swing.JTable

class MyReposController(
        reposTable: JTable,
        releases: JTable,
        branches: JTable,
        openPullRequests: JTable,
        closedPullRequests: JTable,
        comments: JLabel,
        myselfGitHub: GHMyself,
        ghOrganization: GHOrganization
) : BaseRepoListController<MyReposView, MyReposModel>(BaseModel.COLUMN_NAME) {
    init {
        view = MyReposView(bus, reposTable, releases, branches, openPullRequests, closedPullRequests, comments, BaseModel.COLUMN_NAME)
        model = MyReposModel(bus, ghOrganization, myselfGitHub)
    }

    @Subscribe(threadMode = MAIN)
    fun onUpdateRepositoryInfoTables(event: UpdateRepositoryInfoTablesEvent) {
        view.setPullRequestTableModels()
        view.updateRepositoryInfoTables(event.tableModel, event.tooltips)
        model.loadRepoReleasesAndBranches()
    }
}
