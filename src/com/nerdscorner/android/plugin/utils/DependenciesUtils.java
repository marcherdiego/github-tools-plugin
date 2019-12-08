package com.nerdscorner.android.plugin.utils;

import org.kohsuke.github.GHRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.nerdscorner.android.plugin.github.domain.Dependency;
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper;

public class DependenciesUtils {

    /**
     * Builds a map to create the dependencies tree
     *
     * @param repositoryWrapper the repository to find dependencies from
     *
     * @return a {@link Map} containing the dependencies with its level in the dependencies tree
     */
    @Nullable
    public static Map<Integer, List<Dependency>> getDependencies(GHRepositoryWrapper repositoryWrapper) {
        try {
            Map<Integer, List<Dependency>> dependenciesMap = new HashMap<>();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            GHRepository repository = repositoryWrapper.getGhRepository();
            // Try to fetch the dependencies.xml file from the repository
            Document document = builder.parse(
                    repository
                            .getFileContent("/dependencies.xml")
                            .read()
            );

            // Find all dependency nodes
            NodeList nodes = document.getDocumentElement().getElementsByTagName("dependency");
            List<Dependency> dependencies = new ArrayList<>();
            List<Dependency> levelOneDependencies = new ArrayList<>();
            for (int temp = 0; temp < nodes.getLength(); temp++) {
                Node node = nodes.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    // Get dependency metadata
                    Element element = (Element) node;
                    String dependencyId = element.getAttribute("id");
                    String dependencyName = element.getAttribute("name");
                    int dependencyLevel = Integer.valueOf(element.getAttribute("level"));
                    String dependencyUrl = element.getAttribute("url");
                    String dependencyDependencies = element.getAttribute("depends_on");

                    // Build the dependency object
                    Dependency dependency = new Dependency(
                            dependencyId,
                            dependencyName,
                            dependencyLevel,
                            dependencyUrl,
                            dependencyDependencies
                    );
                    dependencies.add(dependency);

                    // Save first level dependencies apart because we need the root to be linked with them later
                    if (dependencyLevel == 1) {
                        levelOneDependencies.add(dependency);
                    }
                }
            }

            // Add the root dependency (this repository) with the level one dependencies as children
            dependencies.add(
                    new Dependency(
                            repository.getFullName(),
                            repository.getName(),
                            0,
                            repository.getHtmlUrl().toString(),
                            levelOneDependencies
                    )
            );

            dependencies.forEach(dependency -> {
                dependenciesMap.computeIfAbsent(dependency.getLevel(), k -> new ArrayList<>());
                dependency.setDependsOn(getDependenciesById(dependency, dependencies));
                dependenciesMap
                        .get(dependency.getLevel())
                        .add(dependency);
            });
            return dependenciesMap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    private static List<Dependency> getDependenciesById(Dependency dependency, List<Dependency> dependencies) {
        if (dependency.getDependsOnIds() == null) {
            return dependency.getDependsOn();
        }
        String[] dependsOn = dependency.getDependsOnIds().split(",");
        List<Dependency> result = new ArrayList<>();
        dependencies.forEach(dep -> {
            for (String dependsOnItem : dependsOn) {
                if (dep.getId().equals(dependsOnItem)) {
                    result.add(dep);
                    break;
                }
            }
        });
        return result;
    }
}
