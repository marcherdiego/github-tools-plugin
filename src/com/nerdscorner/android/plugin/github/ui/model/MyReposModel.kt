package com.nerdscorner.android.plugin.github.ui.model

import com.nerdscorner.android.plugin.github.managers.GitHubManager
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.cancel
import java.util.ArrayList

class MyReposModel(selectedRepo: String)
    : BaseReposModel(selectedRepo) {
    override fun loadRepositories() {
        loaderThread.cancel()
        loaderThread = Thread {
            val myReposTableModel = GHRepoTableModel(ArrayList(), arrayOf(Strings.NAME))
            loadMyRepos(GitHubManager.myselfGitHub, myReposTableModel)
            bus.post(UpdateRepositoryInfoTablesEvent(myReposTableModel))
        }
        loaderThread?.start()
    }
}
