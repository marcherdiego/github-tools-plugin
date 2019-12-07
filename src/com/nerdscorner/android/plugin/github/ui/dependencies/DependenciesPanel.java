package com.nerdscorner.android.plugin.github.ui.dependencies;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.nerdscorner.android.plugin.github.domain.Dependency;
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper;
import com.nerdscorner.android.plugin.utils.ArtifactoryUtils;
import com.nerdscorner.android.plugin.utils.ViewUtils;
import javafx.util.Pair;

public class DependenciesPanel extends JPanel {

    private List<Pair<JButton, JButton>> dependenciesAssociations = new ArrayList<>();

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (dependenciesAssociations.isEmpty()) {
            return;
        }
        for (Pair<JButton, JButton> association : dependenciesAssociations) {
            ViewUtils.drawArrow(graphics2D, association.getKey(), association.getValue());
        }
    }

    public void setRepository(GHRepositoryWrapper repository) {
        ArtifactoryUtils.getDependencies(repository);
        Map<Integer, List<Dependency>> dependenciesByLevel = new HashMap<>();
        //TODO get this from the repo
        Dependency mvpDependency = new Dependency("MVP", "");

        Dependency thumborUtilsDependency = new Dependency("Thumbor Utils", "");
        Dependency commonsDependency = new Dependency("Commons", "", Arrays.asList(mvpDependency));

        Dependency uiDependency = new Dependency("UI", "https://github.com/theappraisallane/ui-android", Arrays.asList(thumborUtilsDependency, commonsDependency));
        Dependency uploadServiceDependency = new Dependency("Upload Service", "", Arrays.asList(commonsDependency));

        Dependency rootDependency = new Dependency(repository.getName(), repository.getUrl(), Arrays.asList(uiDependency, uploadServiceDependency));

        List<Dependency> dependenciesList = new ArrayList<>();
        dependenciesList.add(rootDependency);
        dependenciesByLevel.put(0, dependenciesList);

        dependenciesList = new ArrayList<>();
        dependenciesList.add(uiDependency);
        dependenciesList.add(uploadServiceDependency);
        dependenciesByLevel.put(1, dependenciesList);

        dependenciesList = new ArrayList<>();
        dependenciesList.add(commonsDependency);
        dependenciesList.add(thumborUtilsDependency);
        dependenciesByLevel.put(2, dependenciesList);

        dependenciesList = new ArrayList<>();
        dependenciesList.add(mvpDependency);
        dependenciesByLevel.put(3, dependenciesList);
        //TODO get this from the repo

        int maxWidth = 1;
        for (List<Dependency> level : dependenciesByLevel.values()) {
            maxWidth = Math.max(maxWidth, level.size());
        }

        int levels = dependenciesByLevel.keySet().size();

        removeAll();
        setLayout(new GridLayoutManager(levels + 3 * (levels - 1), maxWidth + (maxWidth - 1)));

        dependenciesAssociations.clear();
        // Bottom up building
        for (int currentLevel = levels - 1; currentLevel >= 0; currentLevel--) {
            List<Dependency> levelDependencies = dependenciesByLevel.get(currentLevel);
            int dependenciesInLevel = levelDependencies.size();
            int levelOffset = maxWidth - dependenciesInLevel;
            for (int j = 0; j < dependenciesInLevel; j++) {
                Dependency dependency = levelDependencies.get(j);
                GridConstraints gridConstraints = new GridConstraints();
                gridConstraints.setRow(4 * currentLevel);
                gridConstraints.setColumn(levelOffset + 2 * j);
                JButton dependencyWidget = new JButton(dependency.getName());
                add(dependencyWidget, gridConstraints);
                dependency.setWidget(dependencyWidget);
                for (Dependency moduleDependency : dependency.getDependsOn()) {
                    dependenciesAssociations.add(new Pair<>(dependencyWidget, moduleDependency.getWidget()));
                }
            }
        }
    }

    public void clear() {
        dependenciesAssociations.clear();
        removeAll();
    }
}
