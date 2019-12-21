package com.nerdscorner.android.plugin.utils

import java.awt.Component
import java.awt.Graphics2D
import java.awt.Point

import javax.swing.JComponent
import kotlin.math.sqrt

object ViewUtils {
    fun hide(vararg views: JComponent) {
        if (views.isNullOrEmpty()) {
            return
        }
        for (view in views) {
            view.isVisible = false
        }
    }

    fun show(vararg views: JComponent) {
        if (views.isNullOrEmpty()) {
            return
        }
        for (view in views) {
            view.isVisible = true
        }
    }

    fun drawArrow(graphics2D: Graphics2D, from: Component, to: Component) {
        val xFrom = from.x + from.width / 2
        val yFrom = from.y + from.height
        val fromPt = Point(xFrom, yFrom)

        val xTo = to.x + to.width / 2
        val yTo = to.y
        val toPt = Point(xTo, yTo)
        drawArrowLine(graphics2D, fromPt.x, fromPt.y, toPt.x, toPt.y, 7, 5)
    }

    private fun drawArrowLine(graphics2D: Graphics2D, x1: Int, y1: Int, x2: Int, y2: Int, d: Int, h: Int) {
        val dx = x2 - x1
        val dy = y2 - y1
        val D = sqrt((dx * dx + dy * dy).toDouble())
        var xm = D - d
        var xn = xm
        var ym = h.toDouble()
        var yn = (-h).toDouble()
        var x: Double
        val sin = dy / D
        val cos = dx / D

        x = xm * cos - ym * sin + x1
        ym = xm * sin + ym * cos + y1.toDouble()
        xm = x

        x = xn * cos - yn * sin + x1
        yn = xn * sin + yn * cos + y1.toDouble()
        xn = x

        val xpoints = intArrayOf(x2, xm.toInt(), xn.toInt())
        val ypoints = intArrayOf(y2, ym.toInt(), yn.toInt())

        graphics2D.drawLine(x1, y1, x2, y2)
        graphics2D.fillPolygon(xpoints, ypoints, 3)
    }
}
