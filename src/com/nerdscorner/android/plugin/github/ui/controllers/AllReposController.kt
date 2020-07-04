package com.nerdscorner.android.plugin.github.ui.controllers

import com.nerdscorner.android.plugin.github.ui.model.AllReposModel
import com.nerdscorner.android.plugin.github.ui.model.AllReposModel.UpdateRepositoryInfoTablesEvent
import com.nerdscorner.android.plugin.github.ui.tablemodels.BaseModel
import com.nerdscorner.android.plugin.github.ui.view.AllReposView
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.kohsuke.github.GHOrganization
import javax.swing.JLabel
import javax.swing.JTable

class AllReposController(
        reposTable: JTable,
        releases: JTable,
        branches: JTable,
        openPullRequests: JTable,
        closedPullRequests: JTable,
        comments: JLabel,
        ghOrganization: GHOrganization
) : BaseRepoListController<AllReposView, AllReposModel>(BaseModel.COLUMN_NAME) {
    init {
        model = AllReposModel(bus, ghOrganization)
        view = AllReposView(bus, reposTable, releases, branches, openPullRequests, closedPullRequests, comments, BaseModel.COLUMN_NAME)
    }

    @Subscribe(threadMode = MAIN)
    fun onUpdateRepositoryInfoTables(event: UpdateRepositoryInfoTablesEvent) {
        view.setPullRequestTableModels()
        view.updateRepositoryInfoTables(event.tableModel, event.tooltips)
        model.loadRepoReleasesAndBranches(view.latestReleaseDate)
    }
}
