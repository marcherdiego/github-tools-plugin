package com.nerdscorner.android.plugin.github.ui.tablemodels

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.utils.Strings
import java.io.Serializable

class VersionBumpsTableModel(
        allRepositories: MutableList<GHRepositoryWrapper>,
        private val bumpedRepositories: MutableList<GHRepositoryWrapper>,
        colNames: Array<String>
) : BaseModel<GHRepositoryWrapper>(allRepositories, colNames), Serializable {

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        if (rowIndex < 0 || rowIndex >= items.size) {
            return null
        }
        val repository = items[rowIndex]
        val bumped = bumpedRepositories.contains(repository)
        return when (columnIndex) {
            COLUMN_NAME -> repository.name
            COLUMN_PREVIOUS_VERSION -> repository.version
            COLUMN_NEXT_VERSION -> repository.nextVersion
            COLUMN_STATUS -> if (bumped) {
                BUMPED
            } else {
                "$NOT_BUMPED (view details)"
            }
            COLUMN_PULL_REQUEST -> if (bumped) {
                VIEW_PR
            } else {
                Strings.BLANK
            }
            else -> null
        }
    }

    companion object {
        const val COLUMN_NAME = 0
        const val COLUMN_PREVIOUS_VERSION = 1
        const val COLUMN_NEXT_VERSION = 2
        const val COLUMN_STATUS = 3
        const val COLUMN_PULL_REQUEST = 4

        private const val VIEW_PR = "View PR"
        private const val BUMPED = "Bumped!"
        private const val NOT_BUMPED = "Not Bumped"
    }
}
