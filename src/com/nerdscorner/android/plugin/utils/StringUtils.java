package com.nerdscorner.android.plugin.utils;

public class StringUtils {
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.equals(Strings.BLANK);
    }
}
