package com.nerdscorner.android.plugin.github.ui.controllers;

import org.greenrobot.eventbus.EventBus;
import org.kohsuke.github.GHDirection;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestQueryBuilder.Sort;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
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
import com.nerdscorner.android.plugin.utils.GithubUtils;
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleMouseAdapter;
import com.nerdscorner.android.plugin.utils.Strings;
import com.nerdscorner.android.plugin.utils.ThreadUtils;

public abstract class BaseRepoListController {

    static final int LARGE_PAGE_SIZE = 500;
    private static final int SMALL_PAGE_SIZE = 25;

    /* default */ GHOrganization ghOrganization;
    private GHRepositoryWrapper currentRepository;

    /* default */ Thread loaderThread;
    private Thread releasesLoaderThread;
    private Thread prsLoaderThread;

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
        ThreadUtils.cancelThread(releasesLoaderThread);
        ThreadUtils.cancelThread(prsLoaderThread);
    }

    private void updateRepoComments(GHRepositoryWrapper repository, GHPullRequest latestClosedPr) {
        GHReleaseWrapper latestRelease = ((GHReleaseTableModel) repoReleasesTable.getModel()).getRow(0);
        if (latestRelease == null) {
            repoComments.setText(String.format(Strings.REPO_NO_RELEASES_YET, repository.getGhRepository().getName()));
            return;
        }
        Date mergedAt = latestClosedPr.getMergedAt();
        if (mergedAt != null) {
            System.out.println(mergedAt + " latest pr closed");
            System.out.println(latestRelease.getGhRelease().getPublished_at() + " latest release published");
            boolean needsRelease = mergedAt.after(latestRelease.getGhRelease().getPublished_at());
            String repoMessage = needsRelease ? Strings.REPO_NEEDS_RELEASE : Strings.REPO_DOES_NOT_NEED_RELEASE;
            repoComments.setText(String.format(repoMessage, repository.getGhRepository().getName()));
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
        commentsUpdated = false;
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
                    } else {
                        closedPrsModel.addRow(new GHPullRequestWrapper(pullRequest));
                        if (!commentsUpdated) {
                            commentsUpdated = true;
                            updateRepoComments(currentRepository, pullRequest);
                        }
                    }
                });
    }

    private void updateRepositoryInfoTables() {
        repoComments.setText(null);
        try {
            ThreadUtils.cancelThread(releasesLoaderThread);
            ThreadUtils.cancelThread(prsLoaderThread);

            releasesLoaderThread = new Thread(() -> {
                try {
                    loadReleases(currentRepository.getGhRepository());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            releasesLoaderThread.start();

            prsLoaderThread = new Thread(this::loadPullRequests);
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
