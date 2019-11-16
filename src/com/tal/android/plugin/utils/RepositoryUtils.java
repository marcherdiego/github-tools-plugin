package com.tal.android.plugin.utils;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.ide.util.PropertiesComponent;
import com.tal.android.plugin.github.domain.gh.GHRepositoryWrapper;
import com.tal.android.plugin.github.events.FavoriteRepositoryUpdatedEvent;

public class RepositoryUtils {

    private static final Gson GSON = new Gson();
    private static RepositoryUtils instance;

    private final PropertiesComponent propertiesComponent;
    private final List<String> favoriteRepos;

    private RepositoryUtils() {
        propertiesComponent = PropertiesComponent.getInstance();
        String favoriteRepositoriesJson = propertiesComponent.getValue(Strings.FAVORITE_REPOSITORIES, Strings.EMPTY_LIST);
        favoriteRepos = GSON.fromJson(favoriteRepositoriesJson, new TypeToken<List<String>>() {
        }.getType());
    }

    public static RepositoryUtils getInstance() {
        if (instance == null) {
            instance = new RepositoryUtils();
        }
        return instance;
    }

    public void toggleFavorite(GHRepositoryWrapper repository) {
        boolean added;
        if (isFavorite(repository)) {
            favoriteRepos.remove(repository.toString());
            added = false;
        } else {
            favoriteRepos.add(repository.toString());
            added = true;
        }
        EventBus.getDefault().post(new FavoriteRepositoryUpdatedEvent(added, repository));
        propertiesComponent.setValue(Strings.FAVORITE_REPOSITORIES, GSON.toJson(favoriteRepos));
    }

    public boolean isFavorite(GHRepositoryWrapper repository) {
        return favoriteRepos != null && favoriteRepos.contains(repository.toString());
    }

    public boolean isFavorite(String repositoryName) {
        return favoriteRepos != null && favoriteRepos.contains(repositoryName);
    }
}
