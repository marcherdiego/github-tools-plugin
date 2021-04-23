package com.nerdscorner.android.plugin.github.ui.view

import org.greenrobot.eventbus.EventBus
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JTabbedPane
import javax.swing.JTextField

class ParametersView(private val githubToken: JTextField,
                     private val circleCiToken: JTextField,
                     private val travisToken: JTextField,
                     private val organizationName: JTextField,
                     private val showOrganizationReposOnly: JCheckBox,
                     private val showDeploymentPanel: JCheckBox,
                     private val tabbedPane: JTabbedPane,
                     private val deploymentPanel: Component,
                     saveButton: JButton,
                     private val parametersMessageLabel: JLabel) {

    lateinit var bus: EventBus

    init {
        saveButton.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                bus.post(
                        SaveButtonClickedEvent(
                                githubToken.text,
                                circleCiToken.text,
                                travisToken.text,
                                organizationName.text,
                                showOrganizationReposOnly.isSelected,
                                showDeploymentPanel.isSelected
                        )
                )
            }
        })
    }

    fun setParameters(organizationName: String?, githubToken: String?, circleCiToken: String?, travisCiToken: String?,
                      showOrganizationReposOnly: Boolean, showDeploymentPanel: Boolean) {
        this.organizationName.text = organizationName
        this.githubToken.text = githubToken
        this.circleCiToken.text = circleCiToken
        this.travisToken.text = travisCiToken
        this.showOrganizationReposOnly.isSelected = showOrganizationReposOnly
        this.showDeploymentPanel.isSelected = showDeploymentPanel
    }

    fun updateMessageLabel(message: String) {
        parametersMessageLabel.text = message
    }

    fun showMessageLabel() {
        parametersMessageLabel.isVisible = true
    }

    fun hideMessageLabel() {
        parametersMessageLabel.isVisible = false
    }

    fun showDeploymentPanel() {
        tabbedPane.addTab("Deployment", deploymentPanel)
    }

    fun hideDeploymentPanel() {
        tabbedPane.removeTabAt(DEPLOYMENT_TAB_INDEX)
    }

    class SaveButtonClickedEvent(
            val githubToken: String?,
            val circleCiToken: String?,
            val travisToken: String?,
            val organizationName: String?,
            val showOrganizationReposOnly: Boolean,
            val showDeploymentPanel: Boolean
    )

    companion object {
        private const val DEPLOYMENT_TAB_INDEX = 3
    }
}