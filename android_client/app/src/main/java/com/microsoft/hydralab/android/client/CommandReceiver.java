// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.android.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CommandReceiver extends BroadcastReceiver {
    static final String ACTION_STOP = BuildConfig.APPLICATION_ID + ".action.STOP";
    static final String ACTION_SIGNAL = BuildConfig.APPLICATION_ID + ".action.SIGNAL";
    static final String ACTION_START = BuildConfig.APPLICATION_ID + ".action.START";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            Intent service = new Intent(context, ScreenRecorderService.class);
            service.setAction(ACTION_STOP);
            context.startService(service);
            return;
        }

        Intent service = new Intent(context, ScreenRecorderService.class);
        if (intent != null) {
            Log.i("CommandReceiver", intent.getAction() + " receive");
            service.setAction(intent.getAction());
        }
        context.startService(service);
    }
}