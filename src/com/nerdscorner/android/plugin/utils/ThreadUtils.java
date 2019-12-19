package com.nerdscorner.android.plugin.utils;

public class ThreadUtils {
    public static void cancelThread(Thread thread) {
        if (thread != null) {
            thread.interrupt();
        }
    }

    public static void cancelThreads(Thread... threads) {
        if (threads == null || threads.length == 0) {
            return;
        }
        for (Thread thread : threads) {
            cancelThread(thread);
        }
    }
}
