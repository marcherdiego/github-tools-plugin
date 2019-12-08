package com.nerdscorner.android.plugin.github.ui.dependencies;

import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.nerdscorner.android.plugin.github.domain.Dependency;
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper;
import com.nerdscorner.android.plugin.utils.DependenciesUtils;
import com.nerdscorner.android.plugin.utils.GithubUtils;
import com.nerdscorner.android.plugin.utils.MouseUtils.DoubleClickAdapter;
import com.nerdscorner.android.plugin.utils.ThreadUtils;
import com.nerdscorner.android.plugin.utils.ViewUtils;
import javafx.util.Pair;

public class DependenciesPanel extends JPanel {

    private Thread loaderThread;
    private List<Pair<JButton, JButton>> dependenciesAssociations = new ArrayList<>();

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (dependenciesAssociations.isEmpty()) {
            return;
        }
        Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (Pair<JButton, JButton> association : dependenciesAssociations) {
            ViewUtils.drawArrow(graphics2D, association.getKey(), association.getValue());
        }
    }

    public void setRepository(GHRepositoryWrapper repository) {
        ThreadUtils.cancelThread(loaderThread);
        clear();
        setLayout(new FlowLayout());
        add(new JLabel("Building dependencies tree.."));
        repaint();
        loaderThread = new Thread(() -> {
            Map<Integer, List<Dependency>> dependenciesByLevel = DependenciesUtils.getDependencies(repository);
            clear();
            if (dependenciesByLevel == null) {
                // No dependencies
                add(new JLabel("This repo has no dependencies defined in its dependencies.xml file"));
            } else {
                // Dependencies found

                // Get tree width
                int maxWidth = 1;
                for (List<Dependency> level : dependenciesByLevel.values()) {
                    maxWidth = Math.max(maxWidth, level.size());
                }

                // Get tree levels
                int levels = dependenciesByLevel.keySet().size();

                // Rows and columns adjustment so spacing is clearer
                // The idea is to build a tree with one space between each dependency and three space between each level
                //                       |Dependency|
                //                        /        \                              |
                //                       /          \                           <-| 3 Spaces
                //                      /            \                            |
                //            |Dependency|          |Dependency|
                //             /        \            /        \                   |
                //            /          \          /          \                <-| 3 Spaces
                //           /            \        /            \                 |
                // |Dependency|          |Dependency|          |Dependency|
                //                            |                                   |
                //                            |                                 <-| 3 Spaces
                //                            |                                   |
                //                       |Dependency|
                setLayout(new GridLayoutManager(levels + 3 * (levels - 1), maxWidth + (maxWidth - 1)));

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

                        /** Map that associates dependency widgets with each other to draw the arrows
                         * in {@link DependenciesPanel#paint(Graphics)}
                         */
                        for (Dependency moduleDependency : dependency.getDependsOn()) {
                            dependenciesAssociations.add(new Pair<>(dependencyWidget, moduleDependency.getWidget()));
                        }

                        // On dependency double click, open github repository
                        dependencyWidget.addMouseListener(new DoubleClickAdapter() {
                            @Override
                            public void onDoubleClick() {
                                GithubUtils.openWebLink(URI.create(dependency.getUrl()));
                            }
                        });
                    }
                }
            }
            SwingUtilities.invokeLater(this::repaint);
        });
        loaderThread.start();
    }

    public void clear() {
        dependenciesAssociations.clear();
        removeAll();
        repaint();
    }
}
