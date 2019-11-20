package com.nerdscorner.android.plugin.utils;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTable;

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
}
