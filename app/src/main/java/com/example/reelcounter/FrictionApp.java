package com.example.reelcounter;

import android.app.Application;

import com.example.reelcounter.guard.LaunchGuardManager;

/**
 * Application entry used to document guard initialization (singleton is lazy; no heavy work here).
 */
public class FrictionApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LaunchGuardManager.getInstance();
    }
}
