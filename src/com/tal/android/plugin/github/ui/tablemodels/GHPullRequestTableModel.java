package com.tal.android.plugin.github.ui.tablemodels;

import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.tal.android.plugin.github.domain.gh.GHPullRequestWrapper;
import com.tal.android.plugin.utils.Strings;

public class GHPullRequestTableModel extends AbstractTableModel implements Serializable {
    private static final int COLUMN_TITLE = 0;
    private static final int COLUMN_AUTHOR = 1;
    private static final int COLUMN_DATE = 2;

    private List<GHPullRequestWrapper> pullRequests;
    private int colsCount;
    private String[] colNames;

    public GHPullRequestTableModel(List<GHPullRequestWrapper> pullRequests, String[] colNames) {
        this.pullRequests = pullRequests;
        this.colsCount = colNames.length;
        this.colNames = colNames;
    }

    public void addRow(GHPullRequestWrapper item) {
        pullRequests.add(item);
    }

    public void removeRow(int row) {
        pullRequests.remove(row);
        fireTableRowsDeleted(row, row);
    }

    public void removeAllRows() {
        pullRequests.clear();
        fireTableDataChanged();
    }

    @Override
    public String getColumnName(int column) {
        return colNames[column];
    }

    @Override
    public int getRowCount() {
        return pullRequests.size();
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

    public GHPullRequestWrapper getRow(int rowIndex) {
        return pullRequests.get(rowIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= pullRequests.size()) {
            return null;
        }
        GHPullRequestWrapper pullRequestWrapper = pullRequests.get(rowIndex);
        GHPullRequest pullRequest = pullRequestWrapper.getGhPullRequest();
        switch (columnIndex) {
            case COLUMN_TITLE:
                return pullRequest.getTitle();
            case COLUMN_AUTHOR:
                try {
                    return pullRequest.getUser().getLogin();
                } catch (IOException e) {
                    return null;
                }
            case COLUMN_DATE:
                try {
                    return new SimpleDateFormat(Strings.DATE_FORMAT).format(pullRequest.getCreatedAt());
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return null;
    }
}
