package com.nerdscorner.android.plugin.github.ui.windows;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GitHub;

import java.awt.event.ActionEvent;

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
import com.nerdscorner.android.plugin.github.ui.controllers.AllReposController;
import com.nerdscorner.android.plugin.github.ui.controllers.DependenciesController;
import com.nerdscorner.android.plugin.github.ui.controllers.MyFavoriteReposController;
import com.nerdscorner.android.plugin.github.ui.controllers.MyReposController;
import com.nerdscorner.android.plugin.utils.Strings;
import com.nerdscorner.android.plugin.utils.ViewUtils;

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

    private JTable allReposDependenciesTable;
    private JPanel dependenciesGraphPanel;
    private DependenciesController dependenciesController;

    private JPanel loginPanel;
    private JPanel pluginPanel;
    private JTextField oauthTokenField;
    private JButton loginButton;
    private JButton logoutButton;
    private JLabel loggedAsField;
    private JButton reloadViewButton;
    private JLabel repoComments;
    private JPanel repoCommentsContainer;
    private JTextField organizationField;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(myToolWindowContent, Strings.BLANK, false);
        toolWindow.getContentManager().addContent(content);
        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
        String oauthToken = propertiesComponent.getValue(Strings.OAUTH_TOKEN_PROPERTY, Strings.BLANK);
        String organization = propertiesComponent.getValue(Strings.ORGANIZATION_NAME_PROPERTY, Strings.BLANK);
        if (StringUtils.isEmpty(oauthToken) || StringUtils.isEmpty(organization)) {
            organizationField.setText(organization);
            ViewUtils.show(loginPanel);
            ViewUtils.hide(logoutButton, reloadViewButton, pluginPanel, loggedAsField, repoCommentsContainer);
            loginButton.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String oauthToken = oauthTokenField.getText();
                    String organization = organizationField.getText();
                    if (StringUtils.isEmpty(oauthToken) || StringUtils.isEmpty(organization)) {
                        String message;
                        if (StringUtils.isEmpty(oauthToken)) {
                            message = Strings.EMPTY_OAUTH_TOKEN;
                        } else {
                            message = Strings.EMPTY_ORGANIZATION_NAME;
                        }
                        ResultDialog resultDialog = new ResultDialog(message);
                        resultDialog.pack();
                        resultDialog.setLocationRelativeTo(null);
                        resultDialog.setTitle(Strings.LOGIN_ERROR);
                        resultDialog.setResizable(false);
                        resultDialog.setVisible(true);
                    } else {
                        boolean success = githubTokenLogin(oauthToken, organization);
                        if (success) {
                            ViewUtils.show(pluginPanel, logoutButton, loggedAsField, reloadViewButton, repoCommentsContainer);
                            ViewUtils.hide(loginPanel);
                            propertiesComponent.setValue(Strings.OAUTH_TOKEN_PROPERTY, oauthToken);
                            propertiesComponent.setValue(Strings.ORGANIZATION_NAME_PROPERTY, organization);
                            oauthTokenField.setText(null);
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
            ViewUtils.show(pluginPanel, logoutButton, loggedAsField, reloadViewButton, repoCommentsContainer);
            ViewUtils.hide(loginPanel);
            githubTokenLogin(oauthToken, organization);
        }

        logoutButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ViewUtils.show(loginPanel);
                ViewUtils.hide(pluginPanel, logoutButton, loggedAsField, reloadViewButton, repoCommentsContainer);
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

    private boolean githubTokenLogin(String oauthKey, String organization) {
        try {
            // Github personal token (https://github.com/settings/tokens)
            github = GitHub.connectUsingOAuth(oauthKey);
            ghOrganization = github.getOrganization(organization);
            myselfGitHub = github.getMyself();
            loggedAsField.setVisible(true);
            loggedAsField.setText(String.format(Strings.LOGGED_AS, myselfGitHub.getLogin(), myselfGitHub.getName()));

            allAllReposController = new AllReposController(
                    allReposTable,
                    allRepoReleases,
                    allRepoPullRequestsTable,
                    repoComments,
                    ghOrganization
            );
            myReposController = new MyReposController(
                    myReposTable,
                    myReposReleasesTable,
                    myReposOpenPrTable,
                    repoComments,
                    myselfGitHub,
                    ghOrganization
            );
            myFavoriteReposController = new MyFavoriteReposController(
                    favoriteRepositoriesTable,
                    favoriteRepositoriesReleasesTable,
                    favoriteRepositoriesOpenPrsTable,
                    repoComments,
                    myselfGitHub,
                    ghOrganization
            );

            dependenciesController = new DependenciesController(
                    allReposDependenciesTable,
                    dependenciesGraphPanel,
                    ghOrganization
            );
            loadTablesInfo();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void loadTablesInfo() {
        repoComments.setText(null);

        allAllReposController.cancel();
        allAllReposController.init();
        allAllReposController.loadRepositories();

        myReposController.cancel();
        myReposController.init();
        myReposController.loadRepositories();

        myFavoriteReposController.cancel();
        myFavoriteReposController.init();
        myFavoriteReposController.loadRepositories();

        dependenciesController.cancel();
        dependenciesController.init();
        dependenciesController.loadRepositories();
    }
}