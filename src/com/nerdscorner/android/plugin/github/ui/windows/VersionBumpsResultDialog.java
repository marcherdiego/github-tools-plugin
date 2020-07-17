package com.nerdscorner.android.plugin.github.ui.windows;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper;
import com.nerdscorner.android.plugin.github.ui.tablemodels.VersionBumpsTableModel;
import com.nerdscorner.android.plugin.utils.BrowserUtils;
import com.nerdscorner.android.plugin.utils.JTableUtils;
import com.nerdscorner.android.plugin.utils.JTableUtils.SimpleDoubleClickAdapter;
import com.nerdscorner.android.plugin.utils.Strings;

public class VersionBumpsResultDialog extends JDialog {
    private JPanel contentPane;
    private JTable reposTable;
    private JButton openAllPRsButton;
    private JButton closeButton;

    public VersionBumpsResultDialog() {
        setContentPane(contentPane);
        setModal(true);

        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    dispose();
                }
            }
        });
        openAllPRsButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int rows = reposTable.getRowCount();
                    for (int i = 0; i < rows; i++) {
                        GHRepositoryWrapper repo = ((VersionBumpsTableModel) reposTable.getModel()).getRow(i);
                        if (repo == null) {
                            continue;
                        }
                        BrowserUtils.INSTANCE.openWebLink(repo.getVersionBumpPullRequestUrl());
                    }
                }
            }
        });
    }

    public VersionBumpsResultDialog setReposTableModel(VersionBumpsTableModel model) {
        reposTable.setModel(model);
        JTableUtils.INSTANCE.centerColumns(
                reposTable,
                VersionBumpsTableModel.COLUMN_NAME,
                VersionBumpsTableModel.COLUMN_PREVIOUS_VERSION,
                VersionBumpsTableModel.COLUMN_NEXT_VERSION,
                VersionBumpsTableModel.COLUMN_STATUS,
                VersionBumpsTableModel.COLUMN_PULL_REQUEST
        );
        reposTable.addMouseListener(new SimpleDoubleClickAdapter() {
            @Override
            public void onDoubleClick(int row, int column) {
                GHRepositoryWrapper repo = ((VersionBumpsTableModel) reposTable.getModel()).getRow(row);
                if (repo == null) {
                    return;
                }
                switch (column) {
                    case VersionBumpsTableModel.COLUMN_NAME:
                        BrowserUtils.INSTANCE.openWebLink(repo.getFullUrl());
                        break;
                    case VersionBumpsTableModel.COLUMN_STATUS:
                        if (repo.getBumpErrorMessage() != null) {
                            showResultDialog(repo);
                        }
                        break;
                    case VersionBumpsTableModel.COLUMN_PULL_REQUEST:
                        BrowserUtils.INSTANCE.openWebLink(repo.getVersionBumpPullRequestUrl());
                        break;
                }
            }
        });
        reposTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return this;
    }

    private void showResultDialog(GHRepositoryWrapper repository) {
        setVisible(false);

        String message = repository.getName() + " not bumped because: " + repository.getBumpErrorMessage();
        ResultDialog resultDialog = new ResultDialog(message);
        resultDialog.pack();
        resultDialog.setLocationRelativeTo(null);
        resultDialog.setTitle(Strings.ERROR_DETAILS);
        resultDialog.setResizable(false);
        resultDialog.setVisible(true);

        setVisible(true);
    }
}
