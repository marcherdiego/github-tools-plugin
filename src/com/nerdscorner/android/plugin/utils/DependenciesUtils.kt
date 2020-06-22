package com.nerdscorner.android.plugin.utils

import com.nerdscorner.android.plugin.github.domain.Dependency
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.util.ArrayList
import javax.xml.parsers.DocumentBuilderFactory

object DependenciesUtils {
    private const val DEPENDENCIES_XML = "/dependencies.xml"

    private const val DEPENDENCY = "dependency"
    private val COMMA_REGEX = ",".toRegex()

    private const val ID = "id"
    private const val NAME = "name"
    private const val LEVEL = "level"
    private const val URL = "url"
    private const val DEPENDS_ON = "depends_on"

    /**
     * Builds a map to create the dependencies tree
     *
     * @param repositoryWrapper the repository to find dependencies from
     *
     * @return a [Map] containing the dependencies with its level in the dependencies tree
     */
    fun getDependencies(repositoryWrapper: GHRepositoryWrapper): Map<Int, List<Dependency>>? {
        try {
            val dependenciesMap = mutableMapOf<Int, MutableList<Dependency>>()
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val repository = repositoryWrapper.ghRepository
            // Try to fetch the dependencies.xml file from the repository
            val document = builder.parse(
                    repository
                            .getFileContent(DEPENDENCIES_XML)
                            .read()
            )

            // Find all dependency nodes
            val nodes = document.documentElement.getElementsByTagName(DEPENDENCY)
            val dependencies = ArrayList<Dependency>()
            val levelOneDependencies = ArrayList<Dependency>()
            for (temp in 0 until nodes.length) {
                val node = nodes.item(temp)
                if (node.nodeType == Node.ELEMENT_NODE) {
                    // Get dependency metadata
                    val element = node as Element
                    val dependencyId = element.getAttribute(ID)
                    val dependencyName = element.getAttribute(NAME)
                    val dependencyLevel = Integer.valueOf(element.getAttribute(LEVEL))
                    val dependencyUrl = element.getAttribute(URL)
                    val dependencyDependencies = element.getAttribute(DEPENDS_ON)

                    // Build the dependency object
                    val dependency = Dependency(
                            dependencyId,
                            dependencyName,
                            dependencyLevel,
                            dependencyUrl,
                            dependencyDependencies
                    )
                    dependencies.add(dependency)

                    // Save first level dependencies apart because we need the root to be linked with them later
                    if (dependencyLevel == 1) {
                        levelOneDependencies.add(dependency)
                    }
                }
            }

            // Add the root dependency (this repository) with the level one dependencies as children
            dependencies.add(
                    Dependency(
                            repository.fullName,
                            repository.name,
                            0,
                            repository.htmlUrl.toString(),
                            levelOneDependencies
                    )
            )

            dependencies.forEach { dependency ->
                dependenciesMap.computeIfAbsent(dependency.level) { ArrayList() }
                dependency.dependsOn = getDependenciesById(dependency, dependencies)
                dependenciesMap[dependency.level]?.add(dependency)
            }
            return dependenciesMap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

    }

    private fun getDependenciesById(dependency: Dependency, dependencies: List<Dependency>): List<Dependency>? {
        if (dependency.dependsOnIds == null) {
            return dependency.dependsOn
        }
        val result = ArrayList<Dependency>()
        dependency
                .dependsOnIds
                ?.split(COMMA_REGEX)
                ?.dropLastWhile {
                    it.isEmpty()
                }
                ?.toTypedArray()
                ?.let { dependsOn ->
                    dependencies.forEach { dep ->
                        for (dependsOnItem in dependsOn) {
                            if (dep.id == dependsOnItem) {
                                result.add(dep)
                                break
                            }
                        }
                    }
                }
        return result
    }
}
