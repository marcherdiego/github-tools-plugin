package com.tal.android.plugin.github.ui.tablemodels;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.tal.android.plugin.github.domain.gh.GHReleaseWrapper;
import com.tal.android.plugin.utils.ListUtils;
import com.tal.android.plugin.utils.Strings;

public class GHReleaseTableModel extends AbstractTableModel implements Serializable {
    private static final int COLUMN_TAG = 0;
    private static final int COLUMN_DATE = 1;

    private List<GHReleaseWrapper> releases;
    private int colsCount;
    private String[] colNames;

    public GHReleaseTableModel(List<GHReleaseWrapper> releases, String[] colNames) {
        this.releases = releases;
        this.colsCount = colNames.length;
        this.colNames = colNames;
    }

    public void addRow(GHReleaseWrapper item) {
        releases.add(item);
    }

    public void removeRow(int row) {
        releases.remove(row);
        fireTableRowsDeleted(row, row);
    }

    public void removeAllRows() {
        releases.clear();
        fireTableDataChanged();
    }

    @Override
    public String getColumnName(int column) {
        return colNames[column];
    }

    @Override
    public int getRowCount() {
        return releases.size();
    }

    @Override
    public int getColumnCount() {
        return colsCount;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public GHReleaseWrapper getRow(int rowIndex) {
        if (ListUtils.isEmpty(releases)) {
            return null;
        }
        return releases.get(rowIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= releases.size()) {
            return null;
        }
        GHReleaseWrapper release = releases.get(rowIndex);
        switch (columnIndex) {
            case COLUMN_TAG:
                return release.getGhRelease().getName();
            case COLUMN_DATE:
                try {
                    return new SimpleDateFormat(Strings.DATE_FORMAT).format(release.getGhRelease().getCreatedAt());
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return null;
    }
}
