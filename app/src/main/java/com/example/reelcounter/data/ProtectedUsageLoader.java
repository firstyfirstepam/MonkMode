package com.example.reelcounter.data;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;

import androidx.annotation.NonNull;

import com.example.reelcounter.R;
import com.example.reelcounter.model.ProtectedUsageSummary;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Loads foreground time and open counts from {@link UsageStatsManager} for strict packages only.
 */
public final class ProtectedUsageLoader {

    public enum Period {
        DAILY,
        WEEKLY,
        MONTHLY
    }

    private ProtectedUsageLoader() {
    }

    public static boolean hasUsageAccess(@NonNull Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) {
            return false;
        }
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.getPackageName()
        );
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    @NonNull
    public static List<ProtectedUsageSummary> load(
            @NonNull Context context,
            @NonNull Set<String> protectedPackages,
            @NonNull Period period
    ) {
        if (!hasUsageAccess(context) || protectedPackages.isEmpty()) {
            return Collections.emptyList();
        }

        long end = System.currentTimeMillis();
        long start = startUtcMillis(period, end);

        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) {
            return Collections.emptyList();
        }

        Map<String, Long> durationByPkg = aggregateForegroundTime(usm, start, end);
        Map<String, Integer> opensByPkg = countForegroundEntries(usm, start, end, protectedPackages);

        PackageManager pm = context.getPackageManager();
        List<ProtectedUsageSummary> out = new ArrayList<>(protectedPackages.size());
        for (String pkg : protectedPackages) {
            String label = pkg;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                CharSequence cs = pm.getApplicationLabel(ai);
                if (cs != null && cs.length() > 0) {
                    label = cs.toString();
                }
            } catch (PackageManager.NameNotFoundException ignored) {
                // keep package name as label
            }
            long ms = durationByPkg.getOrDefault(pkg, 0L);
            int opens = opensByPkg.getOrDefault(pkg, 0);
            out.add(new ProtectedUsageSummary(pkg, label, ms, opens));
        }

        Collections.sort(out);
        return out;
    }

    private static long startUtcMillis(@NonNull Period period, long endUtc) {
        switch (period) {
            case DAILY:
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                return cal.getTimeInMillis();
            case WEEKLY:
                return endUtc - 7L * 24L * 60L * 60L * 1000L;
            case MONTHLY:
            default:
                return endUtc - 30L * 24L * 60L * 60L * 1000L;
        }
    }

    @NonNull
    private static Map<String, Long> aggregateForegroundTime(
            @NonNull UsageStatsManager usm,
            long start,
            long end
    ) {
        Map<String, Long> out = new HashMap<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Map<String, UsageStats> aggregated = usm.queryAndAggregateUsageStats(start, end);
            if (aggregated != null) {
                for (Map.Entry<String, UsageStats> e : aggregated.entrySet()) {
                    UsageStats us = e.getValue();
                    if (us != null) {
                        out.put(e.getKey(), us.getTotalTimeInForeground());
                    }
                }
            }
        } else {
            List<UsageStats> list = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end);
            if (list != null) {
                for (UsageStats us : list) {
                    out.merge(us.getPackageName(), us.getTotalTimeInForeground(), Long::sum);
                }
            }
        }
        return out;
    }

    @NonNull
    private static Map<String, Integer> countForegroundEntries(
            @NonNull UsageStatsManager usm,
            long start,
            long end,
            @NonNull Set<String> protectedPackages
    ) {
        Map<String, Integer> map = new HashMap<>();
        UsageEvents events = usm.queryEvents(start, end);
        if (events == null) {
            return map;
        }
        UsageEvents.Event e = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(e);
            String pkg = e.getPackageName();
            if (pkg == null || !protectedPackages.contains(pkg)) {
                continue;
            }
            int type = e.getEventType();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (type == UsageEvents.Event.ACTIVITY_RESUMED) {
                    map.merge(pkg, 1, Integer::sum);
                }
            } else {
                if (type == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    map.merge(pkg, 1, Integer::sum);
                }
            }
        }
        return map;
    }

    @NonNull
    public static String formatDurationShort(long ms, @NonNull Context context) {
        if (ms < 60_000L) {
            long sec = Math.max(1L, ms / 1000L);
            return context.getString(R.string.usage_duration_seconds, sec);
        }
        long minutes = ms / 60_000L;
        long hours = minutes / 60L;
        long mins = minutes % 60L;
        if (hours > 0) {
            return context.getString(R.string.usage_duration_hours_mins, hours, mins);
        }
        return context.getString(R.string.usage_duration_mins, minutes);
    }

    @NonNull
    public static String formatOpens(int count, @NonNull Context context) {
        return context.getResources().getQuantityString(R.plurals.usage_opens_count, count, count);
    }
}
