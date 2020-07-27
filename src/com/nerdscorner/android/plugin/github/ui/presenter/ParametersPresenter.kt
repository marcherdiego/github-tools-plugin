package com.nerdscorner.android.plugin.github.ui.presenter

import com.nerdscorner.android.plugin.github.events.ParameterUpdatedEvent
import com.nerdscorner.android.plugin.github.ui.model.ParametersModel
import com.nerdscorner.android.plugin.github.ui.view.ParametersView
import com.nerdscorner.android.plugin.github.ui.view.ParametersView.SaveButtonClickedEvent
import com.nerdscorner.android.plugin.utils.ThreadUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class ParametersPresenter(private val view: ParametersView, private val model: ParametersModel, bus: EventBus) {
    init {
        view.bus = bus
        model.bus = bus
        bus.register(this)
        EventBus.getDefault().register(this)
        view.hideMessageLabel()
        view.setParameters(
                model.getOrganizationName(),
                model.getGithubToken(),
                model.getCircleCiToken(),
                model.getTravisCiToken()
        )
    }

    @Subscribe
    fun onSaveButtonClicked(event: SaveButtonClickedEvent) {
        model.saveParameters(event.organizationName, event.githubToken, event.circleCiToken, event.travisToken)
        view.updateMessageLabel(SAVED_MESSAGE)
        view.showMessageLabel()
        ThreadUtils.executeDelayed(SAVED_MESSAGE_DELAY) {
            view.hideMessageLabel()
        }
    }

    @Subscribe
    fun onParameterUpdated(event: ParameterUpdatedEvent) {
        view.setParameters(
                model.getOrganizationName(),
                model.getGithubToken(),
                model.getCircleCiToken(),
                model.getTravisCiToken()
        )
    }

    companion object {
        private const val SAVED_MESSAGE = "Saved!"
        private const val SAVED_MESSAGE_DELAY = 1000L
    }
}