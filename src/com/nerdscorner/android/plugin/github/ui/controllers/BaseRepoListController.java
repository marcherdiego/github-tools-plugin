package com.nerdscorner.android.plugin.github.ui.controllers;

import org.greenrobot.eventbus.EventBus;
import org.kohsuke.github.GHDirection;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestQueryBuilder.Sort;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.PagedIterable;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

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
    static final int SMALL_PAGE_SIZE = 10;

    /* default */ GHOrganization ghOrganization;
    /* default */ GHRepositoryWrapper currentRepository;

    /* default */ Thread loaderThread;
    private Thread releasesLoaderThread;
    private Thread prsLoaderThread;

    /* default */ JTable reposTable;
    /* default */ JTable repoReleasesTable;
    /* default */ JTable repoOpenPullRequestsTable;
    /* default */ JTable repoClosedPullRequestsTable;

    private JLabel repoComments;
    private int dataColumn;

    BaseRepoListController(JTable reposTable, JTable repoReleasesTable, JTable repoOpenPullRequestsTable, JTable repoClosedPullRequestsTable,
                           JLabel repoComments, GHOrganization ghOrganization, int dataColumn) {
        this.reposTable = reposTable;
        this.repoReleasesTable = repoReleasesTable;
        this.repoOpenPullRequestsTable = repoOpenPullRequestsTable;
        this.repoClosedPullRequestsTable = repoClosedPullRequestsTable;
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
                    updateRepositoryInfo();
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
        repoOpenPullRequestsTable.addMouseListener(new SimpleMouseAdapter() {
            public void mousePressed(int row, int column, int clickCount) {
                if (clickCount == 2) {
                    GHPullRequestWrapper pullRequest = ((GHPullRequestTableModel) repoOpenPullRequestsTable.getModel()).getRow(row);
                    openWebLink(pullRequest.getFullUrl());
                }
            }
        });
        repoClosedPullRequestsTable.addMouseListener(new SimpleMouseAdapter() {
            public void mousePressed(int row, int column, int clickCount) {
                if (clickCount == 2) {
                    GHPullRequestWrapper pullRequest = ((GHPullRequestTableModel) repoClosedPullRequestsTable.getModel()).getRow(row);
                    openWebLink(pullRequest.getFullUrl());
                }
            }
        });
        reposTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        repoReleasesTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        repoOpenPullRequestsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        repoClosedPullRequestsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        if (reposTable.getModel() instanceof BaseModel<?>) {
            ((BaseModel<?>) reposTable.getModel()).removeAllRows();
        }
        if (repoReleasesTable.getModel() instanceof BaseModel<?>) {
            ((BaseModel<?>) repoReleasesTable.getModel()).removeAllRows();
        }
        if (repoOpenPullRequestsTable.getModel() instanceof BaseModel<?>) {
            ((BaseModel<?>) repoOpenPullRequestsTable.getModel()).removeAllRows();
        }
        if (repoClosedPullRequestsTable.getModel() instanceof BaseModel<?>) {
            ((BaseModel<?>) repoClosedPullRequestsTable.getModel()).removeAllRows();
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

    private void updateRepoComments(GHRepositoryWrapper repository) {
        GHReleaseWrapper latestRelease = ((GHReleaseTableModel) repoReleasesTable.getModel()).getRow(0);
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
                System.out.println(mergedAt + " latest pr closed");
                System.out.println(latestRelease.getGhRelease().getPublished_at() + " latest release published");
                boolean needsRelease = mergedAt.after(latestRelease.getGhRelease().getPublished_at());
                String repoMessage = needsRelease ? Strings.REPO_NEEDS_RELEASE : Strings.REPO_DOES_NOT_NEED_RELEASE;
                repoComments.setText(String.format(repoMessage, repository.getGhRepository().getName()));
                return;
            }
        }
    }

    private void loadReleases(GHRepository repository) throws IOException {
        final GHReleaseTableModel repoReleasesModel = new GHReleaseTableModel(new ArrayList<>(), new String[]{Strings.TAG, Strings.DATE});
        repoReleasesTable.setModel(repoReleasesModel);
        repository
                .listReleases()
                .withPageSize(SMALL_PAGE_SIZE)
                .forEach(ghRelease -> repoReleasesModel.addRow(new GHReleaseWrapper(ghRelease)));
    }

    private void loadPullRequests(GHIssueState state, JTable table) {
        final GHPullRequestTableModel prListModel = new GHPullRequestTableModel(
                new ArrayList<>(),
                new String[]{Strings.TITLE, Strings.AUTHOR, Strings.DATE}
        );
        table.setModel(prListModel);
        currentRepository
                .getGhRepository()
                .queryPullRequests()
                .state(state)
                .sort(Sort.UPDATED)
                .direction(GHDirection.DESC)
                .list()
                .withPageSize(SMALL_PAGE_SIZE)
                .forEach(ghPullRequest -> prListModel.addRow(new GHPullRequestWrapper(ghPullRequest)));
    }

    private void updateRepositoryInfo() {
        repoComments.setText(null);
        try {
            cancelThread(releasesLoaderThread);
            cancelThread(prsLoaderThread);

            releasesLoaderThread = new Thread(() -> {
                try {
                    loadReleases(currentRepository.getGhRepository());
                    updateRepoComments(currentRepository);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            releasesLoaderThread.start();

            prsLoaderThread = new Thread(() -> {
                loadPullRequests(GHIssueState.OPEN, repoOpenPullRequestsTable);
                loadPullRequests(GHIssueState.CLOSED, repoClosedPullRequestsTable);
            });
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
