package com.example.reelcounter.guard;

import androidx.annotation.NonNull;

import com.example.reelcounter.data.PreferencesManager;

import java.util.Set;

/**
 * Read-only view of which packages are friction-protected (strict mode).
 */
public final class ProtectedAppsManager {

    private final PreferencesManager preferencesManager;

    public ProtectedAppsManager(@NonNull PreferencesManager preferencesManager) {
        this.preferencesManager = preferencesManager;
    }

    public boolean isProtected(@NonNull String packageName) {
        return preferencesManager.isFrictionProtected(packageName);
    }

    @NonNull
    public Set<String> protectedPackagesSnapshot() {
        return preferencesManager.getProtectedPackagesSnapshot();
    }
}
