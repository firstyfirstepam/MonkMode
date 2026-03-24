package com.example.reelcounter.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * One friction attempt for a protected app.
 * <p>
 * Structured for easy migration to Room later (same fields could become table columns).
 * </p>
 */
public final class LaunchLogEntry {

    private final String packageName;
    private final String appLabel;
    private final long timestampUtcMillis;
    @NonNull
    private final String reason;
    private final boolean completedFrictionAndOpened;
    private final boolean canceled;

    public LaunchLogEntry(
            String packageName,
            String appLabel,
            long timestampUtcMillis,
            @NonNull String reason,
            boolean completedFrictionAndOpened,
            boolean canceled
    ) {
        this.packageName = packageName;
        this.appLabel = appLabel;
        this.timestampUtcMillis = timestampUtcMillis;
        this.reason = reason;
        this.completedFrictionAndOpened = completedFrictionAndOpened;
        this.canceled = canceled;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppLabel() {
        return appLabel;
    }

    public long getTimestampUtcMillis() {
        return timestampUtcMillis;
    }

    @NonNull
    public String getReason() {
        return reason;
    }

    public boolean isCompletedFrictionAndOpened() {
        return completedFrictionAndOpened;
    }

    public boolean isCanceled() {
        return canceled;
    }

    @NonNull
    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("packageName", packageName);
        o.put("appLabel", appLabel);
        o.put("timestamp", timestampUtcMillis);
        o.put("reason", reason);
        o.put("completed", completedFrictionAndOpened);
        o.put("canceled", canceled);
        return o;
    }

    @Nullable
    public static LaunchLogEntry fromJson(@Nullable JSONObject o) {
        if (o == null) {
            return null;
        }
        try {
            return new LaunchLogEntry(
                    o.optString("packageName", ""),
                    o.optString("appLabel", ""),
                    o.optLong("timestamp", 0L),
                    o.optString("reason", ""),
                    o.optBoolean("completed", false),
                    o.optBoolean("canceled", false)
            );
        } catch (Exception e) {
            return null;
        }
    }
}
