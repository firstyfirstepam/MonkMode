package com.example.reelcounter;

import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reelcounter.adapter.ProtectAppsAdapter;
import com.example.reelcounter.data.AppRepository;
import com.example.reelcounter.data.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Screen to pick which apps use strict mode (friction) before opening from this launcher.
 */
public class ProtectAppsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protect_apps);

        MaterialToolbar toolbar = findViewById(R.id.protect_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        PreferencesManager prefs = new PreferencesManager(this);
        AppRepository repo = new AppRepository();
        ProtectAppsAdapter adapter = new ProtectAppsAdapter(prefs);

        RecyclerView list = findViewById(R.id.protect_apps_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        PackageManager pm = getPackageManager();
        adapter.setApps(repo.loadLaunchableApps(pm));
    }
}
