package com.nerdscorner.android.plugin.github.domain.gh;

import org.kohsuke.github.GHRelease;

import java.net.URL;

public class GHReleaseWrapper extends Wrapper {
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

    @Override
    public int compare(Wrapper other) {
        if (other instanceof GHReleaseWrapper) {
            GHReleaseWrapper otherWrapper = (GHReleaseWrapper) other;
            try {
                return otherWrapper.ghRelease.getUpdatedAt().compareTo(ghRelease.getUpdatedAt());
            } catch (Exception ignored) {
            }
        }
        return 0;
    }
}
