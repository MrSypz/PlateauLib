package com.sypztep.plateau.common;

public final class NumberUtil {

    public static String formatNumber(long number) {
        return formatNumberInternal(number);
    }

    public static String formatNumber(double number) {
        return formatNumberInternal(number);
    }

    private static String formatNumberInternal(double number) {
        if (number >= 1_000_000_000_000L) {
            return String.format("%.1fT", number / 1_000_000_000_000.0);
        } else if (number >= 1_000_000_000L) {
            return String.format("%.1fB", number / 1_000_000_000.0);
        } else if (number >= 1_000_000L) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000L) {
            return String.format("%.1fK", number / 1_000.0);
        } else {
            return (number % 1 == 0) ? String.valueOf((long) number) : String.valueOf(number);
        }
    }

    public static String formatDouble(double value, int maxDecimals) {
        String format = "%." + maxDecimals + "f";
        String formatted = String.format(format, value);

        if (formatted.indexOf('.') >= 0) {
            formatted = formatted.replaceAll("0+$", "");
            formatted = formatted.replaceAll("\\.$", "");
        }
        return formatted;
    }

    public static String formatDouble(double value) {
        return formatDouble(value, 2);
    }
}
