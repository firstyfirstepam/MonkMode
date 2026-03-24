package com.example.reelcounter;

import android.accessibilityservice.AccessibilityService;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;

import com.example.reelcounter.data.PreferencesManager;
import com.example.reelcounter.guard.LaunchGuardManager;
import com.example.reelcounter.guard.ProtectedAppsManager;
import com.example.reelcounter.guard.RedirectController;
import com.example.reelcounter.util.ForegroundEventDebouncer;

/**
 * Observes foreground windows to catch protected apps opened outside the launcher flow.
 * <p>
 * <b>Limitation:</b> Launcher + accessibility <i>reduces</i> loopholes (notifications, recents,
 * many deep links) but cannot guarantee blocking every path. Split-screen, OEM behaviors, and
 * timing gaps may still allow brief access. This is a personal wellbeing aid, not a security
 * boundary.
 * </p>
 */
public class TrackingAccessibilityService extends AccessibilityService {

    private final ForegroundEventDebouncer debouncer = new ForegroundEventDebouncer();
    private PreferencesManager preferencesManager;
    private ProtectedAppsManager protectedAppsManager;
    private String ourPackageName;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        ourPackageName = getPackageName();
        preferencesManager = new PreferencesManager(this);
        protectedAppsManager = new ProtectedAppsManager(preferencesManager);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (preferencesManager == null) {
            return;
        }
        if (!preferencesManager.isAccessibilityGuardEnabled()) {
            return;
        }
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) {
            return;
        }
        @NonNull String packageName = pkgCs.toString();

        // Never act on our own UI — would cause redirect loops.
        if (packageName.equals(ourPackageName)) {
            return;
        }

        LaunchGuardManager guard = LaunchGuardManager.getInstance();
        if (guard.isFrictionSessionActive()) {
            return;
        }
        if (guard.shouldIgnoreDueToRedirectCooldown()) {
            return;
        }
        if (!debouncer.shouldHandle(packageName, SystemClock.uptimeMillis())) {
            return;
        }
        if (!protectedAppsManager.isProtected(packageName)) {
            return;
        }
        if (guard.hasValidApprovalFor(packageName)) {
            return;
        }

        RedirectController.showFrictionForPackage(getApplicationContext(), packageName);
    }

    @Override
    public void onInterrupt() {
        // Nothing to cancel for this MVP.
    }
}
