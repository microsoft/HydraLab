package com.microsoft.hydralab.android.client;

import android.app.Application;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

public class MainApplication extends Application {
    private static final String APP_CENTER_TEST_APP_SECRET = "2c3adb2a-241d-47b2-9385-1a39fd6f2bc0";

    public MainApplication() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Crash reporting on App Center
        AppCenter.start(this, APP_CENTER_TEST_APP_SECRET, Analytics.class, Crashes.class);
    }
}
