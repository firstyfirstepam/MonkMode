package com.example.reelcounter.model;

import android.graphics.drawable.Drawable;

/**
 * Represents one launchable app row in the launcher grid.
 */
public final class AppInfo {

    private final String packageName;
    private final String label;
    private final Drawable icon;

    public AppInfo(String packageName, String label, Drawable icon) {
        this.packageName = packageName;
        this.label = label;
        this.icon = icon;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getLabel() {
        return label;
    }

    public Drawable getIcon() {
        return icon;
    }
}
