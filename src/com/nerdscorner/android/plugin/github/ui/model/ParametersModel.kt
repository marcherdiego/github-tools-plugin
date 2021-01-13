package com.nerdscorner.android.plugin.github.ui.model

import com.intellij.ide.util.PropertiesComponent
import com.nerdscorner.android.plugin.utils.Strings
import org.greenrobot.eventbus.EventBus

class ParametersModel {
    lateinit var bus: EventBus
    private val propertiesComponent = PropertiesComponent.getInstance()

    fun getOrganizationName() = propertiesComponent.getValue(Strings.ORGANIZATION_NAMES_PROPERTY)

    fun getGithubToken() = propertiesComponent.getValue(Strings.OAUTH_TOKEN_PROPERTY)

    fun getCircleCiToken() = propertiesComponent.getValue(Strings.CIRCLE_CI_TOKEN_PROPERTY)

    fun getTravisCiToken() = propertiesComponent.getValue(Strings.TRAVIS_CI_TOKEN_PROPERTY)

    fun getShowReposFromOutsideOrganization() = propertiesComponent.isTrueValue(Strings.SHOW_REPOS_FROM_OUTSIDE_ORGANIZATION)

    fun saveParameters(organizationName: String?, githubToken: String?, circleCiToken: String?, travisToken: String?,
                       showReposOutsideOrganization: Boolean) {
        propertiesComponent.setValue(Strings.ORGANIZATION_NAMES_PROPERTY, organizationName)
        propertiesComponent.setValue(Strings.OAUTH_TOKEN_PROPERTY, githubToken)
        propertiesComponent.setValue(Strings.CIRCLE_CI_TOKEN_PROPERTY, circleCiToken)
        propertiesComponent.setValue(Strings.TRAVIS_CI_TOKEN_PROPERTY, travisToken)
        propertiesComponent.setValue(Strings.SHOW_REPOS_FROM_OUTSIDE_ORGANIZATION, showReposOutsideOrganization)
    }
}