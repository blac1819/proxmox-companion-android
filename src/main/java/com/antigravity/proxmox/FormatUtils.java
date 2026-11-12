package com.antigravity.proxmox;

import android.text.format.Formatter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for formatting numbers and timestamps for display.
 */
public final class FormatUtils {
    private FormatUtils() {
        // Prevent instantiation
    }

    /**
     * Format a byte count into a human‑readable string (e.g. "12.3 MB").
     * Uses base‑1024 units (KB = 1024 bytes).
     */
    public static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);
        double scaled = bytes / Math.pow(1024, digitGroups);
        return String.format(Locale.US, "%.1f %s", scaled, units[digitGroups]);
    }

    /**
     * Format an uptime given in seconds into a readable form like "2d 04:12:05".
     */
    public static String formatUptime(long seconds) {
        long days = seconds / 86400;
        long hrs = (seconds % 86400) / 3600;
        long mins = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (days > 0) {
            return String.format(Locale.US, "%dd %02d:%02d:%02d", days, hrs, mins, secs);
        } else {
            return String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs);
        }
    }

    /**
     * Convert a Unix timestamp (seconds since epoch) to a formatted date/time string.
     */
    public static String formatTimestamp(long epochSeconds) {
        Date date = new Date(epochSeconds * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return sdf.format(date);
    }
}
