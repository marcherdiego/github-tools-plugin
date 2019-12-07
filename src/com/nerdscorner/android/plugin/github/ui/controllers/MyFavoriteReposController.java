package com.nerdscorner.android.plugin.github.ui.controllers;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHOrganization;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper;
import com.nerdscorner.android.plugin.github.events.FavoriteRepositoryUpdatedEvent;
import com.nerdscorner.android.plugin.github.ui.tablemodels.BaseModel;
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHMyReposTableModel;
import com.nerdscorner.android.plugin.utils.RepositoryUtils;
import com.nerdscorner.android.plugin.utils.Strings;

public class MyFavoriteReposController extends BaseRepoListController {

    private final RepositoryUtils repositoryUtils;
    private final GHMyself myselfGitHub;

    public MyFavoriteReposController(JTable reposTable, JTable repoReleases, JTable repoPullRequestsTable, JTable repoClosedPullRequestsTable,
                                     JLabel repoComments, GHMyself myselfGitHub, GHOrganization ghOrganization) {
        super(reposTable, repoReleases, repoPullRequestsTable, repoClosedPullRequestsTable, repoComments, ghOrganization, GHMyReposTableModel.COLUMN_NAME);
        this.myselfGitHub = myselfGitHub;
        repositoryUtils = RepositoryUtils.getInstance();
    }

    @Override
    public void loadRepositories() {
        cancelThread(loaderThread);
        loaderThread = new Thread(() -> {
            final GHMyReposTableModel myReposTableModel = new GHMyReposTableModel(new ArrayList<>(), new String[]{Strings.FAVORITE, Strings.NAME});
            reposTable.setModel(myReposTableModel);
            reposTable.getColumnModel().getColumn(GHMyReposTableModel.COLUMN_FAV).setMaxWidth(40);
            TableRowSorter<TableModel> sorter = new TableRowSorter<>(reposTable.getModel());
            List<SortKey> sortKeys = new ArrayList<>();
            sortKeys.add(new SortKey(GHMyReposTableModel.COLUMN_NAME, SortOrder.ASCENDING));
            sorter.setSortKeys(sortKeys);
            reposTable.setRowSorter(sorter);
            myselfGitHub
                    .listSubscriptions()
                    .withPageSize(LARGE_PAGE_SIZE)
                    .forEach(repository -> {
                        if (repository.isFork()
                                || !repository.getFullName().startsWith(getOrganizationName())
                                || !repositoryUtils.isFavorite(repository.getName())) {
                            //Ignore forks, projects that doesn't belong to this organization, or projects that are not in favorites
                            return;
                        }
                        SwingUtilities.invokeLater(() -> myReposTableModel.addRow(new GHRepositoryWrapper(repository)));
                    });
        });
        loaderThread.start();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFavoriteRepositoryUpdated(FavoriteRepositoryUpdatedEvent event) {
        GHMyReposTableModel myReposTableModel = (GHMyReposTableModel) reposTable.getModel();
        if (event.added) {
            myReposTableModel.addRow(event.repository);
        } else {
            myReposTableModel.removeRepository(event.repository);
            if (event.repository == currentRepository) {
                ((BaseModel<?>) repoReleasesTable.getModel()).removeAllRows();
                ((BaseModel<?>) repoOpenPullRequestsTable.getModel()).removeAllRows();
            }
        }
        myReposTableModel.fireTableDataChanged();
    }
}
