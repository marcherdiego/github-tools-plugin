package com.nerdscorner.android.plugin.github.ui.tablemodels

import com.nerdscorner.android.plugin.github.domain.gh.GHReleaseWrapper
import java.io.Serializable

class GHReleaseTableModel(releases: MutableList<GHReleaseWrapper>, colNames: Array<String>)
    : BaseModel<GHReleaseWrapper>(releases, colNames), Serializable {

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        if (rowIndex < 0 || rowIndex >= items.size) {
            return null
        }
        val release = items[rowIndex]
        return when (columnIndex) {
            COLUMN_TAG -> release.ghRelease.name
            COLUMN_DATE -> release.publishedAt
            else -> null
        }
    }

    companion object {
        const val COLUMN_TAG = 0
        const val COLUMN_DATE = 1
    }
}
