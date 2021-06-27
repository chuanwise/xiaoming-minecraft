package com.chuanwise.xiaoming.minecraft.util;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgumentUtils {
    public static String getReaminArgs(String[] arguments, int begin) {
        if (arguments.length == 0 || begin >= arguments.length) {
            return "";
        }
        if (begin == arguments.length - 1) {
            return arguments[begin];
        }
        StringBuilder builder = new StringBuilder(arguments[begin]);
        for (int index = begin + 1; index < arguments.length; index ++) {
            builder.append(" ").append(arguments[index]);
        }
        return builder.toString();
    }

    public static String getReaminArgs(List<String> arguments, int begin) {
        return getReaminArgs(arguments.toArray(new String[0]), begin);
    }

    public static String replaceArguments(String format, Object[] arguments) {
        StringBuilder builder = new StringBuilder(format);
        for (Object argument: arguments) {
            int pos = builder.indexOf("{}");
            if (pos != -1) {
                builder.replace(pos, pos + 2, Objects.isNull(argument) ? "null" : argument.toString());
            }
            else {
                break;
            }
        }
        return builder.toString();
    }

    public static final Pattern VARIABLE_REFERENCE = Pattern.compile("\\{(?<identify>[\\S\\s]+?)\\}");
    private static final Random RANDOM = new Random();
    public static String replaceArguments(String format, Map<String, Object> environment, int maxIterateTime) {
        Matcher matcher = VARIABLE_REFERENCE.matcher(format);
        StringBuilder builder = new StringBuilder(format);

        int times = 0;
        int pos = 0;
        while (matcher.find(pos) && times < maxIterateTime) {
            int start = matcher.start();
            int end = matcher.end();
            String identify = matcher.group("identify");
            Object value = environment.get(identify);
            String string = identify;

            // 集合就随机选择一个幸运成员。只有 Collection<String> 的成员会被特殊对待
            if (value instanceof Collection && !((Collection<?>) value).isEmpty()) {
                final Object tempValue = ((Collection<?>) value).toArray(new Object[0])[RANDOM.nextInt(((Collection<?>) value).size())];
                if (tempValue instanceof String) {
                    value = tempValue;
                }
            }
            if (Objects.nonNull(value)) {
                string = value instanceof String ? ((String) value) : value.toString();
                builder.replace(matcher.start(), matcher.end(), string);
            }
            matcher = VARIABLE_REFERENCE.matcher(builder);

            pos = Objects.nonNull(value) ? start : end;
            times++;
        }
        return builder.toString();
    }

    public static Map<String, Object> makeEnvironment(Object object) {
        if (Objects.isNull(object)) {
            return null;
        }

        final Class<?> clazz = object.getClass();
        Map<String, Object> environment = new HashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            final boolean accessible = field.isAccessible();
            field.setAccessible(true);

            try {
                environment.put(field.getName(), field.get(object));
            } catch (IllegalAccessException illegalAccessException) {
                illegalAccessException.printStackTrace();
            }

            field.setAccessible(accessible);
        }

        return environment;
    }
}