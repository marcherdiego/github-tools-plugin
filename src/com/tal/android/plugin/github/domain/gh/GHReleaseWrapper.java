package com.tal.android.plugin.github.domain.gh;

import org.kohsuke.github.GHRelease;

import java.net.URL;

public class GHReleaseWrapper {
    private GHRelease ghRelease;

    public GHReleaseWrapper(GHRelease ghRelease) {
        this.ghRelease = ghRelease;
    }

    public String getUrl() {
        return ghRelease.getUrl().toString();
    }

    public URL getFullUrl() {
        return ghRelease.getHtmlUrl();
    }

    public GHRelease getGhRelease() {
        return ghRelease;
    }

    @Override
    public String toString() {
        return ghRelease.getName();
    }
}
