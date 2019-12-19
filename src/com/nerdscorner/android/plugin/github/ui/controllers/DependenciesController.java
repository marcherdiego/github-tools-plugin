package com.nerdscorner.android.plugin.github.ui.controllers;

import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHOrganization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import com.intellij.uiDesigner.core.GridConstraints;
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper;
import com.nerdscorner.android.plugin.github.ui.dependencies.DependenciesPanel;
import com.nerdscorner.android.plugin.github.ui.tablemodels.BaseModel;
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel;
import com.nerdscorner.android.plugin.github.ui.tables.ColumnRenderer;
import com.nerdscorner.android.plugin.utils.GithubUtils;
import com.nerdscorner.android.plugin.utils.JTableUtils;
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleMouseAdapter;
import com.nerdscorner.android.plugin.utils.Strings;
import com.nerdscorner.android.plugin.utils.ThreadUtils;


import static com.nerdscorner.android.plugin.github.ui.controllers.BaseRepoListController.LARGE_PAGE_SIZE;

public class DependenciesController {
    private Thread loaderThread;
    private final JTable reposTable;
    private final DependenciesPanel dependenciesPanel;
    private GHOrganization ghOrganization;
    private GHMyself myselfGitHub;
    private String selectedRepo;

    public DependenciesController(JTable reposTable, JPanel dependenciesGraphPanel, GHOrganization ghOrganization, GHMyself myselfGitHub) {
        this.reposTable = reposTable;
        this.ghOrganization = ghOrganization;
        this.myselfGitHub = myselfGitHub;
        this.dependenciesPanel = new DependenciesPanel();
        GridConstraints gridConstraints = new GridConstraints();
        gridConstraints.setRow(0);
        gridConstraints.setColumn(0);
        dependenciesGraphPanel.add(dependenciesPanel, gridConstraints);
    }

    public void cancel() {
        ThreadUtils.cancelThread(loaderThread);
        dependenciesPanel.clear();
    }

    public void init() {
        reposTable.addMouseListener(new SimpleMouseAdapter() {
            public void mousePressed(int row, int column, int clickCount) {
                if (clickCount == 1) {
                    updateRepositoryDependenciesTree();
                } else if (clickCount == 2) {
                    GHRepositoryWrapper currentRepository = (GHRepositoryWrapper) reposTable.getValueAt(row, GHRepoTableModel.COLUMN_NAME);
                    GithubUtils.openWebLink(currentRepository.getFullUrl());
                }
            }
        });
        reposTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        if (reposTable.getModel() instanceof BaseModel<?>) {
            ((BaseModel<?>) reposTable.getModel()).removeAllRows();
        }
    }

    public void loadRepositories() {
        ThreadUtils.cancelThread(loaderThread);
        loaderThread = new Thread(() -> {
            final GHRepoTableModel reposTableModel = new GHRepoTableModel(new ArrayList<>(), new String[]{Strings.NAME});
            reposTable.setModel(reposTableModel);
            TableColumn column = reposTable.getColumn(Strings.NAME);
            final Map<String, String> tooltips = new HashMap<>();
            column.setCellRenderer(new ColumnRenderer(tooltips));
            ghOrganization
                    .listRepositories()
                    .withPageSize(LARGE_PAGE_SIZE)
                    .forEach(repository -> {
                        if (repository.isFork() || !repository.getFullName().startsWith(ghOrganization.getLogin())) {
                            //Ignore forks or projects that doesn't belong to this organization
                            return;
                        }
                        GHRepositoryWrapper ghRepositoryWrapper = new GHRepositoryWrapper(repository);
                        reposTableModel.addRow(ghRepositoryWrapper);
                        tooltips.put(ghRepositoryWrapper.getName(), ghRepositoryWrapper.getDescription());
                    });
            myselfGitHub
                    .listSubscriptions()
                    .withPageSize(LARGE_PAGE_SIZE)
                    .forEach(repository -> {
                        if (repository.isFork()) {
                            //Ignore forks or projects that doesn't belong to this organization
                            return;
                        }
                        GHRepositoryWrapper ghRepositoryWrapper = new GHRepositoryWrapper(repository);
                        reposTableModel.addRow(ghRepositoryWrapper);
                    });
            JTableUtils.findAndSelectDefaultRepo(selectedRepo, reposTable);
            SwingUtilities.invokeLater(this::updateRepositoryDependenciesTree);
        });
        loaderThread.start();
    }

    private void updateRepositoryDependenciesTree() {
        dependenciesPanel.setRepository(
                (GHRepositoryWrapper) reposTable.getValueAt(reposTable.getSelectedRow(), GHRepoTableModel.COLUMN_NAME)
        );
    }

    public void setSelectedRepo(String selectedRepo) {
        this.selectedRepo = selectedRepo;
    }
}
