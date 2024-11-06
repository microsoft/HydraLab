// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.android.client;

import android.app.Application;
import android.util.Log;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.AbstractCrashesListener;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.crashes.model.ErrorReport;

import java.util.logging.Logger;

public class MainApplication extends Application {
    private static final String APP_CENTER_TEST_APP_SECRET = "";

    public MainApplication() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AbstractCrashesListener crashesListener = new AbstractCrashesListener() {
            @Override
            public boolean shouldProcess(ErrorReport report) {
                return true;
            }
        };
        Crashes.setListener(crashesListener);
        if (BuildConfig.DEBUG) {
            AppCenter.setLogLevel(Log.VERBOSE);
        }
        AppCenter.setLogger(new AppCenterLogger());
        // Crash reporting on App Center
        AppCenter.start(this, APP_CENTER_TEST_APP_SECRET, Analytics.class, Crashes.class);
    }
}
