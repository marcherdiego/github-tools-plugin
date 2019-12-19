package com.nerdscorner.android.plugin.github.ui.controllers;


import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHOrganization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper;
import com.nerdscorner.android.plugin.github.events.FavoriteRepositoryUpdatedEvent;
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel;
import com.nerdscorner.android.plugin.github.ui.tables.ColumnRenderer;
import com.nerdscorner.android.plugin.utils.JTableUtils;
import com.nerdscorner.android.plugin.utils.Strings;
import com.nerdscorner.android.plugin.utils.ThreadUtils;

public class MyReposController extends BaseRepoListController {

    private GHMyself myselfGitHub;

    public MyReposController(JTable reposTable, JTable repoReleases, JTable repoPullRequestsTable, JTable repoClosedPullRequestsTable,
                             JLabel repoComments, GHMyself myselfGitHub, GHOrganization ghOrganization) {
        super(reposTable, repoReleases, repoPullRequestsTable, repoClosedPullRequestsTable, repoComments, ghOrganization, GHRepoTableModel.COLUMN_NAME);
        this.myselfGitHub = myselfGitHub;
    }

    @Override
    public void loadRepositories() {
        ThreadUtils.cancelThread(loaderThread);
        loaderThread = new Thread(() -> {
            final GHRepoTableModel myReposTableModel = new GHRepoTableModel(new ArrayList<>(), new String[]{Strings.NAME});
            reposTable.setModel(myReposTableModel);
            TableColumn column = reposTable.getColumn(Strings.NAME);
            final Map<String, String> tooltips = new HashMap<>();
            column.setCellRenderer(new ColumnRenderer(tooltips));
            myselfGitHub
                    .listSubscriptions()
                    .withPageSize(LARGE_PAGE_SIZE)
                    .forEach(repository -> {
                        if (repository.isFork() || !repository.getFullName().startsWith(getOrganizationName())) {
                            //Ignore forks or projects that doesn't belong to this organization
                            return;
                        }
                        myReposTableModel.addRow(new GHRepositoryWrapper(repository));
                    });
            JTableUtils.findAndSelectDefaultRepo(selectedRepo, reposTable);
            SwingUtilities.invokeLater(this::updateRepositoryInfoTables);
        });
        loaderThread.start();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFavoriteRepositoryUpdated(FavoriteRepositoryUpdatedEvent event) {
        ((AbstractTableModel) reposTable.getModel()).fireTableDataChanged();
    }
}
