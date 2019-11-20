package com.nerdscorner.android.plugin.github.domain.gh;

import org.kohsuke.github.GHRepository;

import java.io.Serializable;
import java.net.URL;

public class GHRepositoryWrapper implements Serializable {
    private transient GHRepository ghRepository;
    private String name;

    public GHRepositoryWrapper(GHRepository ghRepository) {
        this.ghRepository = ghRepository;
        this.name = ghRepository.getName();
    }

    public String getUrl() {
        return ghRepository.getUrl().toString();
    }

    public URL getFullUrl() {
        return ghRepository.getHtmlUrl();
    }

    public GHRepository getGhRepository() {
        return ghRepository;
    }

    @Override
    public String toString() {
        return name;
    }
}
