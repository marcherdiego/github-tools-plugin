package com.nerdscorner.android.plugin.github.ui.dependencies

import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.utils.DependenciesUtils
import com.nerdscorner.android.plugin.utils.GithubUtils
import com.nerdscorner.android.plugin.utils.MouseUtils.DoubleClickAdapter
import com.nerdscorner.android.plugin.utils.ThreadUtils
import com.nerdscorner.android.plugin.utils.ViewUtils
import javafx.util.Pair
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.util.ArrayList
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.max

class DependenciesPanel : JPanel() {

    private var loaderThread: Thread? = null
    private val dependenciesAssociations = ArrayList<Pair<JButton, JButton>>()

    override fun paint(g: Graphics?) {
        super.paint(g)
        if (dependenciesAssociations.isEmpty()) {
            return
        }
        (g as? Graphics2D?)?.let { graphics2D ->
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            for (association in dependenciesAssociations) {
                ViewUtils.drawArrow(graphics2D, association.key, association.value)
            }
        }
    }

    fun setRepository(repository: GHRepositoryWrapper) {
        ThreadUtils.cancelThread(loaderThread)
        clear()
        layout = FlowLayout()
        add(JLabel("Building dependencies tree.."))
        repaint()
        loaderThread = Thread {
            val dependenciesByLevel = DependenciesUtils.getDependencies(repository)
            clear()
            if (dependenciesByLevel == null) {
                // No dependencies
                add(JLabel("This repo has no dependencies defined in its dependencies.xml file"))
            } else {
                // Dependencies found

                // Get tree width
                var maxWidth = 1
                for (level in dependenciesByLevel.values) {
                    maxWidth = max(maxWidth, level.size)
                }

                // Get tree levels
                val levels = dependenciesByLevel.keys.size

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
                layout = GridLayoutManager(levels + 3 * (levels - 1), maxWidth + (maxWidth - 1))

                // Bottom up building
                for (currentLevel in levels - 1 downTo 0) {
                    val levelDependencies = dependenciesByLevel[currentLevel] ?: break
                    val dependenciesInLevel = levelDependencies.size
                    val levelOffset = maxWidth - dependenciesInLevel
                    for (j in 0 until dependenciesInLevel) {
                        val dependency = levelDependencies[j]
                        val gridConstraints = GridConstraints()
                        gridConstraints.row = 4 * currentLevel
                        gridConstraints.column = levelOffset + 2 * j
                        val dependencyWidget = JButton(dependency.name)
                        add(dependencyWidget, gridConstraints)
                        dependency.widget = dependencyWidget

                        /** Map that associates dependency widgets with each other to draw the arrows
                         * in [DependenciesPanel.paint]
                         */
                        dependency.dependsOn?.let { dependsOn ->
                            for (moduleDependency in dependsOn) {
                                moduleDependency.widget?.let { widget ->
                                    dependenciesAssociations.add(Pair(dependencyWidget, widget))
                                }
                            }
                        }

                        // On dependency double click, open github repository
                        dependencyWidget.addMouseListener(object : DoubleClickAdapter() {
                            override fun onDoubleClick() {
                                GithubUtils.openWebLink(dependency.url)
                            }
                        })
                    }
                }
            }
            SwingUtilities.invokeLater { this.repaint() }
        }
        loaderThread?.start()
    }

    fun clear() {
        dependenciesAssociations.clear()
        removeAll()
        repaint()
    }
}
