package com.example.reelcounter;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.reelcounter.data.FrictionStats;
import com.example.reelcounter.data.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Simple readout of friction metrics derived from stored launch logs.
 */
public class StatsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        MaterialToolbar toolbar = findViewById(R.id.stats_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        TextView attempts = findViewById(R.id.stats_attempts);
        TextView canceled = findViewById(R.id.stats_canceled);
        TextView completed = findViewById(R.id.stats_completed);
        TextView top = findViewById(R.id.stats_top);

        PreferencesManager prefs = new PreferencesManager(this);
        FrictionStats stats = prefs.computeStats();

        attempts.setText(getString(R.string.stats_attempts, stats.getTotalProtectedOpenAttempts()));
        canceled.setText(getString(R.string.stats_canceled, stats.getTotalCanceled()));
        completed.setText(getString(R.string.stats_completed, stats.getTotalCompletedOpenings()));

        String topLine = buildTopLine(this, stats);
        top.setText(topLine);
    }

    @NonNull
    private static String buildTopLine(@NonNull StatsActivity activity, @NonNull FrictionStats stats) {
        @Nullable String pkg = stats.getMostAttemptedPackageName();
        if (pkg == null || stats.getMostAttemptedCount() <= 0) {
            return activity.getString(R.string.stats_top_unknown);
        }
        PackageManager pm = activity.getPackageManager();
        String label = pkg;
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            CharSequence cs = pm.getApplicationLabel(ai);
            if (cs != null && cs.length() > 0) {
                label = cs.toString();
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // keep package name
        }
        return activity.getString(R.string.stats_top, label, stats.getMostAttemptedCount());
    }
}
