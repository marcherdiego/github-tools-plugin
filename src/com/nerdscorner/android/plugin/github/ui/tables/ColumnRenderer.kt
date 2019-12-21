package com.nerdscorner.android.plugin.github.ui.tables

import java.awt.Component

import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

class ColumnRenderer(private val tooltips: Map<String, String>) : DefaultTableCellRenderer() {

    override fun getTableCellRendererComponent(table: JTable?, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        val columnLabel = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
        columnLabel.toolTipText = tooltips[columnLabel.text]
        return columnLabel
    }
}
