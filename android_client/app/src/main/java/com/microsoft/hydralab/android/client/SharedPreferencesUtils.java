// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.android.client;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUtils {
    public static final String PREFERENCE_FILE_KEY = BuildConfig.APPLICATION_ID + ".PREFERENCE_FILE_KEY";
    public static final String SN_KEY = "SN";

    public static String getSharedPreferencesStringValue(Context context, String key, String defaultValue) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
        return sharedPref.getString(key, defaultValue);
    }

    public static void putSharedPreferencesStringValue(Context context, String key, String value) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.apply();
    }
}
