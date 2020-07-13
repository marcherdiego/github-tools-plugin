package com.nerdscorner.android.plugin.github.ui.view

import org.greenrobot.eventbus.EventBus
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JLabel

class ExperimentalView(createAppsChangelogButton: JButton, private val changelogProgress: JLabel) {
    lateinit var bus: EventBus

    init {
        createAppsChangelogButton.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                bus.post(CreateAppsChangelogButtonClickedEvent())
            }
        })
        setChangelogProgressVisibility(false)
    }

    fun setChangelogProgressVisibility(visible: Boolean) {
        changelogProgress.isVisible = visible
    }

    fun updateChangelogProgress(message: String) {
        changelogProgress.text = message
    }

    class CreateAppsChangelogButtonClickedEvent
}
