package com.nerdscorner.android.plugin.github.ui.presenter

import com.nerdscorner.android.plugin.github.ui.model.DeploymentModel
import com.nerdscorner.android.plugin.github.ui.model.DeploymentModel.ReleaseSkippedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.DeploymentModel.VersionBumpSkippedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.DeploymentModel.AllChangelogFetchedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.DeploymentModel.ChangelogFetchedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.DeploymentModel.ReleaseCreatedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.DeploymentModel.ReleasesCreatedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.DeploymentModel.ReposLoadedEvent
import com.nerdscorner.android.plugin.github.ui.model.DeploymentModel.VersionBumpCreatedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.DeploymentModel.VersionBumpsCreatedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.ReleaseCandidateTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.VersionBumpsTableModel
import com.nerdscorner.android.plugin.github.ui.view.DeploymentView
import com.nerdscorner.android.plugin.github.ui.view.DeploymentView.AddLibraryClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.DeploymentView.CreateAppsChangelogClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.DeploymentView.CreateVersionBumpsClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.DeploymentView.OpenReleaseProcessWikiClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.DeploymentView.ReleaseLibrariesClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.DeploymentView.RemoveLibraryClickedEvent
import com.nerdscorner.android.plugin.github.ui.windows.ChangelogResultDialog
import com.nerdscorner.android.plugin.github.ui.windows.ReleaseCandidatesResultDialog
import com.nerdscorner.android.plugin.github.ui.windows.VersionBumpsResultDialog
import com.nerdscorner.android.plugin.utils.BrowserUtils
import com.nerdscorner.android.plugin.utils.MultilineStringLabel
import com.nerdscorner.android.plugin.utils.Strings
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class DeploymentPresenter(private val view: DeploymentView, private val model: DeploymentModel, bus: EventBus) {
    init {
        view.bus = bus
        model.bus = bus
        bus.register(this)
        view.setReviewerTeams(model.getReviewerTeamsName())
        view.setIndividualReviewers(model.getIndividualReviewers())
    }

    fun loadRepositories() {
        model.loadRepositories()
    }

    @Subscribe
    fun onCreateAppsChangelogButtonClicked(event: CreateAppsChangelogClickedEvent) {
        model.saveReviewers(view.getReviewerTeams(), view.getIndividualReviewers())
        if (model.includedLibraries.isNotEmpty()) {
            view.disableButtons()
            view.setAndroidMessagesVisibility(true)
            view.updateAndroidMessages("Fetching libraries changelog...")
            model.fetchAppsChangelog()
        }
    }

    @Subscribe
    fun onChangelogFetchedSuccessfully(event: ChangelogFetchedSuccessfullyEvent) {
        val message = "<html> Completed: ${event.totalProgress.toInt()}% <br> Fetched ${event.libraryName}'s changelog. </html>"
        view.updateAndroidMessages(message)
    }

    @Subscribe
    fun onAllChangelogFetchedSuccessfully(event: AllChangelogFetchedSuccessfullyEvent) {
        view.enableButtons()
        view.setAndroidMessagesVisibility(false)

        val resultDialog = ChangelogResultDialog(model.getLibrariesChangelog())
        resultDialog.pack()
        resultDialog.setLocationRelativeTo(null)
        resultDialog.title = Strings.CHANGELOG
        resultDialog.isResizable = true
        resultDialog.isVisible = true
    }

    @Subscribe
    fun onReposLoaded(event: ReposLoadedEvent) {
        refreshLists()
    }

    @Subscribe
    fun onAddLibraryClicked(event: AddLibraryClickedEvent) {
        model.addLibrary(view.getSelectedExcludedLibrary() ?: return)
        refreshLists()
    }

    @Subscribe
    fun onRemoveLibraryClicked(event: RemoveLibraryClickedEvent) {
        model.removeLibrary(view.getSelectedIncludedLibrary() ?: return)
        refreshLists()
    }

    @Subscribe
    fun onReleaseLibrariesClicked(event: ReleaseLibrariesClickedEvent) {
        model.saveReviewers(view.getReviewerTeams(), view.getIndividualReviewers())
        if (model.includedLibraries.isNotEmpty()) {
            view.disableButtons()
            view.setAndroidMessagesVisibility(true)
            view.updateAndroidMessages("Creating libraries Release Candidates...")
            model.createLibrariesReleases(view.getReviewerTeams(), view.getIndividualReviewers())
        }
    }

    @Subscribe
    fun onReleaseSkippedSuccessfully(event: ReleaseSkippedSuccessfullyEvent) {
        view.updateAndroidMessages(
                MultilineStringLabel("Release Candidates | Completed: ${event.totalProgress.toInt()}%")
                        .addLine("Skipped ${event.libraryName}'s RC...")
                        .build()
        )
    }

    @Subscribe
    fun onReleaseCreatedSuccessfully(event: ReleaseCreatedSuccessfullyEvent) {
        view.updateAndroidMessages(
                MultilineStringLabel("Release Candidates | Completed: ${event.totalProgress.toInt()}%")
                        .addLine("Created ${event.libraryName}'s RC...")
                        .build()
        )
    }

    @Subscribe
    fun onReleasesCreatedSuccessfully(event: ReleasesCreatedSuccessfullyEvent) {
        view.enableButtons()
        view.setAndroidMessagesVisibility(false)
        val rcCreatedDialog = ReleaseCandidatesResultDialog()
                .setReposTableModel(
                        ReleaseCandidateTableModel(
                                model.includedLibraries,
                                event.releasedLibraries,
                                arrayOf(Strings.NAME, Strings.VERSION, Strings.STATUS, Strings.PULL_REQUEST)
                        )
                )
        rcCreatedDialog.pack()
        rcCreatedDialog.setLocationRelativeTo(null)
        rcCreatedDialog.title = Strings.RELEASES
        rcCreatedDialog.isResizable = true
        rcCreatedDialog.isVisible = true
    }

    @Subscribe
    fun onCreateVersionBumpsClicked(event: CreateVersionBumpsClickedEvent) {
        if (model.includedLibraries.isNotEmpty()) {
            view.disableButtons()
            view.setAndroidMessagesVisibility(true)
            view.updateAndroidMessages("Creating libraries Version Bumps...")
            model.createVersionBumps(view.getReviewerTeams(), view.getIndividualReviewers())
        }
    }

    @Subscribe
    fun onVersionBumpSkippedSuccessfully(event: VersionBumpSkippedSuccessfullyEvent) {
        view.updateAndroidMessages(
                MultilineStringLabel("Version Bumps | Completed: ${event.totalProgress.toInt()}%")
                        .addLine("Skipped ${event.libraryName}'s version bump...")
                        .build()
        )
    }

    @Subscribe
    fun onVersionBumpCreatedSuccessfully(event: VersionBumpCreatedSuccessfullyEvent) {
        view.updateAndroidMessages(
                MultilineStringLabel("Version Bumps | Completed: ${event.totalProgress.toInt()}%")
                        .addLine("Created ${event.libraryName}'s version bump...")
                        .build()
        )
    }

    @Subscribe
    fun onVersionBumpsCreatedSuccessfully(event: VersionBumpsCreatedSuccessfullyEvent) {
        view.enableButtons()
        view.setAndroidMessagesVisibility(false)
        val versionBumpsCreatedDialog = VersionBumpsResultDialog()
                .setReposTableModel(
                        VersionBumpsTableModel(
                                model.includedLibraries,
                                event.bumpedLibraries,
                                arrayOf(Strings.NAME, Strings.PREVIOUS_VERSION, Strings.NEXT_VERSION, Strings.STATUS, Strings.PULL_REQUEST)
                        )
                )
        versionBumpsCreatedDialog.pack()
        versionBumpsCreatedDialog.setLocationRelativeTo(null)
        versionBumpsCreatedDialog.title = Strings.VERSION_BUMPS
        versionBumpsCreatedDialog.isResizable = true
        versionBumpsCreatedDialog.isVisible = true
    }

    @Subscribe
    fun onOpenReleaseProcessWikiClicked(event: OpenReleaseProcessWikiClickedEvent) {
        BrowserUtils.openWebLink(model.getReleaseProcessWikiPage())
    }

    private fun refreshLists() {
        val excludeLibraries = model
                .allLibraries
                .subtract(model.includedLibraries)
                .sortedBy {
                    it.name
                }
                .toMutableList()
        val excludedLibrariesModel = GHRepoTableModel(excludeLibraries, arrayOf(Strings.NAME))
        view.updateExcludedLibraries(excludedLibrariesModel)

        val includeLibraries = model
                .includedLibraries
                .sortedBy {
                    it.name
                }
                .toMutableList()
        val includedLibrariesModel = GHRepoTableModel(includeLibraries, arrayOf(Strings.NAME))
        view.updateIncludedLibraries(includedLibrariesModel)

        view.updateIncludedLibrariesLabel("Included repos (${includeLibraries.size})")
    }
}
