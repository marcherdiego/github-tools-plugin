package com.nerdscorner.android.plugin.utils;

public class ThreadUtils {
    public static void cancelThread(Thread thread) {
        if (thread != null) {
            thread.interrupt();
        }
    }
}
