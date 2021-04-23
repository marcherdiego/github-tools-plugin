package com.nerdscorner.android.plugin.github.domain.gh

import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.startThread
import org.kohsuke.github.GHBranch
import org.kohsuke.github.GHCheckRun
import javax.swing.table.AbstractTableModel

class GHBranchWrapper(private val tableModel: AbstractTableModel, val ghBranch: GHBranch) : Wrapper() {

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
        startThread {
            ghBranch
                    .owner
                    ?.getCheckRuns(ghBranch.shA1)
                    ?.forEach {
                        if (it.detailsUrl.toString().contains(TRAVIS_CI)) {
                            travisBuild = true
                            extractBranchStatus(it)
                        } else if (it.detailsUrl.toString().contains(CIRCLE_CI)) {
                            circleBuild = true
                            extractBranchStatus(it)
                        }
                    }
        }
    }

    private fun extractBranchStatus(ghCheckRun: GHCheckRun) {
        buildStatus = ghCheckRun.status.capitalize()
        ghCheckRun.conclusion?.let { conclusion ->
            buildStatus += ": ${conclusion.capitalize()}"
        }
        buildStatusUrl = ghCheckRun.detailsUrl.toString()
        externalBuildId = ghCheckRun.externalId
        tableModel.fireTableDataChanged()
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
