package com.antigravity.proxmox.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormatUtils {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB", "PB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        double value = bytes / Math.pow(1024, digitGroups);
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups]);
    }

    public static String formatPercent(double fraction) {
        double pct = fraction * 100.0;
        return String.format(Locale.US, "%.1f%%", pct);
    }

    public static String formatUptime(long seconds) {
        if (seconds <= 0) return "-";
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (days > 0) {
            return String.format(Locale.US, "%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format(Locale.US, "%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format(Locale.US, "%dm %ds", minutes, secs);
        } else {
            return String.format(Locale.US, "%ds", secs);
        }
    }

    public static String formatTimestamp(long epochSeconds) {
        if (epochSeconds <= 0) return "-";
        return DATE_FORMAT.format(new Date(epochSeconds * 1000L));
    }
}
