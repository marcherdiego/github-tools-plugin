package com.nerdscorner.android.plugin.github.ui.model

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.ui.controllers.BaseRepoListController
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.ThreadUtils
import com.nerdscorner.android.plugin.utils.cancel
import org.greenrobot.eventbus.EventBus
import org.kohsuke.github.GHOrganization
import java.util.ArrayList
import java.util.Date
import java.util.HashMap

class AllReposModel(bus: EventBus, ghOrganization: GHOrganization) : BaseReposModel(bus, ghOrganization) {
    var commentsUpdated: Boolean = false

    override fun loadRepositories() {
        loaderThread.cancel()
        loaderThread = Thread {
            val reposTableModel = GHRepoTableModel(ArrayList(), arrayOf(Strings.NAME))
            val tooltips = HashMap<String, String>()
            ghOrganization
                    .listRepositories()
                    .withPageSize(BaseRepoListController.LARGE_PAGE_SIZE)
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

    override fun loadRepoReleasesAndBranches(latestReleaseDate: Date?) {
        try {
            commentsUpdated = false
            ThreadUtils.cancelThreads(releasesLoaderThread, branchesLoaderThread)

            branchesLoaderThread = Thread {
                try {
                    loadBranches()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            releasesLoaderThread = Thread {
                try {
                    loadReleases()
                    loadPullRequests(latestReleaseDate)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            branchesLoaderThread?.start()
            releasesLoaderThread?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    class UpdateRepositoryInfoTablesEvent(val tableModel: GHRepoTableModel, val tooltips: HashMap<String, String>)
}
