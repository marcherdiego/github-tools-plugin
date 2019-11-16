package com.tal.android.plugin.github.ui.tablemodels;

import java.io.Serializable;
import java.util.List;

import com.tal.android.plugin.github.domain.gh.GHRepositoryWrapper;

public class GHRepoTableModel extends BaseModel<GHRepositoryWrapper> implements Serializable {

    public GHRepoTableModel(List<GHRepositoryWrapper> repositories, String[] colNames) {
        super(repositories, colNames);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return GHRepositoryWrapper.class;
    }
}
