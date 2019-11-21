package com.nerdscorner.android.plugin.github.ui.tables;

import java.awt.Component;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class ColumnRenderer extends DefaultTableCellRenderer {
    private final Map<String, String> tooltips;

    public ColumnRenderer(Map<String, String> tooltips) {
        this.tooltips = tooltips;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel columnLabel = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        columnLabel.setToolTipText(tooltips.get(columnLabel.getText()));
        return columnLabel;
    }
}
