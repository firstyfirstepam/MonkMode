package com.example.reelcounter.theme;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.example.reelcounter.R;
import com.example.reelcounter.data.PreferencesManager;

/**
 * Applies the user-selected app theme before {@code super.onCreate()}.
 */
public final class ThemeApplier {

    private ThemeApplier() {
    }

    public static void apply(@NonNull Activity activity) {
        activity.setTheme(resolveStyle(activity));
    }

    @StyleRes
    public static int resolveStyle(@NonNull Activity activity) {
        return resolveStyle(new PreferencesManager(activity));
    }

    @StyleRes
    public static int resolveStyle(@NonNull PreferencesManager prefs) {
        switch (prefs.getAppThemeId()) {
            case PreferencesManager.THEME_GLASS_SUN:
                return R.style.Theme_ReelCounter_GlassSun;
            case PreferencesManager.THEME_VIOLET_NIGHT:
                return R.style.Theme_ReelCounter_VioletNight;
            case PreferencesManager.THEME_PURPLE_LIGHT:
            default:
                return R.style.Theme_ReelCounter_Purple;
        }
    }
}
