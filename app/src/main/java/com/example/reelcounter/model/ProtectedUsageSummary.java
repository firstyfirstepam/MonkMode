package com.example.reelcounter.model;

import androidx.annotation.NonNull;

/**
 * Aggregated usage for one strict-mode package over a selected time window.
 */
public final class ProtectedUsageSummary implements Comparable<ProtectedUsageSummary> {

    private final String packageName;
    @NonNull
    private final String label;
    private final long totalTimeForegroundMs;
    private final int foregroundOpenCount;

    public ProtectedUsageSummary(
            @NonNull String packageName,
            @NonNull String label,
            long totalTimeForegroundMs,
            int foregroundOpenCount
    ) {
        this.packageName = packageName;
        this.label = label;
        this.totalTimeForegroundMs = totalTimeForegroundMs;
        this.foregroundOpenCount = foregroundOpenCount;
    }

    @NonNull
    public String getPackageName() {
        return packageName;
    }

    @NonNull
    public String getLabel() {
        return label;
    }

    public long getTotalTimeForegroundMs() {
        return totalTimeForegroundMs;
    }

    public int getForegroundOpenCount() {
        return foregroundOpenCount;
    }

    @Override
    public int compareTo(ProtectedUsageSummary o) {
        return Long.compare(o.totalTimeForegroundMs, this.totalTimeForegroundMs);
    }
}
