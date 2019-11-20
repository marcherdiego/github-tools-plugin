package com.nerdscorner.android.plugin.utils;

import java.util.List;

public class ListUtils {
    public static boolean isEmpty(List list) {
        return list == null || list.isEmpty();
    }

    public static boolean isEmpty(Object... items) {
        return items == null || items.length == 0;
    }
}
