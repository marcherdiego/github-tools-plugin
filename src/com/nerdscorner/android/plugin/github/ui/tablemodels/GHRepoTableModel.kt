package com.nerdscorner.android.plugin.github.ui.tablemodels

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import java.io.Serializable

class GHRepoTableModel(repositories: MutableList<GHRepositoryWrapper>, colNames: Array<String>) :
        BaseModel<GHRepositoryWrapper>(repositories, colNames), Serializable {

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return GHRepositoryWrapper::class.java
    }

    override fun compare(one: GHRepositoryWrapper, other: GHRepositoryWrapper) = super.compare(other, one)
}
