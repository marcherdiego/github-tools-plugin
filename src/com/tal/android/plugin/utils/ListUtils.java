package com.tal.android.plugin.utils;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

public class ListUtils {
    public static boolean isEmpty(List ghReleases) {
        return ghReleases == null || ghReleases.isEmpty();
    }

    public static <T> List<T> filter(@Nullable final List<T> list, @NotNull final Criteria<T> criteria) {
        if (isEmpty(list)) {
            return list;
        }
        List<T> resultList = new LinkedList<>();
        for (T item : list) {
            if (criteria.matches(item)) {
                resultList.add(item);
            }
        }
        return resultList;
    }

    public interface Criteria<T> {
        boolean matches(T elem);
    }
}
