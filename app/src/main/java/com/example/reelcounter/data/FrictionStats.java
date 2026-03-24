package com.example.reelcounter.data;

import androidx.annotation.Nullable;

/**
 * Aggregated friction metrics derived from stored launch logs.
 */
public final class FrictionStats {

    private final int totalProtectedOpenAttempts;
    private final int totalCanceled;
    private final int totalCompletedOpenings;
    @Nullable
    private final String mostAttemptedPackageName;
    private final int mostAttemptedCount;

    public FrictionStats(
            int totalProtectedOpenAttempts,
            int totalCanceled,
            int totalCompletedOpenings,
            @Nullable String mostAttemptedPackageName,
            int mostAttemptedCount
    ) {
        this.totalProtectedOpenAttempts = totalProtectedOpenAttempts;
        this.totalCanceled = totalCanceled;
        this.totalCompletedOpenings = totalCompletedOpenings;
        this.mostAttemptedPackageName = mostAttemptedPackageName;
        this.mostAttemptedCount = mostAttemptedCount;
    }

    public int getTotalProtectedOpenAttempts() {
        return totalProtectedOpenAttempts;
    }

    public int getTotalCanceled() {
        return totalCanceled;
    }

    public int getTotalCompletedOpenings() {
        return totalCompletedOpenings;
    }

    @Nullable
    public String getMostAttemptedPackageName() {
        return mostAttemptedPackageName;
    }

    public int getMostAttemptedCount() {
        return mostAttemptedCount;
    }
}
