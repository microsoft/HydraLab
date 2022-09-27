// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.android.client;

import static android.os.Build.VERSION_CODES.O;
import static com.microsoft.hydralab.android.client.CommandReceiver.ACTION_STOP;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

class Notifications {
    public static final int id = 0x1fff;
    private static final String CHANNEL_ID = "Recording";
    private static final String CHANNEL_NAME = "Screen Recorder Notifications";
    private final Context context;

    private NotificationManager mManager;
    private Notification.Action mStopAction;
    private Notification.Builder mBuilder;

    Notifications(Context context) {
        this.context = context;
        if (Build.VERSION.SDK_INT >= O) {
            createNotificationChannel();
        }
    }

    public Notification createRecordingNotification() {
        return getBuilder(context.getString(R.string.service_title), false)
                .build();
    }


    private Notification.Builder getBuilder(String text, boolean needAction) {
        if (mBuilder == null) {
            Notification.Builder builder = new Notification.Builder(context)
                    .setContentTitle(text)
                    .setOngoing(true)
                    .setLocalOnly(true)
                    .setOnlyAlertOnce(true)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_stat_recording);
            if (Build.VERSION.SDK_INT >= O) {
                builder.setChannelId(CHANNEL_ID)
                        .setUsesChronometer(true);
            }
            if (needAction) {
                builder.addAction(stopAction());
            }
            mBuilder = builder;
        }
        return mBuilder;
    }

    @TargetApi(O)
    private void createNotificationChannel() {
        NotificationChannel channel =
                new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
        channel.setShowBadge(false);
        getNotificationManager().createNotificationChannel(channel);
    }

    private Notification.Action stopAction() {
        if (mStopAction == null) {
            Intent intent = new Intent(ACTION_STOP).setPackage(context.getPackageName());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1,
                    intent, PendingIntent.FLAG_ONE_SHOT);
            mStopAction = new Notification.Action(android.R.drawable.ic_media_pause, context.getString(R.string.stop), pendingIntent);
        }
        return mStopAction;
    }

    NotificationManager getNotificationManager() {
        if (mManager == null) {
            mManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mManager;
    }

    public void updateNotification(int id, Notification idleNotification) {
        getNotificationManager().notify(id, idleNotification);
    }
}
