package com.nerdscorner.android.plugin.github.ui.windows;

import org.apache.commons.lang.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;

import javax.swing.*;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.nerdscorner.android.plugin.github.events.ParameterUpdatedEvent;
import com.nerdscorner.android.plugin.github.managers.GitHubManager;
import com.nerdscorner.android.plugin.github.ui.model.AllReposModel;
import com.nerdscorner.android.plugin.github.ui.model.DeploymentModel;
import com.nerdscorner.android.plugin.github.ui.model.MyReposModel;
import com.nerdscorner.android.plugin.github.ui.model.ParametersModel;
import com.nerdscorner.android.plugin.github.ui.presenter.DeploymentPresenter;
import com.nerdscorner.android.plugin.github.ui.presenter.ParametersPresenter;
import com.nerdscorner.android.plugin.github.ui.presenter.RepoListPresenter;
import com.nerdscorner.android.plugin.github.ui.tablemodels.BaseModel;
import com.nerdscorner.android.plugin.github.ui.view.DeploymentView;
import com.nerdscorner.android.plugin.github.ui.view.ParametersView;
import com.nerdscorner.android.plugin.github.ui.view.ReposView;
import com.nerdscorner.android.plugin.utils.Strings;
import com.nerdscorner.android.plugin.utils.ViewUtils;

public class GitHubTool implements ToolWindowFactory {

    private JPanel content;

    private JTable allReposTable;
    private JTable allRepoReleases;
    private JTable allRepoBranches;
    private JTable allRepoOpenPullRequestsTable;
    private JTable allRepoClosedPullRequestsTable;
    private RepoListPresenter allAllReposPresenter;

    private JTable myReposTable;
    private JTable myReposBranchesTable;
    private JTable myReposOpenPrTable;
    private JTable myReposReleasesTable;
    private JTable myReposClosedPrTable;
    private RepoListPresenter myReposPresenter;

    private JButton createAppsChangelogButton;
    private JButton addLibraryButton;
    private JButton removeLibraryButton;
    private JTable excludedRepos;
    private JTable includedRepos;
    private JButton releaseLibrariesButton;
    private JLabel changelogProgress;
    private JButton createVersionBumpsButton;
    private JLabel includedReposLabel;
    private JTextField reviewersTeam;
    private JTextField individualReviewers;
    private JLabel releaseProcessLink;
    private DeploymentPresenter deploymentPresenter;

    private JTextField githubToken;
    private JTextField circleCiToken;
    private JTextField travisToken;
    private JTextField organizationName;
    private JButton saveButton;
    private JLabel parametersMessageLabel;
    private JCheckBox showOrganizationReposOnly;
    private ParametersPresenter parametersPresenter;

    private JPanel loginPanel;
    private JPanel pluginPanel;
    private JTextField organizationField;
    private JTextField oauthTokenField;
    private JButton loginButton;
    private JButton logoutButton;
    private JLabel loggedAsField;
    private JButton reloadViewButton;

    private Project project;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(this.content, Strings.BLANK, false);
        toolWindow.getContentManager().addContent(content);
        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
        String oauthToken = propertiesComponent.getValue(Strings.OAUTH_TOKEN_PROPERTY, Strings.BLANK);
        String organizations = propertiesComponent.getValue(Strings.ORGANIZATION_NAMES_PROPERTY, Strings.BLANK);
        organizationField.setText(organizations);
        if (StringUtils.isEmpty(oauthToken)) {
            ViewUtils.INSTANCE.show(loginPanel);
            ViewUtils.INSTANCE.hide(logoutButton, reloadViewButton, pluginPanel, loggedAsField);
        } else {
            ViewUtils.INSTANCE.show(pluginPanel, logoutButton, loggedAsField, reloadViewButton);
            ViewUtils.INSTANCE.hide(loginPanel);
            githubTokenLogin(oauthToken, organizations);
        }
        loginButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String oauthToken = oauthTokenField.getText();
                if (StringUtils.isEmpty(oauthToken)) {
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
                    String organization = organizationField.getText();
                    boolean success = githubTokenLogin(oauthToken, organization);
                    if (success) {
                        ViewUtils.INSTANCE.show(pluginPanel, logoutButton, loggedAsField, reloadViewButton);
                        ViewUtils.INSTANCE.hide(loginPanel);
                        propertiesComponent.setValue(Strings.OAUTH_TOKEN_PROPERTY, oauthToken);
                        propertiesComponent.setValue(Strings.ORGANIZATION_NAMES_PROPERTY, organization);
                        EventBus.getDefault().post(new ParameterUpdatedEvent());
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
        logoutButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ViewUtils.INSTANCE.show(loginPanel);
                ViewUtils.INSTANCE.hide(pluginPanel, logoutButton, loggedAsField, reloadViewButton);
                propertiesComponent.unsetValue(Strings.OAUTH_TOKEN_PROPERTY);
                propertiesComponent.unsetValue(Strings.TRAVIS_CI_TOKEN_PROPERTY);
                propertiesComponent.unsetValue(Strings.CIRCLE_CI_TOKEN_PROPERTY);
                propertiesComponent.unsetValue(Strings.ORGANIZATION_NAMES_PROPERTY);
                propertiesComponent.unsetValue(Strings.SHOW_REPOS_FROM_ORGANIZATION_ONLY);
                EventBus.getDefault().post(new ParameterUpdatedEvent());
                GitHubManager.clear();
                allAllReposPresenter = null;
                myReposPresenter = null;
                parametersPresenter = null;
                deploymentPresenter = null;
            }
        });

        reloadViewButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
                String oauthToken = propertiesComponent.getValue(Strings.OAUTH_TOKEN_PROPERTY, Strings.BLANK);
                String organizationNames = propertiesComponent.getValue(Strings.ORGANIZATION_NAMES_PROPERTY, Strings.BLANK);
                GitHubManager.clear();
                githubTokenLogin(oauthToken, organizationNames);
            }
        });

        loadParametersPanel();
    }

    private boolean githubTokenLogin(String oauthKey, String organizationNames) {
        try {
            GitHubManager.setup(oauthKey, organizationNames);
            loggedAsField.setVisible(true);
            loggedAsField.setText(String.format(Strings.LOGGED_AS, GitHubManager.getMyLogin(), GitHubManager.getMyName()));
            loadTablesInfo(organizationNames);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void loadParametersPanel() {
        if (parametersPresenter == null) {
            parametersPresenter = new ParametersPresenter(
                    new ParametersView(
                            githubToken,
                            circleCiToken,
                            travisToken,
                            organizationName,
                            showOrganizationReposOnly,
                            saveButton,
                            parametersMessageLabel
                    ),
                    new ParametersModel(),
                    new EventBus()
            );
        }
    }

    private void loadTablesInfo(String organizationNames) {
        if (!GitHubManager.hasOrganizations() && !StringUtils.isEmpty(organizationNames)) {
            final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
            String oauthToken = propertiesComponent.getValue(Strings.OAUTH_TOKEN_PROPERTY, Strings.BLANK);
            String organizations = propertiesComponent.getValue(Strings.ORGANIZATION_NAMES_PROPERTY, Strings.BLANK);
            if (githubTokenLogin(oauthToken, organizations)) {
                return;
            }
        }

        if (allAllReposPresenter == null) {
            allAllReposPresenter = new RepoListPresenter(
                    new ReposView(
                            allReposTable,
                            allRepoReleases,
                            allRepoBranches,
                            allRepoOpenPullRequestsTable,
                            allRepoClosedPullRequestsTable,
                            BaseModel.COLUMN_NAME
                    ),
                    new AllReposModel(project.getName()),
                    new EventBus()
            );
        }
        allAllReposPresenter.init();
        allAllReposPresenter.loadRepositories();

        if (deploymentPresenter == null) {
            deploymentPresenter = new DeploymentPresenter(
                    new DeploymentView(
                            createAppsChangelogButton,
                            changelogProgress,
                            addLibraryButton,
                            removeLibraryButton,
                            excludedRepos,
                            includedReposLabel,
                            includedRepos,
                            releaseLibrariesButton,
                            createVersionBumpsButton,
                            reviewersTeam,
                            individualReviewers,
                            releaseProcessLink
                    ),
                    new DeploymentModel(),
                    new EventBus()
            );
        }
        deploymentPresenter.loadRepositories();

        if (myReposPresenter == null) {
            myReposPresenter = new RepoListPresenter(
                    new ReposView(
                            myReposTable,
                            myReposReleasesTable,
                            myReposBranchesTable,
                            myReposOpenPrTable,
                            myReposClosedPrTable,
                            BaseModel.COLUMN_NAME
                    ),
                    new MyReposModel(project.getName()),
                    new EventBus()
            );
        }
        myReposPresenter.init();
        myReposPresenter.loadRepositories();
    }
}