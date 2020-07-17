package com.nerdscorner.android.plugin.github.ui.tablemodels

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import java.io.Serializable

class ReleaseCandidateTableModel(repositories: MutableList<GHRepositoryWrapper>, colNames: Array<String>)
    : BaseModel<GHRepositoryWrapper>(repositories, colNames), Serializable {

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        if (rowIndex < 0 || rowIndex >= items.size) {
            return null
        }
        val repository = items[rowIndex]
        when (columnIndex) {
            COLUMN_NAME -> return repository.name
            COLUMN_VERSION -> return "rc-${repository.version}"
            COLUMN_PULL_REQUEST -> return VIEW_PR
        }
        return null
    }

    companion object {
        const val COLUMN_NAME = 0
        const val COLUMN_VERSION = 1
        const val COLUMN_PULL_REQUEST = 2

        private const val VIEW_PR = "View PR"
    }
}
