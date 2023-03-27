package com.microsoft.hydralab.android.client;

import android.util.Log;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AppCenterLogger extends Logger  {

    private static final String TAG = "AppCenterLogger";

    public AppCenterLogger() {
        super(TAG, null);
    }


    @Override
    public void log(Level level, String msg) {
        Log.i(TAG, msg == null ? "" : msg);
    }
}
