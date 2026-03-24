package com.example.reelcounter.util;

import androidx.annotation.NonNull;

/**
 * Drops duplicate foreground notifications for the same package in a short window.
 * <p>
 * Accessibility can emit many {@code TYPE_WINDOW_STATE_CHANGED} events in quick succession
 * (activity transitions, dialogs, recents). Without debouncing, the guard could enqueue multiple
 * friction redirects for one user action.
 * </p>
 */
public final class ForegroundEventDebouncer {

    private static final long DEBOUNCE_MS = 600L;

    @NonNull
    private String lastPackage = "";
    private long lastEventUptimeMs;

    public boolean shouldHandle(@NonNull String packageName, long uptimeMillis) {
        if (packageName.equals(lastPackage) && uptimeMillis - lastEventUptimeMs < DEBOUNCE_MS) {
            return false;
        }
        lastPackage = packageName;
        lastEventUptimeMs = uptimeMillis;
        return true;
    }
}
