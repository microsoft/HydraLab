// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.android.client;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class KeepAliveJobService extends JobService {
    private JobScheduler mJobScheduler;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        startService(this);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        startService(this);
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startService(this);
        scheduleJob(startId);
        return START_STICKY;
    }

    private void scheduleJob(int startId) {
        mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo.Builder builder = new JobInfo.Builder(startId, new ComponentName(getPackageName(), KeepAliveJobService.class.getName()));
        if (Build.VERSION.SDK_INT >= 24) {
            builder.setMinimumLatency(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS); //执行的最小延迟时间
            builder.setOverrideDeadline(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);  //执行的最长延时时间
            builder.setMinimumLatency(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);
            builder.setBackoffCriteria(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS, JobInfo.BACKOFF_POLICY_LINEAR);//线性重试方案
        } else {
            builder.setPeriodic(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);
        }
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        builder.setRequiresCharging(true); // 当插入充电器，执行该任务
        mJobScheduler.schedule(builder.build());
    }

    private void startService(Context context) {
        //设置为前台服务
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForeground(13691, new Notifications(getApplicationContext()).createKeepAliveNotification());
//        }
        //启动本地服务
        Intent keepAliveIntent = new Intent(context, KeepAliveService.class);
        startService(keepAliveIntent);
    }
}
