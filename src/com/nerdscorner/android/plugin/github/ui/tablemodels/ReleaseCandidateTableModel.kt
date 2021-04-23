package com.nerdscorner.android.plugin.github.ui.tablemodels

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.utils.Strings
import java.io.Serializable

class ReleaseCandidateTableModel(
        allRepositories: MutableList<GHRepositoryWrapper>,
        private val releasedRepositories: MutableList<GHRepositoryWrapper>,
        colNames: Array<String>
) : BaseModel<GHRepositoryWrapper>(allRepositories, colNames), Serializable {

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        if (rowIndex < 0 || rowIndex >= items.size) {
            return null
        }
        val repository = items[rowIndex]
        val released = releasedRepositories.contains(repository)
        return when (columnIndex) {
            COLUMN_NAME -> repository.name
            COLUMN_VERSION -> "rc-${repository.version}"
            COLUMN_STATUS -> if (released) {
                RELEASED
            } else {
                "$NOT_RELEASED (view details)"
            }
            COLUMN_PULL_REQUEST -> if (released) {
                VIEW_PR
            } else {
                Strings.BLANK
            }
            else -> null
        }
    }

    companion object {
        const val COLUMN_NAME = 0
        const val COLUMN_VERSION = 1
        const val COLUMN_STATUS = 2
        const val COLUMN_PULL_REQUEST = 3

        private const val VIEW_PR = "View PR"
        private const val RELEASED = "Released!"
        private const val NOT_RELEASED = "Not Released"
    }
}
