package com.nerdscorner.android.plugin.github.ui.view

import org.greenrobot.eventbus.EventBus
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton

class ExperimentalView(createAppsChangelogButton: JButton) {
    lateinit var bus: EventBus

    init {
        createAppsChangelogButton.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                bus.post(CreateAppsChangelogButtonClickedEvent())
            }
        })
    }

    class CreateAppsChangelogButtonClickedEvent
}
