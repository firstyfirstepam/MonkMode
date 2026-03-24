package com.example.reelcounter.guard;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.reelcounter.model.LaunchApproval;

/**
 * Coordinates temporary post-friction approvals and UI state used to avoid redirect loops.
 * <p>
 * Approval tokens exist so that after the user legitimately completes friction, the
 * {@link com.example.reelcounter.TrackingAccessibilityService} does not immediately treat the
 * same foreground app as unauthorized. Without a token, showing {@code FrictionActivity} would
 * still leave the protected app underneath, and the service would fire again in a loop.
 * </p>
 */
public final class LaunchGuardManager {

    /** How long a completed friction session authorizes the matching package (MVP: single slot). */
    public static final long APPROVAL_VALIDITY_MS = 10_000L;

    private static final Object LOCK = new Object();
    private static volatile LaunchGuardManager instance;

    @Nullable
    private LaunchApproval currentApproval;
    private volatile boolean frictionSessionActive;
    private volatile long lastRedirectUptimeMs;

    private LaunchGuardManager() {
    }

    @NonNull
    public static LaunchGuardManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new LaunchGuardManager();
                }
            }
        }
        return instance;
    }

    /**
     * Called when friction completes successfully: grants the target package a short window
     * where the accessibility guard will not redirect again.
     */
    public void approveLaunch(@NonNull String packageName) {
        long now = System.currentTimeMillis();
        synchronized (LOCK) {
            currentApproval = new LaunchApproval(packageName, now, now + APPROVAL_VALIDITY_MS);
        }
    }

    public boolean hasValidApprovalFor(@NonNull String packageName) {
        long now = System.currentTimeMillis();
        synchronized (LOCK) {
            if (currentApproval == null) {
                return false;
            }
            if (currentApproval.isExpiredAt(now)) {
                currentApproval = null;
                return false;
            }
            return packageName.equals(currentApproval.getPackageName());
        }
    }

    public void clearApproval() {
        synchronized (LOCK) {
            currentApproval = null;
        }
    }

    public void setFrictionSessionActive(boolean active) {
        frictionSessionActive = active;
    }

    public boolean isFrictionSessionActive() {
        return frictionSessionActive;
    }

    /**
     * After we start a friction redirect, window events may still reference the protected app.
     * A short cooldown ignores those bursts so we do not stack multiple friction launches.
     */
    public boolean shouldIgnoreDueToRedirectCooldown() {
        return SystemClock.uptimeMillis() - lastRedirectUptimeMs < 2_500L;
    }

    public void markRedirectPerformed() {
        lastRedirectUptimeMs = SystemClock.uptimeMillis();
    }
}
