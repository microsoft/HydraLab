// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.scheduled;

import com.microsoft.hydralab.agent.config.AppOptions;
import com.microsoft.hydralab.agent.service.DeviceControlService;
import com.microsoft.hydralab.agent.socket.AgentWebSocketClient;
import com.microsoft.hydralab.common.management.device.DeviceType;
import com.microsoft.hydralab.common.util.DateUtil;
import com.microsoft.hydralab.common.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.io.File;
import java.util.Date;

@Component
@Profile("!test")
public class ScheduledDeviceControlTasks {

    Logger logger = LoggerFactory.getLogger(getClass());
    @Resource
    DeviceControlService deviceControlService;
    @Resource
    AppOptions appOptions;
    @Resource
    AgentWebSocketClient agentWebSocketClient;

    //check connection /5s
    @Scheduled(cron = "*/5 * * * * *")
    public void scheduledCheckWebSocketConnection() {
        if (agentWebSocketClient.isConnectionActive()) {
            return;
        }
        logger.info("Try reconnecting the WS server");
        agentWebSocketClient.reconnect();
    }

    @Scheduled(cron = "0 10 6 ? * *")
    public void scheduleCleanBuildSource() {
        logger.info("schedule clean build APK");
        clearFile(appOptions.getTestPackageLocation());
        logger.info("schedule clean test result");
        clearFile(appOptions.getTestCaseResultLocation());
        logger.info("schedule clean error output");
        clearFile(appOptions.getErrorStorageLocation());
    }

    @Scheduled(cron = "${app.device.auto-reboot.android.cron}")
    public void scheduleRestartDevice() {
        logger.info("schedule reboot android device");
        deviceControlService.rebootDevices(DeviceType.ANDROID);
    }

    public void clearFile(String folderPath) {
        File buildSourceFile = new File(folderPath);
        File[] yearFiles = buildSourceFile.listFiles();
        for (File year : yearFiles) {
            try {
                Integer.valueOf(year.getName());
            } catch (Exception e) {
                continue;
            }
            File[] monthFiles = year.listFiles();
            if (monthFiles == null) {
                FileUtil.deleteFile(year);
                continue;
            }
            for (File month : monthFiles) {
                File[] dateFiles = month.listFiles();
                if (dateFiles == null) {
                    logger.info("going to delete {}", month);
                    FileUtil.deleteFile(month);
                    continue;
                }
                for (File date : dateFiles) {
                    if (isSevenDaysBefore(date.getAbsolutePath())) {
                        logger.info("going to delete {}", date);
                        FileUtil.deleteFile(date);
                    }
                }
                dateFiles = month.listFiles();
                if (dateFiles == null || dateFiles.length == 0) {
                    FileUtil.deleteFile(month);
                }
            }
            monthFiles = year.listFiles();
            if (monthFiles == null || monthFiles.length == 0) {
                FileUtil.deleteFile(year);
            }
        }
    }

    private boolean isSevenDaysBefore(String filePath) {
        String fileSeparator = File.separator;
        if ("\\".equals(fileSeparator)) {
            fileSeparator = "\\\\";
        }
        String[] spilt = filePath.split(fileSeparator);
        int length = spilt.length;
        if (length >= 3) {
            String data = spilt[length - 1];
            String month = spilt[length - 2];
            String year = spilt[length - 3];
            Date dateToday = new Date(System.currentTimeMillis());
            Date fileDate = null;
            try {
                fileDate = DateUtil.stringToDate(year, month, data);
            } catch (Exception e) {
                logger.warn("delete file " + filePath);
            }
            return fileDate != null && DateUtil.betweenDay(dateToday, fileDate) > 7;
        }
        return false;
    }

}
