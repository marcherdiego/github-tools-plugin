package com.nerdscorner.android.plugin.utils

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

object JTableUtils {

    abstract class SimpleMouseAdapter : MouseAdapter() {
        override fun mousePressed(mouseEvent: MouseEvent) {
            val row = (mouseEvent.source as JTable).rowAtPoint(mouseEvent.point)
            val column = (mouseEvent.source as JTable).columnAtPoint(mouseEvent.point)
            if (row == -1 || column == -1) {
                return
            }
            mousePressed(row, column, mouseEvent.clickCount)
        }

        abstract fun mousePressed(row: Int, column: Int, clickCount: Int)
    }

    abstract class SimpleDoubleClickAdapter : SimpleMouseAdapter() {
        override fun mousePressed(row: Int, column: Int, clickCount: Int) {
            if (clickCount % 2 == 0) {
                onDoubleClick(row, column)
            }
        }

        abstract fun onDoubleClick(row: Int, column: Int)
    }

    fun findAndSelectDefaultRepo(targetRepo: String?, table: JTable): Triple<GHRepositoryWrapper, Int, Int>? {
        if (targetRepo != null) {
            for (i in 0 until table.rowCount) {
                val currentRepo = table.getValueAt(i, 0) as GHRepositoryWrapper
                if (targetRepo == currentRepo.toString()) {
                    table.setRowSelectionInterval(i, i)
                    table.scrollRectToVisible(table.getCellRect(i, 0, true))
                    return Triple(currentRepo, i, 0)
                }
            }
        }
        return null
    }

    fun centerColumns(table: JTable, vararg columns: Int) {
        val columnRenderer = object : DefaultTableCellRenderer() {}.apply {
            horizontalAlignment = JLabel.CENTER
        }
        columns.forEach { column ->
            try {
                table
                        .columnModel
                        .getColumn(column)
                        .cellRenderer = columnRenderer
            } catch (e: Exception) {
                // Nothing to do here
            }
        }
    }

    fun getSelectedItem(table: JTable): Any? {
        val row = table.selectedRow
        val column = table.selectedColumn
        if (row == -1 || column == -1) {
            return null
        }
        return table.getValueAt(row, column)
    }
}
