package com.example.reelcounter;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.reelcounter.theme.ThemeApplier;

/**
 * Applies persisted theme before content inflation.
 */
public abstract class BaseThemedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeApplier.apply(this);
        super.onCreate(savedInstanceState);
    }
}
