package com.nerdscorner.android.plugin.github.ui.tablemodels

import com.nerdscorner.android.plugin.github.domain.gh.Wrapper
import java.io.Serializable
import java.util.Comparator
import javax.swing.table.AbstractTableModel

abstract class BaseModel<T : Wrapper> internal constructor(internal val items: MutableList<T>, private val colNames: Array<String>)
    : AbstractTableModel(), Serializable, Comparator<T> {

    private val colsCount: Int = colNames.size

    fun addRow(item: T) {
        val row = items.size
        items.add(item)
        items.sortWith(this)
        fireTableRowsInserted(row, row)
    }

    fun removeAllRows() {
        items.clear()
        fireTableDataChanged()
    }

    override fun getColumnName(column: Int): String {
        return colNames[column]
    }

    override fun getRowCount(): Int {
        return items.size
    }

    override fun getColumnCount(): Int {
        return colsCount
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return false
    }

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return String::class.java
    }

    fun getRow(rowIndex: Int): T? {
        return if (items.isEmpty()) {
            null
        } else items[rowIndex]
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        return if (rowIndex < 0 || rowIndex >= items.size) {
            null
        } else items[rowIndex]
    }

    override fun compare(one: T, other: T): Int {
        return other.compare(one)
    }

    companion object {
        val COLUMN_NAME = 0
    }
}
