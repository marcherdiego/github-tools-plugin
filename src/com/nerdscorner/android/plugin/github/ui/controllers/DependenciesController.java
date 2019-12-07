package com.nerdscorner.android.plugin.github.ui.controllers;

import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHOrganization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.intellij.uiDesigner.core.GridConstraints;
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper;
import com.nerdscorner.android.plugin.github.ui.dependencies.DependenciesPanel;
import com.nerdscorner.android.plugin.github.ui.tablemodels.BaseModel;
import com.nerdscorner.android.plugin.github.ui.tablemodels.GHRepoTableModel;
import com.nerdscorner.android.plugin.github.ui.tables.ColumnRenderer;
import com.nerdscorner.android.plugin.utils.GithubUtils;
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleMouseAdapter;
import com.nerdscorner.android.plugin.utils.Strings;


import static com.nerdscorner.android.plugin.github.ui.controllers.BaseRepoListController.LARGE_PAGE_SIZE;

public class DependenciesController {
    private Thread loaderThread;
    private final JTable reposTable;
    private final DependenciesPanel dependenciesPanel;
    private GHOrganization ghOrganization;
    private GHMyself myselfGitHub;

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
        cancelThread(loaderThread);
        dependenciesPanel.clear();
    }

    public void init() {
        reposTable.addMouseListener(new SimpleMouseAdapter() {
            public void mousePressed(int row, int column, int clickCount) {
                GHRepositoryWrapper currentRepository = (GHRepositoryWrapper) reposTable.getValueAt(row, GHRepoTableModel.COLUMN_NAME);
                if (clickCount == 1) {
                    dependenciesPanel.setRepository(currentRepository);
                    dependenciesPanel.repaint();
                } else if (clickCount == 2) {
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
        cancelThread(loaderThread);
        loaderThread = new Thread(() -> {
            final GHRepoTableModel reposTableModel = new GHRepoTableModel(new ArrayList<>(), new String[]{Strings.NAME});
            reposTable.setModel(reposTableModel);
            TableColumn column = reposTable.getColumn(Strings.NAME);
            final Map<String, String> tooltips = new HashMap<>();
            column.setCellRenderer(new ColumnRenderer(tooltips));
            TableRowSorter<TableModel> sorter = new TableRowSorter<>(reposTable.getModel());
            List<SortKey> sortKeys = new ArrayList<>();
            sortKeys.add(new SortKey(0, SortOrder.ASCENDING));
            sorter.setSortKeys(sortKeys);
            reposTable.setRowSorter(sorter);
            ghOrganization
                    .listRepositories()
                    .withPageSize(LARGE_PAGE_SIZE)
                    .forEach(repository -> {
                        if (repository.isFork() || !repository.getFullName().startsWith(ghOrganization.getLogin())) {
                            //Ignore forks or projects that doesn't belong to this organization
                            return;
                        }
                        GHRepositoryWrapper ghRepositoryWrapper = new GHRepositoryWrapper(repository);
                        SwingUtilities.invokeLater(() -> reposTableModel.addRow(ghRepositoryWrapper));
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
                        SwingUtilities.invokeLater(() -> reposTableModel.addRow(ghRepositoryWrapper));
                    });
        });
        loaderThread.start();
    }

    private void cancelThread(Thread thread) {
        if (thread != null) {
            thread.interrupt();
        }
    }
}
