package com.tal.android.plugin.github.ui.controllers;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.kohsuke.github.GHMyself;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.tal.android.plugin.github.domain.gh.GHPullRequestWrapper;
import com.tal.android.plugin.github.domain.gh.GHReleaseWrapper;
import com.tal.android.plugin.github.domain.gh.GHRepositoryWrapper;
import com.tal.android.plugin.github.events.FavoriteRepositoryUpdatedEvent;
import com.tal.android.plugin.github.ui.tablemodels.GHMyReposTableModel;
import com.tal.android.plugin.github.ui.tablemodels.GHPullRequestTableModel;
import com.tal.android.plugin.github.ui.tablemodels.GHReleaseTableModel;
import com.tal.android.plugin.utils.RepositoryUtils;
import com.tal.android.plugin.utils.Strings;

public class MyFavoriteReposController extends BaseController {

    private final RepositoryUtils repositoryUtils;
    private final GHMyself myselfGitHub;
    private GHRepositoryWrapper currentRepository;

    public MyFavoriteReposController(JTable reposTable, JTable repoReleases, JTable repoPullRequestsTable, JLabel repoComments, GHMyself myselfGitHub) {
        super(reposTable, repoReleases, repoPullRequestsTable, repoComments);
        this.myselfGitHub = myselfGitHub;
        repositoryUtils = RepositoryUtils.getInstance();
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

        reposTable.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent mouseEvent) {
                int row = ((JTable) mouseEvent.getSource()).rowAtPoint(mouseEvent.getPoint());
                if (row == -1) {
                    return;
                }
                currentRepository = (GHRepositoryWrapper) reposTable.getValueAt(row, GHMyReposTableModel.COLUMN_NAME);
                if (mouseEvent.getClickCount() == 1) {
                    updateRepositoryInfo(currentRepository);
                } else if (mouseEvent.getClickCount() == 2) {
                    openWebpage(currentRepository.getFullUrl());
                }
            }
        });
        repoReleasesTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                int row = ((JTable) mouseEvent.getSource()).rowAtPoint(mouseEvent.getPoint());
                if (mouseEvent.getClickCount() == 2 && row != -1) {
                    GHReleaseWrapper release = ((GHReleaseTableModel) repoReleasesTable.getModel()).getRow(row);
                    openWebpage(release.getFullUrl());
                }
            }
        });
        repoPullRequestsTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                int row = ((JTable) mouseEvent.getSource()).rowAtPoint(mouseEvent.getPoint());
                if (mouseEvent.getClickCount() == 2 && row != -1) {
                    GHPullRequestWrapper pullRequest = ((GHPullRequestTableModel) repoPullRequestsTable.getModel()).getRow(row);
                    openWebpage(pullRequest.getFullUrl());
                }
            }
        });
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
            myselfGitHub.listSubscriptions().withPageSize(LARGE_PAGE_SIZE).forEach(repository -> {
                if (repository.isFork()
                        || !repository.getFullName().startsWith(Strings.ORGANIZATION)
                        || !repositoryUtils.isFavorite(repository.getName())) {
                    //Ignore forks, projects that doesn't belong to this organization, or projects that are not in favorites
                    return;
                }
                SwingUtilities.invokeLater(() -> myReposTableModel.addRow(new GHRepositoryWrapper(repository)));
            });
        });
        loaderThread.start();
    }

    @Override
    public void cancel() {
        cancelThread(releasesLoaderThread);
        cancelThread(prsLoaderThread);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFavoriteRepositoryUpdated(FavoriteRepositoryUpdatedEvent event) {
        GHMyReposTableModel myReposTableModel = (GHMyReposTableModel) reposTable.getModel();
        if (event.added) {
            myReposTableModel.addRow(event.repository);
        } else {
            myReposTableModel.removeRepository(event.repository);
            if (event.repository == currentRepository) {
                ((GHReleaseTableModel) repoReleasesTable.getModel()).removeAllRows();
                ((GHPullRequestTableModel) repoPullRequestsTable.getModel()).removeAllRows();
            }
        }
        myReposTableModel.fireTableDataChanged();
    }
}
