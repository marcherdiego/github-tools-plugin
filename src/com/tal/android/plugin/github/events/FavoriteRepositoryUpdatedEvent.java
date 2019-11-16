package com.tal.android.plugin.github.events;

import com.tal.android.plugin.github.domain.gh.GHRepositoryWrapper;

public final class FavoriteRepositoryUpdatedEvent {
    public final boolean added;
    public final GHRepositoryWrapper repository;

    public FavoriteRepositoryUpdatedEvent(boolean added, GHRepositoryWrapper repository) {
        this.added = added;
        this.repository = repository;
    }
}
