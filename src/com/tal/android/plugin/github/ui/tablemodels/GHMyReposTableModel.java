package com.tal.android.plugin.github.ui.tablemodels;

import java.io.Serializable;
import java.util.List;

import com.tal.android.plugin.github.domain.gh.GHRepositoryWrapper;
import com.tal.android.plugin.utils.RepositoryUtils;

public class GHMyReposTableModel extends GHRepoTableModel implements Serializable {
    public static final int COLUMN_FAV = 0;
    public static final int COLUMN_NAME = 1;

    private final RepositoryUtils repositoryUtils;

    public GHMyReposTableModel(List<GHRepositoryWrapper> repositories, String[] colNames) {
        super(repositories, colNames);
        repositoryUtils = RepositoryUtils.getInstance();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case COLUMN_FAV:
                return Boolean.class;
            case COLUMN_NAME:
                return GHRepositoryWrapper.class;
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        switch (column) {
            case COLUMN_FAV:
                return true;
        }
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= repositories.size()) {
            return null;
        }
        GHRepositoryWrapper repository = repositories.get(rowIndex);
        switch (columnIndex) {
            case COLUMN_FAV:
                return repositoryUtils.isFavorite(repository);
            case COLUMN_NAME:
                return repository;
        }
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        super.setValueAt(aValue, rowIndex, columnIndex);
        if (aValue instanceof Boolean && columnIndex == COLUMN_FAV) {
            GHRepositoryWrapper repository = repositories.get(rowIndex);
            repositoryUtils.toggleFavorite(repository);
        }
    }
}
