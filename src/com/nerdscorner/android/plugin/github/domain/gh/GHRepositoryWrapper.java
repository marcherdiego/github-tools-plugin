package com.nerdscorner.android.plugin.github.domain.gh;

import org.kohsuke.github.GHRepository;

import java.io.Serializable;
import java.net.URL;

import com.nerdscorner.android.plugin.utils.StringUtils;

public class GHRepositoryWrapper extends Wrapper implements Serializable {
    private final String description;
    private transient GHRepository ghRepository;
    private String name;

    public GHRepositoryWrapper(GHRepository ghRepository) {
        this.ghRepository = ghRepository;
        this.name = ghRepository.getName();
        String repoDescription = ghRepository.getDescription();
        this.description = StringUtils.isNullOrEmpty(repoDescription) ? name : repoDescription;
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public int compare(Wrapper other) {
        if (other instanceof GHRepositoryWrapper) {
            GHRepositoryWrapper otherWrapper = (GHRepositoryWrapper) other;
            return otherWrapper.name.toLowerCase().compareTo(name.toLowerCase());
        }
        return 0;
    }
}
