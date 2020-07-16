package com.nerdscorner.android.plugin.github.ui.presenter

import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.CreatingReleaseCandidateEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.CreatingVersionBumpEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.LibrariesFetchedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.LibraryFetchedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.ReleaseCreatedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.ReleasesCreatedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.ReleasesCreationFailedEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.ReposLoadedEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.VersionBumpCreatedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.VersionBumpCreationFailedEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.VersionBumpsCreatedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.github.ui.view.ExperimentalView
import com.nerdscorner.android.plugin.github.ui.view.ExperimentalView.AddLibraryClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.ExperimentalView.CreateAppsChangelogClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.ExperimentalView.CreateVersionBumpsClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.ExperimentalView.ReleaseLibrariesClickedEvent
import com.nerdscorner.android.plugin.github.ui.view.ExperimentalView.RemoveLibraryClickedEvent
import com.nerdscorner.android.plugin.github.ui.windows.ChangelogResultDialog
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
        view.setAndroidMessagesVisibility(true)
        view.updateAndroidMessages("Fetching libraries changelog...")
        model.fetchAppsChangelog()
    }

    @Subscribe
    fun onLibraryFetchedSuccessfully(event: LibraryFetchedSuccessfullyEvent) {
        view.updateAndroidMessages("Completed: ${event.totalProgress.toInt()}% | Fetched ${event.libraryName}'s changelog.")
    }

    @Subscribe
    fun onLibrariesFetchedSuccessfully(event: LibrariesFetchedSuccessfullyEvent) {
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
        view.setAndroidMessagesVisibility(true)
        view.updateAndroidMessages("Creating libraries release candidates...")
        model.createLibrariesReleases()
    }

    @Subscribe
    fun onCreatingReleaseCandidate(event: CreatingReleaseCandidateEvent) {
        view.updateAndroidMessages("Completed: ${event.totalProgress.toInt()}% | Creating ${event.libraryName}'s RC...")
    }

    @Subscribe
    fun onReleaseCreatedSuccessfully(event: ReleaseCreatedSuccessfullyEvent) {
        view.updateAndroidMessages("Completed: ${event.totalProgress.toInt()}% | Created ${event.libraryName}'s RC.")
    }

    @Subscribe
    fun onReleasesCreatedSuccessfully(event: ReleasesCreatedSuccessfullyEvent) {
        view.setAndroidMessagesVisibility(false)
    }

    @Subscribe
    fun onReleasesCreationFailed(event: ReleasesCreationFailedEvent) {
        view.updateAndroidMessages("Failed: ${event.message}")
    }

    @Subscribe
    fun onCreateVersionBumpsClicked(event: CreateVersionBumpsClickedEvent) {
        view.setAndroidMessagesVisibility(true)
        view.updateAndroidMessages("Creating libraries version bumps...")
        model.createVersionBumps()
    }

    @Subscribe
    fun onCreatingVersionBump(event: CreatingVersionBumpEvent) {
        view.updateAndroidMessages("Completed: ${event.totalProgress.toInt()}% | Creating ${event.libraryName}'s version bump...")
    }

    @Subscribe
    fun onVersionBumpCreatedSuccessfully(event: VersionBumpCreatedSuccessfullyEvent) {
        view.updateAndroidMessages("Completed: ${event.totalProgress.toInt()}% | Created ${event.libraryName}'s version bump.")
    }

    @Subscribe
    fun onVersionBumpsCreatedSuccessfully(event: VersionBumpsCreatedSuccessfullyEvent) {
        view.setAndroidMessagesVisibility(false)
    }

    @Subscribe
    fun onVersionBumpCreationFailed(event: VersionBumpCreationFailedEvent) {
        view.updateAndroidMessages("Failed: ${event.message}")
    }

    private fun refreshLists() {
        val excludeLibraries = model
                .allLibraries
                .subtract(model.includedLibraries)
                .sortedBy {
                    it.name
                }
                .toMutableList()
        val excludedLibrariesModel = GHRepoTableModel(excludeLibraries,  arrayOf(Strings.NAME))
        view.updateExcludedLibraries(excludedLibrariesModel)

        val includeLibraries = model
                .includedLibraries
                .sortedBy {
                    it.name
                }
                .toMutableList()
        val includedLibrariesModel = GHRepoTableModel(includeLibraries,  arrayOf(Strings.NAME))
        view.updateIncludedLibraries(includedLibrariesModel)
    }
}