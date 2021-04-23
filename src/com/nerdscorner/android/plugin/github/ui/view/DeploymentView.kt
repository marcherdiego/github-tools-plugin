package com.nerdscorner.android.plugin.github.ui.view

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel
import com.nerdscorner.android.plugin.utils.JTableUtils
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleDoubleClickAdapter
import com.nerdscorner.android.plugin.utils.onClick
import org.greenrobot.eventbus.EventBus
import java.awt.font.TextAttribute
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.ListSelectionModel

class DeploymentView(
        private val createAppsChangelogButton: JButton,
        private val androidMessages: JLabel,
        private val addLibraryButton: JButton,
        private val removeLibraryButton: JButton,
        private val excludedRepos: JTable,
        private val includedReposLabel: JLabel,
        private val includedRepos: JTable,
        private val releaseLibrariesButton: JButton,
        private val createVersionBumpsButton: JButton,
        private val reviewerTeam: JTextField,
        private val individualReviewers: JTextField,
        releaseProcessLink: JLabel) {

    lateinit var bus: EventBus

    init {
        createAppsChangelogButton.onClick {
            bus.post(CreateAppsChangelogClickedEvent())
        }
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
        val font = releaseProcessLink.font
        val attributes = font.attributes.toMutableMap()
        attributes[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
        releaseProcessLink.font = font.deriveFont(attributes)
        releaseProcessLink.onClick {
            bus.post(OpenReleaseProcessWikiClickedEvent())
        }
        excludedRepos.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        includedRepos.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        setAndroidMessagesVisibility(false)
    }

    fun setReviewerTeams(teamName: String?) {
        reviewerTeam.text = teamName
    }

    fun getReviewerTeams(): String? = reviewerTeam.text

    fun setIndividualReviewers(individuals: String?) {
        individualReviewers.text = individuals
    }

    fun getIndividualReviewers(): String? = individualReviewers.text

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

    fun disableButtons() {
        createAppsChangelogButton.isEnabled = false
        addLibraryButton.isEnabled = false
        removeLibraryButton.isEnabled = false
        releaseLibrariesButton.isEnabled = false
        createVersionBumpsButton.isEnabled = false
    }

    fun enableButtons() {
        createAppsChangelogButton.isEnabled = true
        addLibraryButton.isEnabled = true
        removeLibraryButton.isEnabled = true
        releaseLibrariesButton.isEnabled = true
        createVersionBumpsButton.isEnabled = true
    }

    class CreateAppsChangelogClickedEvent
    class AddLibraryClickedEvent
    class RemoveLibraryClickedEvent
    class ReleaseLibrariesClickedEvent
    class CreateVersionBumpsClickedEvent
    class OpenReleaseProcessWikiClickedEvent
}
