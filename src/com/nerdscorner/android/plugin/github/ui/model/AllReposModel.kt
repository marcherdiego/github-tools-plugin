package com.nerdscorner.android.plugin.github.ui.model

import com.nerdscorner.android.plugin.github.managers.GitHubManager
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.cancel
import com.nerdscorner.android.plugin.utils.startThread
import java.util.ArrayList

class AllReposModel(selectedRepo: String)
    : BaseReposModel(selectedRepo) {
    override fun loadRepositories() {
        loaderThread.cancel()
        loaderThread = startThread {
            val reposTableModel = GHRepoTableModel(ArrayList(), arrayOf(Strings.NAME))
            loadOrganizationRepos(reposTableModel)
            if (canShowReposFromOutsideOrganization()) {
                loadMyRepos(GitHubManager.myselfGitHub, reposTableModel)
            }
            bus.post(UpdateRepositoryInfoTablesEvent(reposTableModel))
        }
    }
}
