package com.example.reelcounter;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import android.widget.RadioGroup;

import com.example.reelcounter.data.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Friction toggles, accessibility guard preference, and links to protected apps / notes.
 */
public class SettingsActivity extends BaseThemedActivity {

    private PreferencesManager prefs;
    private SwitchMaterial guardSwitch;
    private TextView accessibilityStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new PreferencesManager(this);

        MaterialToolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        bindThemePicker();

        guardSwitch = findViewById(R.id.switch_accessibility_guard);
        accessibilityStatus = findViewById(R.id.accessibility_status_text);
        MaterialButton openA11y = findViewById(R.id.open_accessibility_settings_button);

        SwitchMaterial countdown = findViewById(R.id.switch_countdown);
        SwitchMaterial puzzle = findViewById(R.id.switch_puzzle);
        TextInputEditText secondsInput = findViewById(R.id.countdown_seconds_input);
        MaterialButton applySeconds = findViewById(R.id.apply_seconds_button);
        MaterialButton openStrictApps = findViewById(R.id.open_strict_apps_button);
        MaterialButton openUsage = findViewById(R.id.open_usage_button);
        MaterialButton openNotes = findViewById(R.id.open_notes_button);

        guardSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.setAccessibilityGuardEnabled(isChecked));

        openA11y.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        countdown.setChecked(prefs.isCountdownEnabled());
        puzzle.setChecked(prefs.isPuzzleEnabled());
        secondsInput.setText(String.valueOf(prefs.getCountdownSeconds()));

        countdown.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.setCountdownEnabled(isChecked));

        puzzle.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.setPuzzleEnabled(isChecked));

        applySeconds.setOnClickListener(v -> {
            if (secondsInput.getText() == null) {
                return;
            }
            String raw = secondsInput.getText().toString().trim();
            try {
                int s = Integer.parseInt(raw);
                prefs.setCountdownSeconds(s);
                secondsInput.setText(String.valueOf(prefs.getCountdownSeconds()));
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.settings_seconds_invalid, Toast.LENGTH_SHORT).show();
            }
        });

        openStrictApps.setOnClickListener(v ->
                startActivity(new Intent(this, ProtectAppsActivity.class)));

        openUsage.setOnClickListener(v ->
                startActivity(new Intent(this, ProtectedUsageActivity.class)));

        openNotes.setOnClickListener(v ->
                startActivity(new Intent(this, NotesActivity.class)));

        bindGuardUi();
    }

    private void bindThemePicker() {
        RadioGroup group = findViewById(R.id.theme_radio_group);
        String current = prefs.getAppThemeId();
        group.setOnCheckedChangeListener(null);
        if (PreferencesManager.THEME_GLASS_SUN.equals(current)) {
            group.check(R.id.theme_glass_sun);
        } else if (PreferencesManager.THEME_VIOLET_NIGHT.equals(current)) {
            group.check(R.id.theme_violet_night);
        } else {
            group.check(R.id.theme_purple);
        }
        group.setOnCheckedChangeListener((g, checkedId) -> {
            String next;
            if (checkedId == R.id.theme_glass_sun) {
                next = PreferencesManager.THEME_GLASS_SUN;
            } else if (checkedId == R.id.theme_violet_night) {
                next = PreferencesManager.THEME_VIOLET_NIGHT;
            } else {
                next = PreferencesManager.THEME_PURPLE_LIGHT;
            }
            if (!next.equals(prefs.getAppThemeId())) {
                prefs.setAppThemeId(next);
                recreate();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindGuardUi();
    }

    private void bindGuardUi() {
        guardSwitch.setOnCheckedChangeListener(null);
        guardSwitch.setChecked(prefs.isAccessibilityGuardEnabled());
        guardSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.setAccessibilityGuardEnabled(isChecked));

        boolean systemOn = isAccessibilityServiceEnabled();
        accessibilityStatus.setText(systemOn
                ? R.string.settings_accessibility_status_enabled
                : R.string.settings_accessibility_status_disabled);
    }

    private boolean isAccessibilityServiceEnabled() {
        String setting = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        if (setting == null) {
            return false;
        }
        ComponentName component = new ComponentName(this, TrackingAccessibilityService.class);
        String flat = component.flattenToString();
        String flatShort = component.flattenToShortString();
        return setting.contains(flat) || setting.contains(flatShort);
    }
}
