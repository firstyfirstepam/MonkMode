package com.example.reelcounter.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.example.reelcounter.model.LaunchLogEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MVP persistence backed by {@link SharedPreferences}.
 * <p>
 * Launch logs and notes are stored as JSON text so the same shapes can move to Room later
 * without rewriting UI code.
 * </p>
 */
public final class PreferencesManager {

    private static final String PREFS = "friction_launcher_prefs";

    private static final String KEY_PROTECTED = "protected_packages";
    private static final String KEY_NOTES_JSON = "notes_json";
    private static final String KEY_LOGS_JSON = "launch_logs_json";
    private static final String KEY_COUNTDOWN_ENABLED = "countdown_enabled";
    private static final String KEY_PUZZLE_ENABLED = "puzzle_enabled";
    private static final String KEY_COUNTDOWN_SECONDS = "countdown_seconds";
    private static final String KEY_ACCESSIBILITY_GUARD = "accessibility_guard_enabled";

    private static final int MAX_LOG_ENTRIES = 2000;

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isFrictionProtected(@NonNull String packageName) {
        Set<String> set = prefs.getStringSet(KEY_PROTECTED, Collections.emptySet());
        return set != null && set.contains(packageName);
    }

    public void setFrictionProtected(@NonNull String packageName, boolean enabled) {
        Set<String> copy = new HashSet<>(getProtectedPackagesMutable());
        if (enabled) {
            copy.add(packageName);
        } else {
            copy.remove(packageName);
        }
        prefs.edit().putStringSet(KEY_PROTECTED, copy).apply();
    }

    /**
     * Snapshot of packages marked friction-protected (safe to mutate by caller).
     */
    @NonNull
    public Set<String> getProtectedPackagesSnapshot() {
        return new HashSet<>(getProtectedPackagesMutable());
    }

    @NonNull
    private Set<String> getProtectedPackagesMutable() {
        Set<String> existing = prefs.getStringSet(KEY_PROTECTED, Collections.emptySet());
        return new HashSet<>(existing != null ? existing : Collections.emptySet());
    }

    public boolean isCountdownEnabled() {
        return prefs.getBoolean(KEY_COUNTDOWN_ENABLED, true);
    }

    public void setCountdownEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_COUNTDOWN_ENABLED, enabled).apply();
    }

    public boolean isPuzzleEnabled() {
        return prefs.getBoolean(KEY_PUZZLE_ENABLED, true);
    }

    public void setPuzzleEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_PUZZLE_ENABLED, enabled).apply();
    }

    public int getCountdownSeconds() {
        int v = prefs.getInt(KEY_COUNTDOWN_SECONDS, 7);
        if (v < 1) {
            return 1;
        }
        if (v > 120) {
            return 120;
        }
        return v;
    }

    public void setCountdownSeconds(int seconds) {
        int clamped = Math.max(1, Math.min(120, seconds));
        prefs.edit().putInt(KEY_COUNTDOWN_SECONDS, clamped).apply();
    }

    /**
     * When true, user intends to use {@link com.example.reelcounter.TrackingAccessibilityService}
     * (must still enable the service in system settings).
     */
    public boolean isAccessibilityGuardEnabled() {
        return prefs.getBoolean(KEY_ACCESSIBILITY_GUARD, false);
    }

    public void setAccessibilityGuardEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ACCESSIBILITY_GUARD, enabled).apply();
    }

    @NonNull
    public List<String> getNotes() {
        List<String> out = new ArrayList<>();
        String raw = prefs.getString(KEY_NOTES_JSON, "[]");
        try {
            JSONArray arr = new JSONArray(raw != null ? raw : "[]");
            for (int i = 0; i < arr.length(); i++) {
                out.add(arr.optString(i, ""));
            }
        } catch (JSONException ignored) {
            // ignore corrupt storage
        }
        return out;
    }

    public void setNotes(@NonNull List<String> notes) {
        JSONArray arr = new JSONArray();
        for (String n : notes) {
            arr.put(n);
        }
        prefs.edit().putString(KEY_NOTES_JSON, arr.toString()).apply();
    }

    public void addNote(@NonNull String note) {
        String trimmed = note.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        List<String> list = getNotes();
        list.add(trimmed);
        setNotes(list);
    }

    public void removeNoteAtIndex(int index) {
        List<String> list = getNotes();
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            setNotes(list);
        }
    }

    /**
     * Appends a log row when friction starts; returns its index for later updates.
     */
    public int appendLaunchLog(@NonNull LaunchLogEntry entry) {
        List<JSONObject> list = readLogsAsObjects();
        try {
            list.add(entry.toJson());
        } catch (JSONException e) {
            return -1;
        }
        while (list.size() > MAX_LOG_ENTRIES) {
            list.remove(0);
        }
        writeLogsFromObjects(list);
        return list.size() - 1;
    }

    /**
     * Updates a row created by {@link #appendLaunchLog(LaunchLogEntry)}.
     */
    public void updateLaunchLog(int index, @NonNull String reason, boolean completed, boolean canceled) {
        List<JSONObject> list = readLogsAsObjects();
        if (index < 0 || index >= list.size()) {
            return;
        }
        JSONObject o = list.get(index);
        try {
            o.put("reason", reason);
            o.put("completed", completed);
            o.put("canceled", canceled);
            list.set(index, o);
            writeLogsFromObjects(list);
        } catch (JSONException ignored) {
            // ignore
        }
    }

    @NonNull
    public List<LaunchLogEntry> getLaunchLogsNewestFirst() {
        List<JSONObject> list = readLogsAsObjects();
        List<LaunchLogEntry> out = new ArrayList<>(list.size());
        for (JSONObject o : list) {
            LaunchLogEntry e = LaunchLogEntry.fromJson(o);
            if (e != null) {
                out.add(e);
            }
        }
        Collections.reverse(out);
        return out;
    }

    @NonNull
    public FrictionStats computeStats() {
        List<JSONObject> list = readLogsAsObjects();
        int attempted = list.size();
        int canceled = 0;
        int completed = 0;
        Map<String, Integer> attemptsByPackage = new HashMap<>();

        for (JSONObject o : list) {
            LaunchLogEntry e = LaunchLogEntry.fromJson(o);
            if (e == null) {
                continue;
            }
            String pkg = e.getPackageName();
            attemptsByPackage.put(pkg, attemptsByPackage.getOrDefault(pkg, 0) + 1);
            if (e.isCanceled()) {
                canceled++;
            }
            if (e.isCompletedFrictionAndOpened()) {
                completed++;
            }
        }

        String topPackage = null;
        int topCount = 0;
        for (Map.Entry<String, Integer> en : attemptsByPackage.entrySet()) {
            if (en.getValue() > topCount) {
                topCount = en.getValue();
                topPackage = en.getKey();
            }
        }

        return new FrictionStats(attempted, canceled, completed, topPackage, topCount);
    }

    @NonNull
    private List<JSONObject> readLogsAsObjects() {
        String raw = prefs.getString(KEY_LOGS_JSON, "[]");
        List<JSONObject> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw != null ? raw : "[]");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) {
                    list.add(o);
                }
            }
        } catch (JSONException ignored) {
            // return empty
        }
        return list;
    }

    private void writeLogsFromObjects(@NonNull List<JSONObject> list) {
        JSONArray arr = new JSONArray();
        for (JSONObject o : list) {
            arr.put(o);
        }
        prefs.edit().putString(KEY_LOGS_JSON, arr.toString()).apply();
    }
}
