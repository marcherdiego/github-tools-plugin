package com.nerdscorner.android.plugin.github.ui.model

import com.nerdscorner.android.plugin.github.managers.GitHubManager
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.cancel
import org.kohsuke.github.GHMyself
import org.kohsuke.github.GHOrganization
import java.util.ArrayList

class AllReposModel(selectedRepo: String)
    : BaseReposModel(selectedRepo) {
    override fun loadRepositories() {
        loaderThread.cancel()
        loaderThread = Thread {
            val reposTableModel = GHRepoTableModel(ArrayList(), arrayOf(Strings.NAME))
            loadOrganizationRepos(GitHubManager.ghOrganization, reposTableModel)
            if (allowNonOrganizationRepos()) {
                loadMyRepos(GitHubManager.myselfGitHub, reposTableModel)
            }
            bus.post(UpdateRepositoryInfoTablesEvent(reposTableModel))
        }
        loaderThread?.start()
    }
}
