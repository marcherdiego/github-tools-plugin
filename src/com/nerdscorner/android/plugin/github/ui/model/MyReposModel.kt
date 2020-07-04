package com.nerdscorner.android.plugin.github.ui.model

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.ui.controllers.BaseRepoListController
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.cancel
import org.greenrobot.eventbus.EventBus
import org.kohsuke.github.GHMyself
import org.kohsuke.github.GHOrganization
import java.util.ArrayList
import java.util.HashMap

class MyReposModel(bus: EventBus, ghOrganization: GHOrganization, private val myselfGitHub: GHMyself) : BaseReposModel(bus, ghOrganization) {
    override fun loadRepositories() {
        loaderThread.cancel()
        loaderThread = Thread {
            val myReposTableModel = GHRepoTableModel(ArrayList(), arrayOf(Strings.NAME))
            val tooltips = HashMap<String, String>()
            myselfGitHub
                    .listSubscriptions()
                    .withPageSize(BaseRepoListController.LARGE_PAGE_SIZE)
                    .forEach { repository ->
                        if (repository.isFork.not() && repository.fullName.startsWith(organizationName)) {
                            myReposTableModel.addRow(GHRepositoryWrapper(repository))
                        }
                    }
            bus.post(UpdateRepositoryInfoTablesEvent(myReposTableModel, tooltips))
        }
        loaderThread?.start()
    }

    class UpdateRepositoryInfoTablesEvent(val tableModel: GHRepoTableModel, val tooltips: HashMap<String, String>)
}
