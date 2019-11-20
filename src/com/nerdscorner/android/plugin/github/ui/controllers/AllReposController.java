package com.nerdscorner.android.plugin.github.ui.controllers;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.kohsuke.github.GHOrganization;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.nerdscorner.android.plugin.github.domain.gh.GHPullRequestWrapper;
import com.nerdscorner.android.plugin.github.domain.gh.GHReleaseWrapper;
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper;
import com.nerdscorner.android.plugin.github.events.FavoriteRepositoryUpdatedEvent;
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHPullRequestTableModel;
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHReleaseTableModel;
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel;
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleMouseAdapter;
import com.nerdscorner.android.plugin.utils.Strings;

public class AllReposController extends BaseRepoListController {

    public AllReposController(JTable reposTable, JTable repoReleases, JTable repoPullRequestsTable, JLabel repoComments, GHOrganization ghOrganization) {
        super(reposTable, repoReleases, repoPullRequestsTable, repoComments, ghOrganization);
    }

    @Override
    protected void startListeningBus() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void init() {
        startListeningBus();
        reposTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        repoReleasesTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        repoPullRequestsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        reposTable.addMouseListener(new SimpleMouseAdapter() {
            public void mousePressed(int row, int column, int clickCount) {
                GHRepositoryWrapper repository = (GHRepositoryWrapper) reposTable.getValueAt(row, GHRepoTableModel.COLUMN_NAME);
                if (clickCount == 1) {
                    try {
                        updateRepositoryInfo(repository);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else if (clickCount == 2) {
                    openWebLink(repository.getFullUrl());
                }
            }
        });
        repoReleasesTable.addMouseListener(new SimpleMouseAdapter() {
            public void mousePressed(int row, int column, int clickCount) {
                if (clickCount == 2) {
                    GHReleaseWrapper release = ((GHReleaseTableModel) repoReleasesTable.getModel()).getRow(row);
                    openWebLink(release.getFullUrl());
                }
            }
        });
        repoPullRequestsTable.addMouseListener(new SimpleMouseAdapter() {
            public void mousePressed(int row, int column, int clickCount) {
                if (clickCount == 2) {
                    GHPullRequestWrapper pullRequest = ((GHPullRequestTableModel) repoPullRequestsTable.getModel()).getRow(row);
                    openWebLink(pullRequest.getFullUrl());
                }
            }
        });
    }

    @Override
    public void loadRepositories() {
        cancelThread(loaderThread);
        loaderThread = new Thread(() -> {
            final GHRepoTableModel reposTableModel = new GHRepoTableModel(new ArrayList<>(), new String[]{Strings.NAME});
            reposTable.setModel(reposTableModel);
            TableRowSorter<TableModel> sorter = new TableRowSorter<>(reposTable.getModel());
            List<SortKey> sortKeys = new ArrayList<>();
            sortKeys.add(new SortKey(0, SortOrder.ASCENDING));
            sorter.setSortKeys(sortKeys);
            reposTable.setRowSorter(sorter);
            ghOrganization.listRepositories().withPageSize(LARGE_PAGE_SIZE).forEach(repository -> {
                if (repository.isFork() || !repository.getFullName().startsWith(getOrganizationName())) {
                    //Ignore forks or projects that doesn't belong to this organization
                    return;
                }
                SwingUtilities.invokeLater(() -> reposTableModel.addRow(new GHRepositoryWrapper(repository)));
            });
        });
        loaderThread.start();
    }

    @Override
    public void cancel() {
        cancelThread(releasesLoaderThread);
        cancelThread(prsLoaderThread);
    }

    @Subscribe
    public void onFavoriteRepositoryUpdated(FavoriteRepositoryUpdatedEvent event) {
        ((AbstractTableModel) reposTable.getModel()).fireTableDataChanged();
    }
}
