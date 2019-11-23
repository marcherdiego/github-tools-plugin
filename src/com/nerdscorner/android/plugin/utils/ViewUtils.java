package com.nerdscorner.android.plugin.utils;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;

import javax.swing.JComponent;

public final class ViewUtils {
    public static void hide(JComponent... views) {
        if (ListUtils.isEmpty(views)) {
            return;
        }
        for (JComponent view : views) {
            view.setVisible(false);
        }
    }

    public static void show(JComponent... views) {
        if (ListUtils.isEmpty(views)) {
            return;
        }
        for (JComponent view : views) {
            view.setVisible(true);
        }
    }

    public static void drawArrow(Graphics2D graphics2D, Component from, Component to) {
        int xFrom = from.getX() + from.getWidth() / 2;
        int yFrom = from.getY() + from.getHeight();
        Point fromPt = new Point(xFrom, yFrom);

        int xTo = to.getX() + to.getWidth() / 2;
        int yTo = to.getY();
        Point toPt = new Point(xTo, yTo);
        drawArrowLine(graphics2D, fromPt.x, fromPt.y, toPt.x, toPt.y, 7, 5);
    }

    private static void drawArrowLine(Graphics2D graphics2D, int x1, int y1, int x2, int y2, int d, int h) {
        int dx = x2 - x1, dy = y2 - y1;
        double D = Math.sqrt(dx * dx + dy * dy);
        double xm = D - d, xn = xm, ym = h, yn = -h, x;
        double sin = dy / D, cos = dx / D;

        x = xm * cos - ym * sin + x1;
        ym = xm * sin + ym * cos + y1;
        xm = x;

        x = xn * cos - yn * sin + x1;
        yn = xn * sin + yn * cos + y1;
        xn = x;

        int[] xpoints = {x2, (int) xm, (int) xn};
        int[] ypoints = {y2, (int) ym, (int) yn};

        graphics2D.drawLine(x1, y1, x2, y2);
        graphics2D.fillPolygon(xpoints, ypoints, 3);
    }
}
