package com.nerdscorner.android.plugin.github.ui.presenter

import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.ReleaseSkippedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.VersionBumpSkippedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.ChangelogsFetchedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.ChangelogFetchedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.ReleaseCreatedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.ReleasesCreatedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.ReposLoadedEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.VersionBumpCreatedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.VersionBumpsCreatedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.ReleaseCandidateTableModel
import com.nerdscorner.android.plugin.github.ui.tablemodels.VersionBumpsTableModel
import com.nerdscorner.android.plugin.github.ui.view.ExperimentalView
import com.nerdscorner.android.plugin.github.ui.view.ExperimentalView.AddLibraryClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.ExperimentalView.CreateAppsChangelogClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.ExperimentalView.CreateVersionBumpsClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.ExperimentalView.ReleaseLibrariesClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.ExperimentalView.RemoveLibraryClickedEvent
import com.nerdscorner.android.plugin.github.ui.windows.ChangelogResultDialog
import com.nerdscorner.android.plugin.github.ui.windows.ReleaseCandidatesResultDialog
import com.nerdscorner.android.plugin.github.ui.windows.VersionBumpsResultDialog
import com.nerdscorner.android.plugin.utils.MultilineStringLabel
import com.nerdscorner.android.plugin.utils.Strings
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class ExperimentalPresenter(private val view: ExperimentalView, private val model: ExperimentalModel, bus: EventBus) {
    init {
        view.bus = bus
        model.bus = bus
        bus.register(this)
        model.loadRepositories()
    }

    @Subscribe
    fun onCreateAppsChangelogButtonClicked(event: CreateAppsChangelogClickedEvent) {
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
    fun onChangelogsFetchedSuccessfully(event: ChangelogsFetchedSuccessfullyEvent) {
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
        if (model.includedLibraries.isNotEmpty()) {
            view.disableButtons()
            view.setAndroidMessagesVisibility(true)
            view.updateAndroidMessages("Creating libraries Release Candidates...")
            model.createLibrariesReleases()
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
            model.createVersionBumps()
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
