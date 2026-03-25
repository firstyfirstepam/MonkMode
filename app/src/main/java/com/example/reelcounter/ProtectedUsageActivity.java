package com.example.reelcounter;

import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.reelcounter.adapter.ProtectedUsageAdapter;
import com.example.reelcounter.data.PreferencesManager;
import com.example.reelcounter.data.ProtectedUsageLoader;
import com.example.reelcounter.model.ProtectedUsageSummary;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shows Android {@link android.app.usage.UsageStatsManager} data for strict-mode packages only:
 * foreground duration and approximate open counts per window (today / 7d / 30d).
 */
public class ProtectedUsageActivity extends BaseThemedActivity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private PreferencesManager preferencesManager;
    private ProtectedUsageAdapter adapter;
    private SwipeRefreshLayout refresh;
    private TextView empty;
    private TextView periodHint;
    private MaterialCardView permissionCard;
    private TabLayout tabLayout;

    private ProtectedUsageLoader.Period currentPeriod = ProtectedUsageLoader.Period.DAILY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protected_usage);

        preferencesManager = new PreferencesManager(this);

        MaterialToolbar toolbar = findViewById(R.id.usage_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        periodHint = findViewById(R.id.usage_period_hint);
        tabLayout = findViewById(R.id.usage_tabs);
        refresh = findViewById(R.id.usage_refresh);
        RecyclerView list = findViewById(R.id.usage_list);
        empty = findViewById(R.id.usage_empty);
        permissionCard = findViewById(R.id.usage_permission_card);
        MaterialButton grant = findViewById(R.id.usage_grant_button);

        adapter = new ProtectedUsageAdapter(getPackageManager());
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        RecyclerView.ItemAnimator animator = list.getItemAnimator();
        if (animator instanceof DefaultItemAnimator) {
            ((DefaultItemAnimator) animator).setSupportsChangeAnimations(true);
        }

        TypedArray a = obtainStyledAttributes(new int[]{androidx.appcompat.R.attr.colorPrimary});
        int primary = a.getColor(0, 0xFF6200EE);
        a.recycle();
        refresh.setColorSchemeColors(primary);
        refresh.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.white, getTheme()));
        refresh.setOnRefreshListener(this::reload);

        grant.setOnClickListener(v -> openUsageAccessSettings());

        tabLayout.addTab(tabLayout.newTab().setText(R.string.usage_tab_daily));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.usage_tab_weekly));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.usage_tab_monthly));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    currentPeriod = ProtectedUsageLoader.Period.DAILY;
                    periodHint.setText(R.string.usage_period_hint_today);
                } else if (tab.getPosition() == 1) {
                    currentPeriod = ProtectedUsageLoader.Period.WEEKLY;
                    periodHint.setText(R.string.usage_period_hint_week);
                } else {
                    currentPeriod = ProtectedUsageLoader.Period.MONTHLY;
                    periodHint.setText(R.string.usage_period_hint_month);
                }
                reload();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        periodHint.setText(R.string.usage_period_hint_today);
        currentPeriod = ProtectedUsageLoader.Period.DAILY;
        updatePermissionUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionUi();
        reload();
    }

    @Override
    protected void onDestroy() {
        io.shutdown();
        super.onDestroy();
    }

    /**
     * Opens the system App usage data screen. Package URI helps some devices highlight this app
     * after {@link android.Manifest.permission#PACKAGE_USAGE_STATS} is declared in the manifest.
     */
    private void openUsageAccessSettings() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.setData(Uri.fromParts("package", getPackageName(), null));
        }
        startActivity(intent);
    }

    private void updatePermissionUi() {
        boolean access = ProtectedUsageLoader.hasUsageAccess(this);
        permissionCard.setVisibility(access ? View.GONE : View.VISIBLE);
    }

    private void reload() {
        ProtectedUsageLoader.Period period = currentPeriod;
        refresh.setRefreshing(true);

        io.execute(() -> {
            Set<String> strict = preferencesManager.getProtectedPackagesSnapshot();
            boolean access = ProtectedUsageLoader.hasUsageAccess(getApplicationContext());
            List<ProtectedUsageSummary> data;
            if (!access || strict.isEmpty()) {
                data = Collections.emptyList();
            } else {
                data = ProtectedUsageLoader.load(getApplicationContext(), strict, period);
            }

            List<ProtectedUsageSummary> finalData = data;
            boolean finalAccess = access;
            runOnUiThread(() -> {
                refresh.setRefreshing(false);
                adapter.setItems(finalData);
                updateEmptyState(finalAccess, strict.isEmpty());
            });
        });
    }

    private void updateEmptyState(boolean access, boolean noStrict) {
        if (!access) {
            empty.setVisibility(View.VISIBLE);
            empty.setText(R.string.usage_empty_no_permission);
            return;
        }
        if (noStrict) {
            empty.setVisibility(View.VISIBLE);
            empty.setText(R.string.usage_empty_no_apps);
            return;
        }
        empty.setVisibility(View.GONE);
    }
}
