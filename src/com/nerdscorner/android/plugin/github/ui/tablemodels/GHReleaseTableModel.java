package com.nerdscorner.android.plugin.github.ui.tablemodels;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.List;

import com.nerdscorner.android.plugin.github.domain.gh.GHReleaseWrapper;
import com.nerdscorner.android.plugin.utils.Strings;

public class GHReleaseTableModel extends BaseModel<GHReleaseWrapper> implements Serializable {
    private static final int COLUMN_TAG = 0;
    private static final int COLUMN_DATE = 1;

    public GHReleaseTableModel(List<GHReleaseWrapper> releases, String[] colNames) {
        super(releases, colNames);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= items.size()) {
            return null;
        }
        GHReleaseWrapper release = items.get(rowIndex);
        switch (columnIndex) {
            case COLUMN_TAG:
                return release.getGhRelease().getName();
            case COLUMN_DATE:
                return new SimpleDateFormat(Strings.DATE_FORMAT).format(release.getGhRelease().getPublished_at());
        }
        return null;
    }
}
