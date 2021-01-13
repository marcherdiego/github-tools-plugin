package com.nerdscorner.android.plugin.github.ui.windows;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;

import com.intellij.ide.util.PropertiesComponent;
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper;
import com.nerdscorner.android.plugin.github.ui.tablemodels.ReleaseCandidateTableModel;
import com.nerdscorner.android.plugin.utils.BrowserUtils;
import com.nerdscorner.android.plugin.utils.JTableUtils;
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleDoubleClickAdapter;
import com.nerdscorner.android.plugin.utils.Strings;

public class ReleaseCandidatesResultDialog extends JDialog {
    private String organizationName = PropertiesComponent.getInstance().getValue(Strings.ORGANIZATION_NAMES_PROPERTY);
    private JPanel rootPanel;
    private JTable reposTable;
    private JButton openAllPRsButton;
    private JButton closeButton;
    private JButton openAllReposButton;

    public ReleaseCandidatesResultDialog() {
        setContentPane(rootPanel);
        setModal(true);

        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    dispose();
                }
            }
        });
        openAllReposButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int rows = reposTable.getRowCount();
                    for (int i = 0; i < rows; i++) {
                        GHRepositoryWrapper repo = ((ReleaseCandidateTableModel) reposTable.getModel()).getRow(i);
                        if (repo == null) {
                            continue;
                        }
                        BrowserUtils.INSTANCE.openWebLink(repo.getFullUrl());
                    }
                }
            }
        });
        openAllPRsButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int rows = reposTable.getRowCount();
                    for (int i = 0; i < rows; i++) {
                        GHRepositoryWrapper repo = ((ReleaseCandidateTableModel) reposTable.getModel()).getRow(i);
                        if (repo == null) {
                            continue;
                        }
                        BrowserUtils.INSTANCE.openWebLink(repo.getRcPullRequestUrl());
                    }
                }
            }
        });
    }

    public ReleaseCandidatesResultDialog setReposTableModel(ReleaseCandidateTableModel model) {
        reposTable.setModel(model);
        JTableUtils.INSTANCE.centerColumns(
                reposTable,
                ReleaseCandidateTableModel.COLUMN_NAME,
                ReleaseCandidateTableModel.COLUMN_VERSION,
                ReleaseCandidateTableModel.COLUMN_STATUS,
                ReleaseCandidateTableModel.COLUMN_PULL_REQUEST
        );
        reposTable.addMouseListener(new SimpleDoubleClickAdapter() {
            @Override
            public void onDoubleClick(int row, int column) {
                GHRepositoryWrapper repo = ((ReleaseCandidateTableModel) reposTable.getModel()).getRow(row);
                if (repo == null) {
                    return;
                }
                switch (column) {
                    case ReleaseCandidateTableModel.COLUMN_NAME:
                        BrowserUtils.INSTANCE.openWebLink(repo.getFullUrl());
                        break;
                    case ReleaseCandidateTableModel.COLUMN_VERSION:
                        String repoName = repo.getName();
                        String branchName = "rc-" + repo.getVersion();
                        String rcBranch = "https://github.com/" + organizationName + "/" + repoName + "/tree/" + branchName;
                        BrowserUtils.INSTANCE.openWebLink(rcBranch);
                        break;
                    case ReleaseCandidateTableModel.COLUMN_STATUS:
                        if (repo.getRcCreationErrorMessage() != null) {
                            showResultDialog(repo);
                        }
                        break;
                    case ReleaseCandidateTableModel.COLUMN_PULL_REQUEST:
                        BrowserUtils.INSTANCE.openWebLink(repo.getRcPullRequestUrl());
                        break;
                }
            }
        });
        reposTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return this;
    }

    private void showResultDialog(GHRepositoryWrapper repository) {
        setVisible(false);

        String message = repository.getName() + " not released because: " + repository.getRcCreationErrorMessage();
        ResultDialog resultDialog = new ResultDialog(message);
        resultDialog.pack();
        resultDialog.setLocationRelativeTo(null);
        resultDialog.setTitle(Strings.ERROR_DETAILS);
        resultDialog.setResizable(false);
        resultDialog.setVisible(true);

        setVisible(true);
    }
}
