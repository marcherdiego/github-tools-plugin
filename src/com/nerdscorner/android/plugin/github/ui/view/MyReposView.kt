package com.nerdscorner.android.plugin.github.ui.view

import org.greenrobot.eventbus.EventBus
import javax.swing.JLabel
import javax.swing.JTable

class MyReposView(
        bus: EventBus,
        reposTable: JTable,
        releasesTable: JTable,
        branchesTable: JTable,
        openPullRequestsTable: JTable,
        closedPullRequestsTable: JTable,
        repoComments: JLabel,
        dataColumn: Int
) : BaseReposView(bus, reposTable, releasesTable, branchesTable, openPullRequestsTable, closedPullRequestsTable, repoComments, dataColumn) {
}
