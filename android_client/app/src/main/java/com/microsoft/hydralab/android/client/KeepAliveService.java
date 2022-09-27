// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.android.client;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;


public class KeepAliveService extends Service {
    private final IBinder mBinder = new KeepAliveService.MyBinder();
    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    public class MyBinder extends Binder {
        KeepAliveService getService() {
            return KeepAliveService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bindService(new Intent(getApplicationContext(), ScreenRecorderService.class), conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int startCommand = super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(conn);
        Intent service = new Intent(getApplicationContext(), ScreenRecorderService.class);
        service.setAction(CommandReceiver.ACTION_SIGNAL);
        startService(service);
    }


}