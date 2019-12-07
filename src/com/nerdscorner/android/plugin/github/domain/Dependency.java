package com.nerdscorner.android.plugin.github.domain;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JButton;

public class Dependency {
    private String id;
    private String name;
    private int level;
    private String url;
    private List<Dependency> dependsOn;
    private String dependsOnIds;

    private JButton widget;

    public Dependency(String id, String name, int level, String url, @Nullable String dependsOnIds) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.url = url;
        this.dependsOnIds = dependsOnIds;
    }

    public Dependency(String id, String name, int level, String url, @Nonnull List<Dependency> dependsOn) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.url = url;
        this.dependsOn = dependsOn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<Dependency> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<Dependency> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public JButton getWidget() {
        return widget;
    }

    public void setWidget(JButton widget) {
        this.widget = widget;
    }

    public String getDependsOnIds() {
        return dependsOnIds;
    }

    public void setDependsOnIds(String dependsOnIds) {
        this.dependsOnIds = dependsOnIds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
