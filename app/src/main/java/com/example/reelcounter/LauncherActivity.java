package com.example.reelcounter;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reelcounter.adapter.AppGridAdapter;
import com.example.reelcounter.data.AppRepository;
import com.example.reelcounter.data.PreferencesManager;
import com.example.reelcounter.model.AppInfo;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Custom home screen: grid of launchable apps with optional friction for protected packages.
 * <p>
 * <b>Launcher + accessibility guard</b> reduces loopholes (notifications, recents, many deep
 * links) but cannot block every possible path. This app is for personal digital wellbeing, not
 * airtight enforcement.
 * </p>
 */
public class LauncherActivity extends BaseThemedActivity implements AppGridAdapter.Listener {

    private PreferencesManager preferencesManager;
    private AppRepository appRepository;
    private AppGridAdapter adapter;
    private final Set<String> protectedSnapshot = new HashSet<>();
    private final List<AppInfo> allApps = new ArrayList<>();
    private RecyclerView recyclerView;
    private TextView emptyView;
    private TextInputEditText searchInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        preferencesManager = new PreferencesManager(this);
        appRepository = new AppRepository();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        MaterialButton strictModeButton = findViewById(R.id.button_strict_mode_apps);
        strictModeButton.setOnClickListener(v ->
                startActivity(new Intent(this, ProtectAppsActivity.class)));

        recyclerView = findViewById(R.id.app_grid);
        emptyView = findViewById(R.id.empty_view);
        searchInput = findViewById(R.id.search_input);

        protectedSnapshot.clear();
        protectedSnapshot.addAll(preferencesManager.getProtectedPackagesSnapshot());
        adapter = new AppGridAdapter(this, protectedSnapshot);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        recyclerView.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        protectedSnapshot.clear();
        protectedSnapshot.addAll(preferencesManager.getProtectedPackagesSnapshot());
        adapter.setProtectedPackages(protectedSnapshot);
        applyFilter(searchInput.getText() != null ? searchInput.getText().toString() : "");
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        allApps.clear();
        allApps.addAll(appRepository.loadLaunchableApps(pm));
        applyFilter(searchInput.getText() != null ? searchInput.getText().toString() : "");
    }

    private void applyFilter(@NonNull String queryRaw) {
        String q = queryRaw.trim().toLowerCase(Locale.getDefault());
        List<AppInfo> filtered = new ArrayList<>();
        for (AppInfo app : allApps) {
            if (q.isEmpty()) {
                filtered.add(app);
            } else {
                String label = app.getLabel() != null ? app.getLabel().toLowerCase(Locale.getDefault()) : "";
                String pkg = app.getPackageName().toLowerCase(Locale.getDefault());
                if (label.contains(q) || pkg.contains(q)) {
                    filtered.add(app);
                }
            }
        }
        adapter.setApps(filtered);
        boolean empty = filtered.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_protect_apps) {
            startActivity(new Intent(this, ProtectAppsActivity.class));
            return true;
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.action_stats) {
            startActivity(new Intent(this, StatsActivity.class));
            return true;
        }
        if (id == R.id.action_usage) {
            startActivity(new Intent(this, ProtectedUsageActivity.class));
            return true;
        }
        if (id == R.id.action_notes) {
            startActivity(new Intent(this, NotesActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAppClicked(@NonNull AppInfo app) {
        String pkg = app.getPackageName();

        /*
         * Launcher interception: protected apps never start directly from the grid; friction runs
         * first. The accessibility service adds a second layer for opens that skip this path.
         */
        if (preferencesManager.isFrictionProtected(pkg)) {
            Intent friction = new Intent(this, FrictionActivity.class);
            friction.putExtra(FrictionActivity.EXTRA_TARGET_PACKAGE, pkg);
            friction.putExtra(FrictionActivity.EXTRA_LAUNCH_SOURCE, FrictionActivity.SOURCE_LAUNCHER);
            startActivity(friction);
            return;
        }

        launchApp(pkg);
    }

    @Override
    public void onAppLongPressed(@NonNull AppInfo app) {
        String pkg = app.getPackageName();
        boolean next = !preferencesManager.isFrictionProtected(pkg);
        preferencesManager.setFrictionProtected(pkg, next);
        protectedSnapshot.clear();
        protectedSnapshot.addAll(preferencesManager.getProtectedPackagesSnapshot());
        adapter.setProtectedPackages(protectedSnapshot);

        String msg = getString(
                next ? R.string.snackbar_protected_on : R.string.snackbar_protected_off,
                app.getLabel()
        );
        Snackbar.make(recyclerView, msg, Snackbar.LENGTH_SHORT).show();
    }

    private void launchApp(@NonNull String packageName) {
        PackageManager pm = getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(packageName);
        if (launch == null) {
            Toast.makeText(
                    this,
                    getString(R.string.snackbar_launch_failed, packageName),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launch);
    }
}
