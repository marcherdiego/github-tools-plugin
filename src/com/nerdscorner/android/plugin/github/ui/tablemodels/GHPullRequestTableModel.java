package com.nerdscorner.android.plugin.github.ui.tablemodels;

import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.List;

import com.nerdscorner.android.plugin.github.domain.gh.GHPullRequestWrapper;
import com.nerdscorner.android.plugin.utils.Strings;

public class GHPullRequestTableModel extends BaseModel<GHPullRequestWrapper> implements Serializable {
    private static final int COLUMN_TITLE = 0;
    private static final int COLUMN_AUTHOR = 1;
    private static final int COLUMN_DATE = 2;

    public GHPullRequestTableModel(List<GHPullRequestWrapper> pullRequests, String[] colNames) {
        super(pullRequests, colNames);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= items.size()) {
            return null;
        }
        GHPullRequestWrapper pullRequestWrapper = items.get(rowIndex);
        GHPullRequest pullRequest = pullRequestWrapper.getGhPullRequest();
        switch (columnIndex) {
            case COLUMN_TITLE:
                return pullRequest.getTitle();
            case COLUMN_AUTHOR:
                try {
                    return pullRequest.getUser().getLogin();
                } catch (Exception e) {
                    return null;
                }
            case COLUMN_DATE:
                try {
                    return new SimpleDateFormat(Strings.DATE_FORMAT).format(pullRequest.getUpdatedAt());
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
        return null;
    }
}
