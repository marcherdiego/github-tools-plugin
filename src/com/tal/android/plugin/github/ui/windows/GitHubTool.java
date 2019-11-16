package com.tal.android.plugin.github.ui.windows;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GitHub;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.tal.android.plugin.github.ui.controllers.AllReposController;
import com.tal.android.plugin.github.ui.controllers.MyFavoriteReposController;
import com.tal.android.plugin.github.ui.controllers.MyReposController;
import com.tal.android.plugin.utils.Strings;

public class GitHubTool implements ToolWindowFactory {

    private GHMyself myselfGitHub;
    private GHOrganization ghOrganization;
    private GitHub github;

    private JPanel myToolWindowContent;

    private JTable allReposTable;
    private JTable allRepoReleases;
    private JTable allRepoPullRequestsTable;
    private AllReposController allAllReposController;

    private JTable myReposTable;
    private JTable myReposOpenPrTable;
    private JTable myReposReleasesTable;
    private MyReposController myReposController;

    private JTable favoriteRepositoriesTable;
    private JTable favoriteRepositoriesReleasesTable;
    private JTable favoriteRepositoriesOpenPrsTable;
    private MyFavoriteReposController myFavoriteReposController;

    private JPanel loginPanel;
    private JPanel pluginPanel;
    private JTextField oauthTokenField;
    private JButton loginButton;
    private JButton logoutButton;
    private JLabel loggedAsField;
    private JButton reloadViewButton;
    private JLabel repoComments;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(myToolWindowContent, Strings.BLANK, false);
        toolWindow.getContentManager().addContent(content);
        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
        String oauthToken = propertiesComponent.getValue(Strings.OAUTH_TOKEN_PROPERTY, Strings.BLANK);

        if (StringUtils.isEmpty(oauthToken)) {
            logoutButton.setVisible(false);
            reloadViewButton.setVisible(false);
            loginPanel.setVisible(true);
            pluginPanel.setVisible(false);
            loggedAsField.setVisible(false);
            loginButton.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String oauthToken = oauthTokenField.getText();
                    if (StringUtils.isEmpty(oauthToken)) {
                        ResultDialog resultDialog = new ResultDialog(Strings.EMPTY_OAUTH_TOKEN);
                        resultDialog.pack();
                        resultDialog.setLocationRelativeTo(null);
                        resultDialog.setTitle(Strings.LOGIN_ERROR);
                        resultDialog.setResizable(false);
                        resultDialog.setVisible(true);
                    } else {
                        boolean success = githubTokenLogin(oauthToken);
                        if (success) {
                            loginPanel.setVisible(false);
                            pluginPanel.setVisible(true);
                            logoutButton.setVisible(true);
                            loggedAsField.setVisible(false);
                            reloadViewButton.setVisible(true);
                            propertiesComponent.setValue(Strings.OAUTH_TOKEN_PROPERTY, oauthToken);
                        } else {
                            ResultDialog resultDialog = new ResultDialog(Strings.VERIFY_OAUTH_TOKEN);
                            resultDialog.pack();
                            resultDialog.setLocationRelativeTo(null);
                            resultDialog.setTitle(Strings.LOGIN_ERROR);
                            resultDialog.setResizable(false);
                            resultDialog.setVisible(true);
                        }
                    }
                }
            });
        } else {
            loginPanel.setVisible(false);
            pluginPanel.setVisible(true);
            logoutButton.setVisible(true);
            loggedAsField.setVisible(true);
            reloadViewButton.setVisible(true);
            githubTokenLogin(oauthToken);
        }

        logoutButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logoutButton.setVisible(false);
                loggedAsField.setVisible(false);
                reloadViewButton.setVisible(false);
                loginPanel.setVisible(true);
                pluginPanel.setVisible(false);

                propertiesComponent.setValue(Strings.OAUTH_TOKEN_PROPERTY, Strings.BLANK);
                github = null;
                ghOrganization = null;
                myselfGitHub = null;
            }
        });

        reloadViewButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadTablesInfo();
            }
        });
    }

    private boolean githubTokenLogin(final String oauthKey) {
        try {
            // Github personal token (https://github.com/settings/tokens)
            github = GitHub.connectUsingOAuth(oauthKey);
            ghOrganization = github.getOrganization(Strings.ORGANIZATION);
            myselfGitHub = github.getMyself();
            loggedAsField.setVisible(true);
            loggedAsField.setText(String.format(Strings.LOGGED_AS, myselfGitHub.getLogin(), myselfGitHub.getName()));

            allAllReposController = new AllReposController(allReposTable, allRepoReleases, allRepoPullRequestsTable, repoComments, ghOrganization);
            myReposController = new MyReposController(myReposTable, myReposReleasesTable, myReposOpenPrTable, repoComments, myselfGitHub);
            myFavoriteReposController = new MyFavoriteReposController(favoriteRepositoriesTable, favoriteRepositoriesReleasesTable, favoriteRepositoriesOpenPrsTable, repoComments, myselfGitHub);
            loadTablesInfo();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void loadTablesInfo() {
        allAllReposController.cancel();
        allAllReposController.init();
        allAllReposController.loadRepositories();

        myReposController.cancel();
        myReposController.init();
        myReposController.loadRepositories();

        myFavoriteReposController.cancel();
        myFavoriteReposController.init();
        myFavoriteReposController.loadRepositories();
    }
}