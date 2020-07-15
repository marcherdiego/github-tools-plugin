package com.nerdscorner.android.plugin.github.ui.view

import com.nerdscorner.android.plugin.utils.onClick
import org.greenrobot.eventbus.EventBus
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList

class ExperimentalView(
        createAppsChangelogButton: JButton,
        private val changelogProgress: JLabel,
        addLibraryButton: JButton,
        removeLibraryButton: JButton,
        private val excludedRepos: JList<*>,
        private val includedRepos: JList<*>,
        releaseLibrariesButton: JButton) {

    lateinit var bus: EventBus

    init {
        createAppsChangelogButton.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                bus.post(CreateAppsChangelogButtonClickedEvent())
            }
        })
        addLibraryButton.onClick {
            bus.post(AddLibraryButtonClickedEvent())
        }
        removeLibraryButton.onClick {
            bus.post(RemoveLibraryButtonEvent())
        }
        releaseLibrariesButton.onClick {
            bus.post(ReleaseLibrariesButtonEvent())
        }
        setChangelogProgressVisibility(false)
    }

    fun setChangelogProgressVisibility(visible: Boolean) {
        changelogProgress.isVisible = visible
    }

    fun updateChangelogProgress(message: String) {
        changelogProgress.text = message
    }

    fun updateExcludedLibraries(model: DefaultListModel<String>) {
        excludedRepos.model = model
    }

    fun updateIncludedLibraries(model: DefaultListModel<String>) {
        includedRepos.model = model
    }

    fun getSelectedExcludedLibrary(): String? = excludedRepos.selectedValue?.toString()

    fun getSelectedIncludedLibrary(): String? = includedRepos.selectedValue?.toString()

    class CreateAppsChangelogButtonClickedEvent
    class AddLibraryButtonClickedEvent
    class RemoveLibraryButtonEvent
    class ReleaseLibrariesButtonEvent
}
