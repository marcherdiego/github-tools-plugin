package com.nerdscorner.android.plugin.github.ui.view

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.utils.JTableUtils
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleDoubleClickAdapter
import com.nerdscorner.android.plugin.utils.onClick
import org.greenrobot.eventbus.EventBus
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.ListSelectionModel

class ExperimentalView(
        createAppsChangelogButton: JButton,
        private val androidMessages: JLabel,
        addLibraryButton: JButton,
        removeLibraryButton: JButton,
        private val excludedRepos: JTable,
        private val includedReposLabel: JLabel,
        private val includedRepos: JTable,
        releaseLibrariesButton: JButton,
        createVersionBumpsButton: JButton) {

    lateinit var bus: EventBus

    init {
        createAppsChangelogButton.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                bus.post(CreateAppsChangelogClickedEvent())
            }
        })
        addLibraryButton.onClick {
            bus.post(AddLibraryClickedEvent())
        }
        removeLibraryButton.onClick {
            bus.post(RemoveLibraryClickedEvent())
        }
        releaseLibrariesButton.onClick {
            bus.post(ReleaseLibrariesClickedEvent())
        }
        createVersionBumpsButton.onClick {
            bus.post(CreateVersionBumpsClickedEvent())
        }
        excludedRepos.addMouseListener(object : SimpleDoubleClickAdapter() {
            override fun onDoubleClick(row: Int, column: Int) {
                bus.post(AddLibraryClickedEvent())
            }
        })
        includedRepos.addMouseListener(object : SimpleDoubleClickAdapter() {
            override fun onDoubleClick(row: Int, column: Int) {
                bus.post(RemoveLibraryClickedEvent())
            }
        })
        excludedRepos.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        includedRepos.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        setAndroidMessagesVisibility(false)
    }

    fun setAndroidMessagesVisibility(visible: Boolean) {
        androidMessages.isVisible = visible
    }

    fun updateAndroidMessages(message: String) {
        androidMessages.text = message
    }

    fun updateExcludedLibraries(model: GHRepoTableModel) {
        excludedRepos.model = model
    }

    fun updateIncludedLibraries(model: GHRepoTableModel) {
        includedRepos.model = model
    }

    fun getSelectedExcludedLibrary(): GHRepositoryWrapper? = JTableUtils.getSelectedItem(excludedRepos) as? GHRepositoryWrapper

    fun getSelectedIncludedLibrary(): GHRepositoryWrapper? = JTableUtils.getSelectedItem(includedRepos) as? GHRepositoryWrapper

    fun updateIncludedLibrariesLabel(text: String) {
        includedReposLabel.text = text
    }

    class CreateAppsChangelogClickedEvent
    class AddLibraryClickedEvent
    class RemoveLibraryClickedEvent
    class ReleaseLibrariesClickedEvent
    class CreateVersionBumpsClickedEvent
}
