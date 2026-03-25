package com.example.reelcounter;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

/**
 * Brief branded entry with the app motto; then continues to the launcher.
 */
public class SplashActivity extends BaseThemedActivity {

    private static final long DISPLAY_MS = 2000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean navigated;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        handler.postDelayed(this::goHome, DISPLAY_MS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void goHome() {
        if (isFinishing() || navigated) {
            return;
        }
        navigated = true;
        startActivity(new Intent(this, LauncherActivity.class));
        finish();
    }
}
