package com.nerdscorner.android.plugin.utils;

import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.model.impl.RepositoryTypeImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper;

public class ArtifactoryUtils {
    public static void getDependencies(GHRepositoryWrapper repositoryWrapper) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(
                    repositoryWrapper
                            .getGhRepository()
                            .getFileContent("/dependencies.xml")
                            .read()
            );
            Element root = document.getDocumentElement();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Artifactory artifactory = ArtifactoryClientBuilder.create()
                .setUrl("http://artifactory.tal-devops.com/artifactory/tal")
                .setUsername("admin")
                .setPassword("Studio42")
                .build();
        artifactory
                .repositories()
                .list(RepositoryTypeImpl.LOCAL)
                .forEach((repo) -> System.out.println(repo.getDescription()));
        artifactory
                .repositories()
                .list(RepositoryTypeImpl.REMOTE)
                .forEach((repo) -> System.out.println(repo.getDescription()));
        artifactory
                .repositories()
                .list(RepositoryTypeImpl.VIRTUAL)
                .forEach((repo) -> System.out.println(repo.getDescription()));
//        Repository repo = artifactory
//                .repository(repoName)
//                .get();
//        repo.getDescription();
    }
}
