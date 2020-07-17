package com.nerdscorner.android.plugin.github.ui.tablemodels

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import java.io.Serializable

class VersionBumpsTableModel(repositories: MutableList<GHRepositoryWrapper>, colNames: Array<String>)
    : BaseModel<GHRepositoryWrapper>(repositories, colNames), Serializable {

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        if (rowIndex < 0 || rowIndex >= items.size) {
            return null
        }
        val repository = items[rowIndex]
        return when (columnIndex) {
            COLUMN_NAME -> repository.name
            COLUMN_PREVIOUS_VERSION -> repository.version
            COLUMN_NEXT_VERSION -> repository.nextVersion
            COLUMN_PULL_REQUEST -> VIEW_PR
            else -> null
        }
    }

    companion object {
        const val COLUMN_NAME = 0
        const val COLUMN_PREVIOUS_VERSION = 1
        const val COLUMN_NEXT_VERSION = 2
        const val COLUMN_PULL_REQUEST = 3

        private const val VIEW_PR = "View PR"
    }
}
