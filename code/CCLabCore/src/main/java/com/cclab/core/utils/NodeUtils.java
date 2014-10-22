package com.cclab.core.utils;

/**
 * Created by ane on 10/22/14.
 */
public class NodeUtils {
    public static String join(Object[] parts, int skip, String delimiter) {
        StringBuilder ret = new StringBuilder();
        for (int i = skip - 1; i < parts.length - 1; i++)
            ret.append(parts[i]).append(delimiter);
        ret.append(parts[parts.length - 1]);
        return ret.toString();
    }

    public static String join(Object[] parts, String delimiter) {
        return join(parts, 0, delimiter);
    }
}
