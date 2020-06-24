package com.nerdscorner.android.plugin.github.ui.tablemodels

import com.nerdscorner.android.plugin.github.domain.gh.GHReleaseWrapper
import com.nerdscorner.android.plugin.utils.Strings
import java.io.Serializable
import java.text.SimpleDateFormat

class GHReleaseTableModel(releases: MutableList<GHReleaseWrapper>, colNames: Array<String>)
    : BaseModel<GHReleaseWrapper>(releases, colNames), Serializable {

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        if (rowIndex < 0 || rowIndex >= items.size) {
            return null
        }
        val release = items[rowIndex]
        when (columnIndex) {
            COLUMN_TAG -> return release.ghRelease.name
            COLUMN_DATE -> return SimpleDateFormat(Strings.DATE_FORMAT).format(release.ghRelease.published_at)
        }
        return null
    }

    companion object {
        const val COLUMN_TAG = 0
        const val COLUMN_DATE = 1
    }
}
