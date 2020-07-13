package com.nerdscorner.android.plugin.github.ui.presenter

import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.LibrariesFetchedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.model.ExperimentalModel.LibraryFetchedSuccessfullyEvent
import com.nerdscorner.android.plugin.github.ui.view.ExperimentalView
import com.nerdscorner.android.plugin.github.ui.view.ExperimentalView.CreateAppsChangelogButtonClickedEvent
import com.nerdscorner.android.plugin.github.ui.windows.ChangelogResultDialog
import com.nerdscorner.android.plugin.utils.Strings
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class ExperimentalPresenter(private val view: ExperimentalView, private val model: ExperimentalModel, bus: EventBus) {
    init {
        view.bus = bus
        model.bus = bus
        bus.register(this)
    }

    @Subscribe
    fun onCreateAppsChangelogButtonClicked(event: CreateAppsChangelogButtonClickedEvent) {
        view.setChangelogProgressVisibility(true)
        view.updateChangelogProgress("Fetching libraries changelog...")
        model.fetchAppsChangelog()
    }

    @Subscribe
    fun onLibraryFetchedSuccessfully(event: LibraryFetchedSuccessfullyEvent) {
        val progress = "%.2f".format(event.totalProgress)
        view.updateChangelogProgress("Completed: $progress% | Fetched ${event.libraryName}'s changelog")
    }

    @Subscribe
    fun onLibrariesFetchedSuccessfully(event: LibrariesFetchedSuccessfullyEvent) {
        view.setChangelogProgressVisibility(false)

        val resultDialog = ChangelogResultDialog(model.getLibrariesChangelog())
        resultDialog.pack()
        resultDialog.setLocationRelativeTo(null)
        resultDialog.title = Strings.CHANGELOG
        resultDialog.isResizable = true
        resultDialog.isVisible = true
    }
}
