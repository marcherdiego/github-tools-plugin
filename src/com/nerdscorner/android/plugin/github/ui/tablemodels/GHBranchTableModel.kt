package com.nerdscorner.android.plugin.github.ui.tablemodels

import com.nerdscorner.android.plugin.github.domain.gh.GHBranchWrapper
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import java.io.Serializable

class GHBranchTableModel(branches: MutableList<GHBranchWrapper>, colNames: Array<String>)
    : BaseModel<GHBranchWrapper>(branches, colNames), Serializable {

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        if (rowIndex < 0 || rowIndex >= items.size) {
            return null
        }
        val branch = items[rowIndex]
        return when (columnIndex) {
            COLUMN_NAME -> branch.ghBranch.name
            COLUMN_STATUS -> branch.buildStatus
            COLUMN_TRIGGER_BUILD -> TRIGGER_BUILD
            else -> null
        }
    }

    override fun compare(one: GHBranchWrapper, other: GHBranchWrapper): Int {
        return super.compare(other, one)
    }

    companion object {
        const val COLUMN_NAME = 0
        const val COLUMN_STATUS = 1
        const val COLUMN_TRIGGER_BUILD = 2

        private const val TRIGGER_BUILD = "Re-run"
    }
}
