package com.nerdscorner.android.plugin.github.ui.view

import org.greenrobot.eventbus.EventBus
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JTextField

class ParametersView(private val githubToken: JTextField, private val circleCiToken: JTextField, private val travisToken: JTextField,
                     private val organizationName: JTextField, saveButton: JButton) {

    lateinit var bus: EventBus

    init {
        saveButton.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                bus.post(SaveButtonClickedEvent(
                        githubToken.text,
                        circleCiToken.text,
                        travisToken.text,
                        organizationName.text
                ))
            }
        })
    }

    fun setParameters(organizationName: String?, githubToken: String?, circleCiToken: String?, travisCiToken: String?) {
        this.organizationName.text = organizationName
        this.githubToken.text = githubToken
        this.circleCiToken.text = circleCiToken
        this.travisToken.text = travisCiToken
    }

    class SaveButtonClickedEvent(
            val githubToken: String?,
            val circleCiToken: String?,
            val travisToken: String?,
            val organizationName: String?
    )
}