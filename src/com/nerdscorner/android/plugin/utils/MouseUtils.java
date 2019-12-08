package com.nerdscorner.android.plugin.utils;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class MouseUtils {

    public abstract static class DoubleClickAdapter extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                onDoubleClick();
            }
        }

        public abstract void onDoubleClick();
    }
}
