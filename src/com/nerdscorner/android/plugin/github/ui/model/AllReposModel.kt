package com.nerdscorner.android.plugin.github.ui.model

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.cancel
import org.kohsuke.github.GHOrganization
import java.util.ArrayList
import java.util.HashMap

class AllReposModel(ghOrganization: GHOrganization, selectedRepo: String) : BaseReposModel(selectedRepo, ghOrganization) {
    override fun loadRepositories() {
        loaderThread.cancel()
        loaderThread = Thread {
            val reposTableModel = GHRepoTableModel(ArrayList(), arrayOf(Strings.NAME))
            val tooltips = HashMap<String, String>()
            ghOrganization
                    .listRepositories()
                    .withPageSize(LARGE_PAGE_SIZE)
                    .forEach { repository ->
                        if (repository.isFork.not() && repository.fullName.startsWith(organizationName)) {
                            val ghRepositoryWrapper = GHRepositoryWrapper(repository)
                            reposTableModel.addRow(ghRepositoryWrapper)
                            tooltips[ghRepositoryWrapper.name] = ghRepositoryWrapper.description
                        }
                    }
            bus.post(UpdateRepositoryInfoTablesEvent(reposTableModel, tooltips))
        }
        loaderThread?.start()
    }
}
