package com.nerdscorner.android.plugin.github.domain;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.swing.JButton;

public class Dependency {
    private String name;
    private String url;
    private List<Dependency> dependsOn;

    private JButton widget;

    public Dependency(String name, String url) {
        this(name, url, new ArrayList<>());
    }

    public Dependency(String name, String url, @Nonnull List<Dependency> dependsOn) {
        this.name = name;
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
}
