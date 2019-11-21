package com.nerdscorner.android.plugin.github.ui.controllers;

import org.greenrobot.eventbus.EventBus;
import org.kohsuke.github.GHDirection;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestQueryBuilder.Sort;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.PagedIterable;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import com.nerdscorner.android.plugin.github.domain.gh.GHPullRequestWrapper;
import com.nerdscorner.android.plugin.github.domain.gh.GHReleaseWrapper;
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper;
import com.nerdscorner.android.plugin.github.ui.tablemodels.BaseModel;
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHPullRequestTableModel;
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHReleaseTableModel;
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleMouseAdapter;
import com.nerdscorner.android.plugin.utils.Strings;

public abstract class BaseRepoListController {

    static final int LARGE_PAGE_SIZE = 500;

    /* default */ GHOrganization ghOrganization;
    /* default */ GHRepositoryWrapper currentRepository;

    /* default */ Thread loaderThread;
    private Thread releasesLoaderThread;
    private Thread prsLoaderThread;

    /* default */ JTable reposTable;
    /* default */ JTable repoReleasesTable;
    /* default */ JTable repoPullRequestsTable;

    private JLabel repoComments;
    private int dataColumn;

    BaseRepoListController(JTable reposTable, JTable repoReleasesTable, JTable repoPullRequestsTable, JLabel repoComments,
                           GHOrganization ghOrganization, int dataColumn) {
        this.reposTable = reposTable;
        this.repoReleasesTable = repoReleasesTable;
        this.repoPullRequestsTable = repoPullRequestsTable;
        this.repoComments = repoComments;
        this.ghOrganization = ghOrganization;
        this.dataColumn = dataColumn;
    }

    private void startListeningBus() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    public void init() {
        startListeningBus();

        reposTable.addMouseListener(new SimpleMouseAdapter() {
            public void mousePressed(int row, int column, int clickCount) {
                currentRepository = (GHRepositoryWrapper) reposTable.getValueAt(row, dataColumn);
                if (clickCount == 1) {
                    updateRepositoryInfo(currentRepository);
                } else if (clickCount == 2) {
                    openWebLink(currentRepository.getFullUrl());
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
        reposTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        repoReleasesTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        repoPullRequestsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        if (reposTable.getModel() instanceof BaseModel<?>) {
            ((BaseModel<?>) reposTable.getModel()).removeAllRows();
        }
        if (repoReleasesTable.getModel() instanceof BaseModel<?>) {
            ((BaseModel<?>) repoReleasesTable.getModel()).removeAllRows();
        }
        if (repoPullRequestsTable.getModel() instanceof BaseModel<?>) {
            ((BaseModel<?>) repoPullRequestsTable.getModel()).removeAllRows();
        }
    }

    public void cancel() {
        cancelThread(loaderThread);
        cancelThread(releasesLoaderThread);
        cancelThread(prsLoaderThread);
    }

    void cancelThread(Thread thread) {
        if (thread != null) {
            thread.interrupt();
        }
    }

    private static void openWebLink(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void openWebLink(URL url) {
        try {
            openWebLink(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void updateRepoComments(GHRepositoryWrapper repository, @Nullable GHReleaseWrapper latestRelease) {
        if (latestRelease == null) {
            repoComments.setText(String.format(Strings.REPO_NO_RELEASES_YET, repository.getGhRepository().getName()));
            return;
        }
        PagedIterable<GHPullRequest> closedPrs = repository
                .getGhRepository()
                .queryPullRequests()
                .state(GHIssueState.CLOSED)
                .sort(Sort.UPDATED)
                .direction(GHDirection.DESC)
                .list()
                .withPageSize(LARGE_PAGE_SIZE);
        for (GHPullRequest closedPr : closedPrs) {
            Date mergedAt = closedPr.getMergedAt();
            if (mergedAt != null) {
                boolean needsRelease = mergedAt.after(latestRelease.getGhRelease().getPublished_at());
                String repoMessage = needsRelease ? Strings.REPO_NEEDS_RELEASE : Strings.REPO_DOES_NOT_NEED_RELEASE;
                repoComments.setText(String.format(repoMessage, repository.getGhRepository().getName()));
                return;
            }
        }
    }

    private void loadReleases(GHRepository repository, JTable releasesTable) throws IOException {
        final GHReleaseTableModel repoReleasesModel = new GHReleaseTableModel(new ArrayList<>(), new String[]{Strings.TAG, Strings.DATE});
        releasesTable.setModel(repoReleasesModel);
        List<GHRelease> releases = repository
                .listReleases()
                .withPageSize(LARGE_PAGE_SIZE)
                .asList();
        for (GHRelease release : releases) {
            repoReleasesModel.addRow(new GHReleaseWrapper(release));
        }
        SwingUtilities.invokeLater(() -> repoReleasesModel.fireTableRowsInserted(releases.size(), releases.size()));
    }

    private void loadPullRequests(GHRepository repository, JTable prsTable) {
        final GHPullRequestTableModel repoPullRequestsModel = new GHPullRequestTableModel(
                new ArrayList<>(),
                new String[]{Strings.TITLE, Strings.AUTHOR, Strings.DATE}
        );
        prsTable.setModel(repoPullRequestsModel);
        List<GHPullRequest> pullRequests = repository
                .queryPullRequests()
                .state(GHIssueState.OPEN)
                .sort(Sort.UPDATED)
                .direction(GHDirection.DESC)
                .list()
                .withPageSize(LARGE_PAGE_SIZE)
                .asList();
        for (GHPullRequest ghPullRequest : pullRequests) {
            repoPullRequestsModel.addRow(new GHPullRequestWrapper(ghPullRequest));
        }
        SwingUtilities.invokeLater(() -> repoPullRequestsModel.fireTableRowsInserted(pullRequests.size(), pullRequests.size()));
    }

    private void updateRepositoryInfo(GHRepositoryWrapper repository) {
        repoComments.setText(null);
        try {
            cancelThread(releasesLoaderThread);
            cancelThread(prsLoaderThread);

            releasesLoaderThread = new Thread(() -> {
                try {
                    loadReleases(repository.getGhRepository(), repoReleasesTable);
                    updateRepoComments(repository, ((GHReleaseTableModel) repoReleasesTable.getModel()).getRow(0));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            releasesLoaderThread.start();

            prsLoaderThread = new Thread(() -> loadPullRequests(repository.getGhRepository(), repoPullRequestsTable));
            prsLoaderThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* default */ String getOrganizationName() {
        return ghOrganization.getLogin();
    }

    public abstract void loadRepositories();
}
