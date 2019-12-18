package com.nerdscorner.android.plugin.github.ui.controllers;

import org.greenrobot.eventbus.EventBus;
import org.kohsuke.github.GHDirection;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequestQueryBuilder.Sort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.annotation.Nullable;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import com.nerdscorner.android.plugin.github.domain.gh.GHPullRequestWrapper;
import com.nerdscorner.android.plugin.github.domain.gh.GHReleaseWrapper;
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper;
import com.nerdscorner.android.plugin.github.ui.tablemodels.BaseModel;
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHPullRequestTableModel;
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHReleaseTableModel;
import com.nerdscorner.android.plugin.utils.GithubUtils;
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleMouseAdapter;
import com.nerdscorner.android.plugin.utils.Strings;
import com.nerdscorner.android.plugin.utils.ThreadUtils;

public abstract class BaseRepoListController {

    static final int LARGE_PAGE_SIZE = 500;
    private static final int SMALL_PAGE_SIZE = 40;

    /* default */ GHOrganization ghOrganization;
    private GHRepositoryWrapper currentRepository;

    /* default */ Thread loaderThread;
    private Thread repoDataLoaderThread;

    /* default */ JTable reposTable;
    private JTable repoReleasesTable;
    private JTable repoOpenPullRequestsTable;
    private JTable repoClosedPullRequestsTable;

    private JLabel repoComments;
    private int dataColumn;

    private boolean commentsUpdated;

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
                    updateRepositoryInfoTables();
                } else if (clickCount == 2) {
                    GithubUtils.openWebLink(currentRepository.getFullUrl());
                }
            }
        });
        repoReleasesTable.addMouseListener(new SimpleMouseAdapter() {
            public void mousePressed(int row, int column, int clickCount) {
                if (clickCount == 2) {
                    GHReleaseWrapper release = ((GHReleaseTableModel) repoReleasesTable.getModel()).getRow(row);
                    GithubUtils.openWebLink(release.getFullUrl());
                }
            }
        });
        repoOpenPullRequestsTable.addMouseListener(new SimpleMouseAdapter() {
            public void mousePressed(int row, int column, int clickCount) {
                if (clickCount == 2) {
                    GHPullRequestWrapper pullRequest = ((GHPullRequestTableModel) repoOpenPullRequestsTable.getModel()).getRow(row);
                    GithubUtils.openWebLink(pullRequest.getFullUrl());
                }
            }
        });
        repoClosedPullRequestsTable.addMouseListener(new SimpleMouseAdapter() {
            public void mousePressed(int row, int column, int clickCount) {
                if (clickCount == 2) {
                    GHPullRequestWrapper pullRequest = ((GHPullRequestTableModel) repoClosedPullRequestsTable.getModel()).getRow(row);
                    GithubUtils.openWebLink(pullRequest.getFullUrl());
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
        ThreadUtils.cancelThread(loaderThread);
        ThreadUtils.cancelThread(repoDataLoaderThread);
    }

    @Nullable
    private Date getLatestReleaseDate() {
        GHReleaseWrapper latestRelease = ((GHReleaseTableModel) repoReleasesTable.getModel()).getRow(0);
        return latestRelease == null ? null : latestRelease.getGhRelease().getPublished_at();
    }

    private void loadReleases() throws IOException {
        final GHReleaseTableModel repoReleasesModel = new GHReleaseTableModel(new ArrayList<>(), new String[]{Strings.TAG, Strings.DATE});
        repoReleasesTable.setModel(repoReleasesModel);
        currentRepository
                .getGhRepository()
                .listReleases()
                .withPageSize(SMALL_PAGE_SIZE)
                .forEach(ghRelease -> repoReleasesModel.addRow(new GHReleaseWrapper(ghRelease)));
    }

    private void loadPullRequests() {
        final GHPullRequestTableModel openPrsModel = new GHPullRequestTableModel(
                new ArrayList<>(),
                new String[]{Strings.TITLE, Strings.AUTHOR, Strings.DATE}
        );
        final GHPullRequestTableModel closedPrsModel = new GHPullRequestTableModel(
                new ArrayList<>(),
                new String[]{Strings.TITLE, Strings.AUTHOR, Strings.DATE}
        );
        repoOpenPullRequestsTable.setModel(openPrsModel);
        repoClosedPullRequestsTable.setModel(closedPrsModel);
        final String repoName = currentRepository.getGhRepository().getName();
        final Date latestReleaseDate = getLatestReleaseDate();
        repoComments.setText("Analyzing releases and merged pull requests...");
        currentRepository
                .getGhRepository()
                .queryPullRequests()
                .state(GHIssueState.ALL)
                .sort(Sort.UPDATED)
                .direction(GHDirection.DESC)
                .list()
                .withPageSize(SMALL_PAGE_SIZE)
                .forEach(pullRequest -> {
                    if (pullRequest.getState() == GHIssueState.OPEN) {
                        openPrsModel.addRow(new GHPullRequestWrapper(pullRequest));
                    } else if (pullRequest.getState() == GHIssueState.CLOSED) {
                        if (latestReleaseDate == null) {
                            commentsUpdated = true;
                            repoComments.setText(String.format(Strings.REPO_NO_RELEASES_YET, repoName));
                            return;
                        }
                        Date mergedAt = pullRequest.getMergedAt();
                        if (mergedAt != null) {
                            if (mergedAt.after(latestReleaseDate)) {
                                closedPrsModel.addRow(new GHPullRequestWrapper(pullRequest));
                                if (!commentsUpdated) {
                                    commentsUpdated = true;
                                    repoComments.setText(String.format(Strings.REPO_NEEDS_RELEASE, repoName));
                                }
                            }
                        }
                    }
                });
        // If we haven't found any closed PR after the latest release, then this repo does not need to be released
        if (!commentsUpdated) {
            commentsUpdated = true;
            repoComments.setText(String.format(Strings.REPO_DOES_NOT_NEED_RELEASE, repoName));
        }
    }

    private void updateRepositoryInfoTables() {
        repoComments.setText(null);
        try {
            ThreadUtils.cancelThread(repoDataLoaderThread);
            repoDataLoaderThread = new Thread(() -> {
                try {
                    commentsUpdated = false;
                    loadReleases();
                    loadPullRequests();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            repoDataLoaderThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* default */ String getOrganizationName() {
        return ghOrganization.getLogin();
    }

    public abstract void loadRepositories();
}
