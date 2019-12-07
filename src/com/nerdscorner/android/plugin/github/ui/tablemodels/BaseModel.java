package com.nerdscorner.android.plugin.github.ui.tablemodels;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.nerdscorner.android.plugin.github.domain.gh.Wrapper;
import com.nerdscorner.android.plugin.utils.ListUtils;

public abstract class BaseModel<T extends Wrapper> extends AbstractTableModel implements Serializable, Comparator<T> {
    public static final int COLUMN_NAME = 0;

    private final int colsCount;
    private final String[] colNames;

    final List<T> items;

    BaseModel(List<T> items, String[] colNames) {
        this.items = items;
        this.colNames = colNames;
        this.colsCount = colNames.length;
    }

    public void addRow(T item) {
        int row = items.size();
        items.add(item);
        items.sort(this);
        fireTableRowsInserted(row, row);
    }

    public void removeAllRows() {
        items.clear();
        fireTableDataChanged();
    }

    @Override
    public String getColumnName(int column) {
        return colNames[column];
    }

    @Override
    public int getRowCount() {
        return items.size();
    }

    @Override
    public int getColumnCount() {
        return colsCount;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    public T getRow(int rowIndex) {
        if (ListUtils.isEmpty(items)) {
            return null;
        }
        return items.get(rowIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= items.size()) {
            return null;
        }
        return items.get(rowIndex);
    }

    @Override
    public int compare(T one, T other) {
        return one.compare(other);
    }
}
