package com.tal.android.plugin.github.ui.tablemodels;


import java.io.Serializable;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.tal.android.plugin.github.domain.gh.GHRepositoryWrapper;

public class GHRepoTableModel extends AbstractTableModel implements Serializable {
    public static final int COLUMN_NAME = 0;
    protected List<GHRepositoryWrapper> repositories;
    private int colsCount;
    private String[] colNames;

    public GHRepoTableModel(List<GHRepositoryWrapper> repositories, String[] colNames) {
        this.repositories = repositories;
        this.colsCount = colNames.length;
        this.colNames = colNames;
    }

    public void addRow(GHRepositoryWrapper item) {
        int row = repositories.size();
        repositories.add(item);
        fireTableRowsInserted(row, row);
    }

    public void removeRow(int row) {
        repositories.remove(row);
        fireTableRowsDeleted(row, row);
    }

    public void removeAllRows() {
        repositories.clear();
        fireTableDataChanged();
    }

    @Override
    public String getColumnName(int column) {
        return colNames[column];
    }

    @Override
    public int getRowCount() {
        return repositories.size();
    }

    @Override
    public int getColumnCount() {
        return colsCount;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return GHRepositoryWrapper.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= repositories.size()) {
            return null;
        }
        return repositories.get(rowIndex);
    }

    public void removeRepository(GHRepositoryWrapper repository) {
        repositories.remove(repository);
    }
}
