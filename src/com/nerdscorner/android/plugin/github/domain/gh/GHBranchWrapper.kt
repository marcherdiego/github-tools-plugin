package com.nerdscorner.android.plugin.github.domain.gh

import com.nerdscorner.android.plugin.utils.Strings
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
    var travisBuild: Boolean = false
    var circleBuild: Boolean = false
    var externalBuildId: String = Strings.BLANK

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
                        buildStatus = it.status.capitalize()
                        it.conclusion?.let { conclusion ->
                            buildStatus += ": ${conclusion.capitalize()}"
                        }
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

    companion object {

        private const val TRAVIS_CI = "travis-ci"
        private const val CIRCLE_CI = "circleci"
    }
}
