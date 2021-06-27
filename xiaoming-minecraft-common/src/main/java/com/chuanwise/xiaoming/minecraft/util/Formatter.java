package com.chuanwise.xiaoming.minecraft.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Formatter {
    public static final String COLOR_HEAD = "§";
    public static final String YELLOW = COLOR_HEAD + "e";
    public static final String RED = COLOR_HEAD + "c";
    public static final String ORANGE = COLOR_HEAD + "6";
    public static final String GREEN = COLOR_HEAD + "a";
    public static final String BLUE = COLOR_HEAD + "b";
    public static final String RESET = COLOR_HEAD + "r";
    public static final String GRAY = COLOR_HEAD + "7";
    public static final String WHITE = COLOR_HEAD + "f";

    public static final String MESSAGE_HEAD = aroundByGrayBracket(yellow("小明"));

    public static String yellow(String message) {
        return YELLOW + message + RESET;
    }

    public static String orange(String message) {
        return ORANGE + message + RESET;
    }

    public static String red(String message) {
        return RED + message + RESET;
    }

    public static String blue(String message) {
        return BLUE + message + RESET;
    }

    public static String gray(String message) {
        return GRAY + message + RESET;
    }

    public static String green(String message) {
        return GREEN + message + RESET;
    }

    public static String white(String message) {
        return WHITE + message + RESET;
    }

    public static String headThen(String message) {
        return MESSAGE_HEAD + " " + white(message);
    }

    public static String aroundByBracket(String center, String bracketColor) {
        return bracketColor + "[" + center + bracketColor + "]" + RESET;
    }

    public static String aroundByGrayBracket(String center) {
        return aroundByBracket(center, GRAY);
    }

    protected static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");
    public static String translateHexColorCodes(String message) {
        Matcher matcher = HEX_COLOR_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 32);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, COLOR_HEAD + "x"
                    + COLOR_HEAD + group.charAt(0) + COLOR_HEAD + group.charAt(1)
                    + COLOR_HEAD + group.charAt(2) + COLOR_HEAD + group.charAt(3)
                    + COLOR_HEAD + group.charAt(4) + COLOR_HEAD + group.charAt(5)
            );
        }
        return matcher.appendTail(buffer).toString();
    }

    protected static final Pattern ORIGINAL_COLOR_PATTERN = Pattern.compile("[&](?<center>.)");
    public static String translateOriginalColorCodes(String message) {
        Matcher matcher = ORIGINAL_COLOR_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer(message);
        while (matcher.find()) {
            buffer.replace(matcher.start(), matcher.end(), COLOR_HEAD + matcher.group("center"));
        }
        return buffer.toString();
    }

    public static String translateColorCodes(String message) {
        return translateOriginalColorCodes(translateHexColorCodes(message));
    }

    public static String clearColors(String message) {
        return message.replaceAll("[&§].", "");
    }
}