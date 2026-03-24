package com.example.reelcounter.guard;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.example.reelcounter.FrictionActivity;

/**
 * Starts {@link FrictionActivity} from non-Activity contexts (e.g. accessibility) with safe flags.
 * <p>
 * Uses the main looper because starting activities from arbitrary accessibility callbacks can be
 * fragile on some OEM builds.
 * </p>
 */
public final class RedirectController {

    private RedirectController() {
    }

    public static void showFrictionForPackage(
            @NonNull Context appContext,
            @NonNull String targetPackage
    ) {
        LaunchGuardManager.getInstance().markRedirectPerformed();
        Handler main = new Handler(Looper.getMainLooper());
        main.post(() -> {
            Intent i = new Intent(appContext, FrictionActivity.class);
            i.putExtra(FrictionActivity.EXTRA_TARGET_PACKAGE, targetPackage);
            i.putExtra(FrictionActivity.EXTRA_LAUNCH_SOURCE, FrictionActivity.SOURCE_GUARD);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            appContext.startActivity(i);
        });
    }
}
