// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.exception.reporter;

import com.microsoft.hydralab.common.appcenter.AppCenterClient;
import com.microsoft.hydralab.common.appcenter.entity.HandledErrorLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author zhoule
 * @date 08/02/2023
 */

public class AppCenterReporter implements ExceptionReporter {
    private final Logger logger = LoggerFactory.getLogger(AppCenterReporter.class);
    private AppCenterClient appCenterClient;
    private boolean isAppCenterEnabled = false;

    public boolean isAppCenterEnabled() {
        return isAppCenterEnabled;
    }

    public void initAppCenterReporter(String appCenterSecret, String name, String versionName, String versionCode) {
        this.appCenterClient = new AppCenterClient(appCenterSecret, name, versionName, versionCode);
        isAppCenterEnabled = true;
    }

    @Override
    public void reportException(Exception e) {
        reportExceptionToAppCenter(e, Thread.currentThread());
    }

    @Override
    public void reportException(Exception e, Thread thread) {
        reportExceptionToAppCenter(e, thread);
    }


    private void reportExceptionToAppCenter(Exception e, Thread thread) {
        logger.info("Exception collected in Thread {} with message {}", thread.getName(), e.getMessage());
        if (!isAppCenterEnabled) {
            logger.warn("AppCenter is not enabled, skip reporting exception to AppCenter");
            return;
        }
        HandledErrorLog handledErrorLog = appCenterClient.createErrorLog(thread, e, true);
        try {
            appCenterClient.send(handledErrorLog);
        } catch (IOException ex) {
            logger.warn("Failed to send exception to AppCenter", ex);
        }
    }
}
