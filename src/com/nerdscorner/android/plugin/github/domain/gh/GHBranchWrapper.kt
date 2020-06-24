package com.nerdscorner.android.plugin.github.domain.gh

import org.kohsuke.github.GHBranch

class GHBranchWrapper(val ghBranch: GHBranch) : Wrapper() {

    init {
        ghBranch
                .owner
                ?.getCheckRuns(ghBranch.shA1)
                ?.forEach {
                    if (it.detailsUrl.toString().contains("travis-ci") || it.detailsUrl.toString().contains("circleci")) {
                        buildStatus = it.status
                        buildStatusUrl = it.detailsUrl.toString()
                        return@forEach
                    }
                }
    }

    val url: String
        get() = ghBranch
                .owner
                .getCommit(ghBranch.shA1)
                .htmlUrl
                .toString()

    var buildStatus: String? = null
    var buildStatusUrl: String? = null

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
}
