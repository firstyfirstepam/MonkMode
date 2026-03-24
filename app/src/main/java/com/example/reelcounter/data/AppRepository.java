package com.example.reelcounter.data;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import com.example.reelcounter.model.AppInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Loads launchable apps using {@link PackageManager}.
 */
public final class AppRepository {

    /**
     * Returns installed apps that appear in the launcher (MAIN / LAUNCHER), sorted by label.
     */
    public List<AppInfo> loadLaunchableApps(PackageManager pm) {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolves = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_DEFAULT_ONLY);
        List<AppInfo> apps = new ArrayList<>(resolves.size());
        for (ResolveInfo info : resolves) {
            String pkg = info.activityInfo.packageName;
            CharSequence labelCs = info.loadLabel(pm);
            String label = labelCs != null ? labelCs.toString() : pkg;
            Drawable icon = info.loadIcon(pm);
            apps.add(new AppInfo(pkg, label, icon));
        }

        Collections.sort(apps, Comparator.comparing(AppInfo::getLabel, String.CASE_INSENSITIVE_ORDER));
        return apps;
    }
}
