package com.nerdscorner.android.plugin.github.domain.gh;

import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.net.URL;

public class GHPullRequestWrapper extends Wrapper {
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

    @Override
    public int compare(Wrapper other) {
        if (other instanceof GHPullRequestWrapper) {
            GHPullRequestWrapper otherWrapper = (GHPullRequestWrapper) other;
            try {
                return otherWrapper.ghPullRequest.getUpdatedAt().compareTo(ghPullRequest.getUpdatedAt());
            } catch (Exception ignored) {
            }
        }
        return 0;
    }
}
