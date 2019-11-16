package com.tal.android.plugin.github.ui.controllers;

import org.kohsuke.github.GHDirection;
import org.kohsuke.github.GHIssueState;
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
import javax.swing.SwingUtilities;

import com.tal.android.plugin.github.domain.gh.GHPullRequestWrapper;
import com.tal.android.plugin.github.domain.gh.GHReleaseWrapper;
import com.tal.android.plugin.github.domain.gh.GHRepositoryWrapper;
import com.tal.android.plugin.github.ui.tablemodels.GHPullRequestTableModel;
import com.tal.android.plugin.github.ui.tablemodels.GHReleaseTableModel;
import com.tal.android.plugin.utils.Strings;

public abstract class BaseController {

    static final int LARGE_PAGE_SIZE = 500;

    Thread loaderThread;
    Thread releasesLoaderThread;
    Thread prsLoaderThread;

    JTable reposTable;
    JTable repoReleasesTable;
    JTable repoPullRequestsTable;
    private JLabel repoComments;

    BaseController(JTable reposTable, JTable repoReleasesTable, JTable repoPullRequestsTable, JLabel repoComments) {
        this.reposTable = reposTable;
        this.repoReleasesTable = repoReleasesTable;
        this.repoPullRequestsTable = repoPullRequestsTable;
        this.repoComments = repoComments;
    }

    protected abstract void startListeningBus();

    public abstract void init();

    public abstract void loadRepositories();

    public abstract void cancel();

    void cancelThread(Thread thread) {
        if (thread != null) {
            thread.interrupt();
        }
    }

    private static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void openWebpage(URL url) {
        try {
            openWebpage(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void updateRepoComments(GHRepositoryWrapper repository, @Nullable GHReleaseWrapper latestRelease) {
        if (latestRelease == null) {
            repoComments.setText(Strings.REPO_NO_RELEASES_YET);
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
                if (needsRelease) {
                    repoComments.setText(Strings.REPO_NEEDS_RELEASE);
                } else {
                    repoComments.setText(Strings.REPO_DOES_NOT_NEED_RELEASE);
                }
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

    void updateRepositoryInfo(GHRepositoryWrapper repository) {
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
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
