package com.nerdscorner.android.plugin.utils;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTable;

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper;

public final class JTableUtils {
    private JTableUtils() {
    }

    public static abstract class SimpleMouseAdapter extends MouseAdapter {
        @Override
        public final void mousePressed(MouseEvent mouseEvent) {
            int row = ((JTable) mouseEvent.getSource()).rowAtPoint(mouseEvent.getPoint());
            int column = ((JTable) mouseEvent.getSource()).columnAtPoint(mouseEvent.getPoint());
            if (row == -1 || column == -1) {
                return;
            }
            mousePressed(row, column, mouseEvent.getClickCount());
        }

        public abstract void mousePressed(int row, int column, int clickCount);
    }

    public static void findAndSelectDefaultRepo(String targetRepo, JTable table) {
        if (targetRepo != null) {
            for (int i = 0; i < table.getRowCount(); i++) {
                GHRepositoryWrapper currentRepo = (GHRepositoryWrapper) table.getValueAt(i, 0);
                if (currentRepo != null && targetRepo.equals(currentRepo.toString())) {
                    table.setRowSelectionInterval(i, i);
                    table.scrollRectToVisible(table.getCellRect(i, 0, true));
                    return;
                }
            }
        }
    }
}
