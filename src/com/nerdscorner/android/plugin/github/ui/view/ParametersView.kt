package com.nerdscorner.android.plugin.github.ui.view

import org.greenrobot.eventbus.EventBus
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JTextField

class ParametersView(private val githubToken: JTextField,
                     private val circleCiToken: JTextField,
                     private val travisToken: JTextField,
                     private val organizationName: JTextField,
                     private val showReposOutsideOrganization: JCheckBox,
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
                                showReposOutsideOrganization.isSelected
                        )
                )
            }
        })
    }

    fun setParameters(organizationName: String?, githubToken: String?, circleCiToken: String?, travisCiToken: String?,
                      showReposOutsideOrganization: Boolean) {
        this.organizationName.text = organizationName
        this.githubToken.text = githubToken
        this.circleCiToken.text = circleCiToken
        this.travisToken.text = travisCiToken
        this.showReposOutsideOrganization.isSelected = showReposOutsideOrganization
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

    class SaveButtonClickedEvent(
            val githubToken: String?,
            val circleCiToken: String?,
            val travisToken: String?,
            val organizationName: String?,
            val showReposOutsideOrganization: Boolean
    )
}