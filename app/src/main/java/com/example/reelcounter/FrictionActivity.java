package com.example.reelcounter;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.reelcounter.data.PreferencesManager;
import com.example.reelcounter.guard.LaunchGuardManager;
import com.example.reelcounter.model.LaunchLogEntry;
import com.example.reelcounter.util.PuzzleGenerator;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Random;

/**
 * Friction pause before (or on top of) a protected app.
 * <p>
 * Entry paths: (1) user tapped a protected icon in {@link LauncherActivity}, or (2) the
 * accessibility guard detected the protected package in the foreground without a valid approval
 * token. Approval tokens exist so completing friction does not immediately re-trigger the guard
 * while the same app stays visible.
 * </p>
 * <p>
 * This still cannot block every launch path on Android; it is a wellbeing aid, not a security
 * boundary.
 * </p>
 */
public class FrictionActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_PACKAGE = "extra_target_package";
    public static final String EXTRA_LAUNCH_SOURCE = "extra_launch_source";

    public static final String SOURCE_LAUNCHER = "source_launcher";
    public static final String SOURCE_GUARD = "source_guard";

    private PreferencesManager preferencesManager;
    private LaunchGuardManager launchGuardManager;
    private String targetPackage;
    private String targetLabel;
    private String launchSource;

    private int logIndex = -1;
    private boolean logFinalized;

    private boolean countdownDone;
    private boolean puzzleSolved;

    @Nullable
    private CountDownTimer countDownTimer;

    @Nullable
    private PuzzleGenerator.Puzzle activePuzzle;

    private ChipGroup reasonChipGroup;
    private TextInputEditText reasonInput;
    private TextInputEditText puzzleAnswerInput;
    private MaterialButton openButton;
    private MaterialButton puzzleVerifyButton;
    private TextView countdownText;
    private View countdownCard;
    private View puzzleCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readIntent(getIntent());
        if (targetPackage == null || targetPackage.isEmpty()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_friction);

        preferencesManager = new PreferencesManager(this);
        launchGuardManager = LaunchGuardManager.getInstance();

        PackageManager pm = getPackageManager();
        try {
            CharSequence label = pm.getApplicationLabel(pm.getApplicationInfo(targetPackage, 0));
            targetLabel = label != null ? label.toString() : targetPackage;
        } catch (PackageManager.NameNotFoundException e) {
            targetLabel = targetPackage;
        }

        MaterialToolbar toolbar = findViewById(R.id.friction_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> cancelAndFinish());

        reasonChipGroup = findViewById(R.id.reason_chip_group);
        reasonInput = findViewById(R.id.reason_input);
        puzzleAnswerInput = findViewById(R.id.puzzle_answer);
        openButton = findViewById(R.id.open_app_button);
        puzzleVerifyButton = findViewById(R.id.puzzle_verify);
        countdownText = findViewById(R.id.countdown_text);
        countdownCard = findViewById(R.id.countdown_card);
        puzzleCard = findViewById(R.id.puzzle_card);
        TextView reminder = findViewById(R.id.reminder_text);
        TextView puzzlePrompt = findViewById(R.id.puzzle_prompt);

        android.widget.ImageView icon = findViewById(R.id.target_icon);
        try {
            icon.setImageDrawable(pm.getApplicationIcon(targetPackage));
        } catch (PackageManager.NameNotFoundException e) {
            icon.setImageResource(R.mipmap.ic_launcher);
        }
        ((TextView) findViewById(R.id.target_name)).setText(targetLabel);

        boolean fromGuard = SOURCE_GUARD.equals(launchSource);
        openButton.setText(fromGuard ? R.string.friction_continue_app : R.string.friction_open_app);

        reminder.setText(pickReminderText());

        long now = System.currentTimeMillis();
        LaunchLogEntry initial = new LaunchLogEntry(targetPackage, targetLabel, now, "", false, false);
        logIndex = preferencesManager.appendLaunchLog(initial);

        boolean countdownOn = preferencesManager.isCountdownEnabled();
        boolean puzzleOn = preferencesManager.isPuzzleEnabled();

        if (!countdownOn) {
            countdownDone = true;
            countdownCard.setVisibility(View.VISIBLE);
            countdownText.setText(R.string.friction_countdown_skipped);
        } else {
            countdownDone = false;
            startCountdown(preferencesManager.getCountdownSeconds());
        }

        if (!puzzleOn) {
            puzzleSolved = true;
            puzzleCard.setVisibility(View.GONE);
        } else {
            puzzleSolved = false;
            puzzleCard.setVisibility(View.VISIBLE);
            PuzzleGenerator gen = new PuzzleGenerator();
            activePuzzle = gen.newAdditionPuzzle();
            puzzlePrompt.setText(activePuzzle.getPrompt());
            puzzleVerifyButton.setOnClickListener(v -> verifyPuzzle());
        }

        updateOpenEnabledState();

        openButton.setOnClickListener(v -> {
            if (!canOpenNow()) {
                Toast.makeText(this, R.string.friction_launch_blocked, Toast.LENGTH_SHORT).show();
                return;
            }
            if (fromGuard) {
                finalizeLog(true, false);
                launchGuardManager.approveLaunch(targetPackage);
                Intent bring = pm.getLaunchIntentForPackage(targetPackage);
                if (bring != null) {
                    bring.addCategory(Intent.CATEGORY_LAUNCHER);
                    bring.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(bring);
                }
                finish();
                return;
            }

            Intent launch = pm.getLaunchIntentForPackage(targetPackage);
            if (launch == null) {
                Toast.makeText(
                        this,
                        getString(R.string.snackbar_launch_failed, targetPackage),
                        Toast.LENGTH_SHORT
                ).show();
                finalizeLog(false, true);
                return;
            }
            finalizeLog(true, false);
            launchGuardManager.approveLaunch(targetPackage);
            launch.addCategory(Intent.CATEGORY_LAUNCHER);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
            finish();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cancelAndFinish();
            }
        });
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        launchGuardManager.setFrictionSessionActive(true);
    }

    @Override
    protected void onStop() {
        launchGuardManager.setFrictionSessionActive(false);
        super.onStop();
    }

    private void readIntent(@NonNull Intent intent) {
        targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE);
        launchSource = intent.getStringExtra(EXTRA_LAUNCH_SOURCE);
        if (launchSource == null) {
            launchSource = SOURCE_LAUNCHER;
        }
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if (!logFinalized) {
            finalizeLog(false, true);
        }
        super.onDestroy();
    }

    @NonNull
    private String currentReason() {
        String chipPart = "";
        int checkedId = reasonChipGroup.getCheckedChipId();
        if (checkedId != View.NO_ID) {
            Chip chip = reasonChipGroup.findViewById(checkedId);
            if (chip != null) {
                chipPart = chip.getText().toString().trim();
            }
        }
        String extra = "";
        if (reasonInput != null && reasonInput.getText() != null) {
            extra = reasonInput.getText().toString().trim();
        }
        if (chipPart.isEmpty()) {
            return extra;
        }
        if (extra.isEmpty()) {
            return chipPart;
        }
        return chipPart + " — " + extra;
    }

    private void cancelAndFinish() {
        finalizeLog(false, true);
        if (SOURCE_GUARD.equals(launchSource)) {
            Intent home = new Intent(this, LauncherActivity.class);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(home);
        }
        finish();
    }

    private void finalizeLog(boolean completed, boolean canceled) {
        if (logFinalized || logIndex < 0) {
            return;
        }
        logFinalized = true;
        preferencesManager.updateLaunchLog(logIndex, currentReason(), completed, canceled);
    }

    @NonNull
    private String pickReminderText() {
        List<String> notes = preferencesManager.getNotes();
        if (notes.isEmpty()) {
            return getString(R.string.friction_default_note);
        }
        int idx = new Random().nextInt(notes.size());
        String n = notes.get(idx);
        return n != null && !n.isEmpty() ? n : getString(R.string.friction_default_note);
    }

    private void startCountdown(int seconds) {
        long totalMs = seconds * 1000L;
        countdownText.setText(getString(R.string.friction_countdown, seconds));
        countDownTimer = new CountDownTimer(totalMs, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                int left = (int) Math.ceil(millisUntilFinished / 1000.0);
                countdownText.setText(getString(R.string.friction_countdown, left));
            }

            @Override
            public void onFinish() {
                countdownDone = true;
                countdownText.setText(R.string.friction_countdown_done);
                updateOpenEnabledState();
            }
        };
        countDownTimer.start();
    }

    private void verifyPuzzle() {
        if (activePuzzle == null || puzzleAnswerInput.getText() == null) {
            return;
        }
        String ans = puzzleAnswerInput.getText().toString().trim();
        if (ans.equals(activePuzzle.getExpectedAnswer())) {
            puzzleSolved = true;
            puzzleVerifyButton.setEnabled(false);
            puzzleAnswerInput.setEnabled(false);
            updateOpenEnabledState();
        } else {
            Toast.makeText(this, R.string.friction_puzzle_wrong, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean canOpenNow() {
        boolean cdOk = !preferencesManager.isCountdownEnabled() || countdownDone;
        boolean pzOk = !preferencesManager.isPuzzleEnabled() || puzzleSolved;
        return cdOk && pzOk;
    }

    private void updateOpenEnabledState() {
        openButton.setEnabled(canOpenNow());
    }
}
