package com.nerdscorner.android.plugin.github.domain.gh;

import org.kohsuke.github.GHPullRequest;

import java.net.URL;

public class GHPullRequestWrapper {
    private GHPullRequest ghPullRequest;

    public GHPullRequestWrapper(GHPullRequest ghPullRequest) {
        this.ghPullRequest = ghPullRequest;
    }

    public GHPullRequest getGhPullRequest() {
        return ghPullRequest;
    }

    public URL getFullUrl() {
        return ghPullRequest.getHtmlUrl();
    }

    @Override
    public String toString() {
        return ghPullRequest.getTitle();
    }
}
