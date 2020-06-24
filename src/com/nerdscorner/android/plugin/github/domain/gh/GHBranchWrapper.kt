package com.nerdscorner.android.plugin.github.domain.gh

import com.intellij.ide.util.PropertiesComponent
import com.nerdscorner.android.plugin.github.ui.windows.ResultDialog
import com.nerdscorner.android.plugin.github.ui.windows.SimpleInputDialog
import com.nerdscorner.android.plugin.github.ui.windows.SimpleInputDialog.Callback
import com.nerdscorner.android.plugin.utils.CircleCiUtils
import com.nerdscorner.android.plugin.utils.GithubUtils
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.TravisCiUtils
import org.kohsuke.github.GHBranch

class GHBranchWrapper(val ghBranch: GHBranch) : Wrapper() {

    val url: String
        get() = ghBranch
                .owner
                .getCommit(ghBranch.shA1)
                .htmlUrl
                .toString()

    var buildStatus: String? = null
    var buildStatusUrl: String? = null
    private var travisBuild: Boolean = false
    private var circleBuild: Boolean = false
    private var externalBuildId: String = Strings.BLANK

    init {
        ghBranch
                .owner
                ?.getCheckRuns(ghBranch.shA1)
                ?.forEach {
                    travisBuild = it
                            .detailsUrl
                            .toString()
                            .contains(TRAVIS_CI)
                    circleBuild = it
                            .detailsUrl
                            .toString()
                            .contains(CIRCLE_CI)
                    if (travisBuild || circleBuild) {
                        buildStatus = it.status
                        buildStatusUrl = it.detailsUrl.toString()
                        externalBuildId = it.externalId
                        return@forEach
                    }
                }
    }

    override fun toString(): String {
        return ghBranch.name
    }

    override fun compare(other: Wrapper): Int {
        if (other is GHBranchWrapper) {
            try {

                return other.ghBranch.name.compareTo(ghBranch.name)
            } catch (ignored: Exception) {
            }
        }
        return 0
    }

    fun triggerBuild() {
        if (travisBuild) {
            triggerTravisBuild()
        } else if (circleBuild) {
            triggerCircleBuild()
        }
    }

    private fun triggerTravisBuild() {
        val travisCiToken = propertiesComponent.getValue(Strings.TRAVIS_CI_TOKEN_PROPERTY, Strings.BLANK)
        if (travisCiToken.isEmpty()) {
            showSimpleInputDialog(Strings.ENTER_TRAVIS_CI_TOKEN, Callback { input ->
                if (input.isNullOrEmpty().not()) {
                    propertiesComponent.setValue(Strings.TRAVIS_CI_TOKEN_PROPERTY, input)
                    triggerTravisBuild()
                }
            })
        } else {
            TravisCiUtils.reRunJob(
                    externalId = externalBuildId,
                    success = {
                        showResultDialog(BUILD_TRIGGERED, true)
                    },
                    fail = { message ->
                        showResultDialog("$BUILD_TRIGGER_FAILED $message", false) {
                            propertiesComponent.setValue(Strings.TRAVIS_CI_TOKEN_PROPERTY, Strings.BLANK)
                        }
                    }
            )
        }
    }

    private fun triggerCircleBuild() {
        val circleCiToken = propertiesComponent.getValue(Strings.CIRCLE_CI_TOKEN_PROPERTY, Strings.BLANK)
        if (circleCiToken.isEmpty()) {
            showSimpleInputDialog(Strings.ENTER_CIRCLE_CI_TOKEN, Callback { input ->
                if (input.isNullOrEmpty().not()) {
                    propertiesComponent.setValue(Strings.CIRCLE_CI_TOKEN_PROPERTY, input)
                    triggerCircleBuild()
                }
            })
        } else {
            CircleCiUtils.reRunWorkflow(
                    externalId = externalBuildId,
                    success = {
                        showResultDialog(BUILD_TRIGGERED, true)
                    },
                    fail = { message ->
                        showResultDialog("$BUILD_TRIGGER_FAILED $message", false) {
                            propertiesComponent.setValue(Strings.CIRCLE_CI_TOKEN_PROPERTY, Strings.BLANK)
                        }
                    }
            )
        }
    }

    private fun showSimpleInputDialog(title: String, callback: Callback) {
        val resultDialog = SimpleInputDialog(callback)
        resultDialog.pack()
        resultDialog.setLocationRelativeTo(null)
        resultDialog.title = title
        resultDialog.isResizable = false
        resultDialog.isVisible = true
    }

    private fun showResultDialog(message: String, success: Boolean, clear: () -> Unit = {}) {
        val resultDialog = if (success) {
            ResultDialog(
                    message,
                    VIEW_BUILD,
                    ResultDialog.SuccessCallback {
                        openBuildInBrowser()
                    }
            )
        } else {
            ResultDialog(
                    message,
                    VIEW_BUILD,
                    ResultDialog.SuccessCallback {
                        openBuildInBrowser()
                    },
                    CLEAR_TOKEN,
                    ResultDialog.ClearCallback {
                        clear()
                    }
            )
        }
        resultDialog.pack()
        resultDialog.setLocationRelativeTo(null)
        resultDialog.title = Strings.REBUILD_ERROR
        resultDialog.isResizable = false
        resultDialog.isVisible = true
    }

    fun openBuildInBrowser() {
        GithubUtils.openWebLink(buildStatusUrl)
    }

    companion object {
        private val propertiesComponent = PropertiesComponent.getInstance()

        private const val TRAVIS_CI = "travis-ci"
        private const val CIRCLE_CI = "circleci"

        private const val VIEW_BUILD = "View build"
        private const val CLEAR_TOKEN = "Clear token"
        private const val BUILD_TRIGGERED = "Build triggered!"
        private const val BUILD_TRIGGER_FAILED = "Build trigger failed with message:"
    }
}
