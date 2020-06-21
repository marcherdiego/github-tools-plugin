package com.nerdscorner.android.plugin.utils

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JTable

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

    fun findAndSelectDefaultRepo(targetRepo: String?, table: JTable): GHRepositoryWrapper? {
        if (targetRepo != null) {
            for (i in 0 until table.rowCount) {
                val currentRepo = table.getValueAt(i, 0) as GHRepositoryWrapper
                if (targetRepo == currentRepo.toString()) {
                    table.setRowSelectionInterval(i, i)
                    table.scrollRectToVisible(table.getCellRect(i, 0, true))
                    return currentRepo
                }
            }
        }
        return null
    }
}
