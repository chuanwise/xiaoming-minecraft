package com.chuanwise.xiaoming.minecraft.util;

import java.util.Objects;

public class StringUtils {
    public static boolean isEmpty(String string) {
        return Objects.isNull(string) || string.isEmpty();
    }

    public static StringBuilder replaceAll(StringBuilder stringBuilder, String from, String to) {
        int position = stringBuilder.indexOf(from);
        while (position != -1) {
            stringBuilder.replace(position, position + from.length(), to);
            position = stringBuilder.indexOf(from, position + to.length());
        }
        return stringBuilder;
    }
}
