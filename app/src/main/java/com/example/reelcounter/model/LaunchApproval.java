package com.example.reelcounter.model;

import androidx.annotation.NonNull;

/**
 * One-time launch approval for a protected package after the user completes friction.
 * <p>
 * The AccessibilityService compares the foreground package against this record. A short
 * expiry window (e.g. 10 seconds) limits how long a completed friction session can be reused,
 * so a later accidental open still triggers friction unless the user goes through it again.
 * </p>
 */
public final class LaunchApproval {

    private final String packageName;
    private final long approvedAtUtcMillis;
    private final long expiresAtUtcMillis;

    public LaunchApproval(
            @NonNull String packageName,
            long approvedAtUtcMillis,
            long expiresAtUtcMillis
    ) {
        this.packageName = packageName;
        this.approvedAtUtcMillis = approvedAtUtcMillis;
        this.expiresAtUtcMillis = expiresAtUtcMillis;
    }

    @NonNull
    public String getPackageName() {
        return packageName;
    }

    public long getApprovedAtUtcMillis() {
        return approvedAtUtcMillis;
    }

    public long getExpiresAtUtcMillis() {
        return expiresAtUtcMillis;
    }

    public boolean isExpiredAt(long nowUtcMillis) {
        return nowUtcMillis >= expiresAtUtcMillis;
    }
}
