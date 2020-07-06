package com.nerdscorner.android.plugin.github.ui.model

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.cancel
import org.kohsuke.github.GHMyself
import org.kohsuke.github.GHOrganization
import java.util.ArrayList
import java.util.HashMap

class MyReposModel(ghOrganization: GHOrganization, private val myselfGitHub: GHMyself) : BaseReposModel(ghOrganization) {
    override fun loadRepositories() {
        loaderThread.cancel()
        loaderThread = Thread {
            val myReposTableModel = GHRepoTableModel(ArrayList(), arrayOf(Strings.NAME))
            val tooltips = HashMap<String, String>()
            myselfGitHub
                    .listSubscriptions()
                    .withPageSize(LARGE_PAGE_SIZE)
                    .forEach { repository ->
                        if (repository.isFork.not() && repository.fullName.startsWith(organizationName)) {
                            myReposTableModel.addRow(GHRepositoryWrapper(repository))
                        }
                    }
            bus.post(UpdateRepositoryInfoTablesEvent(myReposTableModel, tooltips))
        }
        loaderThread?.start()
    }
}
