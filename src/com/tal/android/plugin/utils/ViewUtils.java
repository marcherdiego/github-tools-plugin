package com.tal.android.plugin.utils;

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
}
