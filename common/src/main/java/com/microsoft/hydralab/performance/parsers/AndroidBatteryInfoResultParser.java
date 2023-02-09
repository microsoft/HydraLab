// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance.parsers;

import com.microsoft.hydralab.performance.Entity.AndroidBatteryInfo;
import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceResultParser;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AndroidBatteryInfoResultParser implements PerformanceResultParser {
    protected Logger classLogger = LoggerFactory.getLogger(getClass());

    @Override
    public PerformanceTestResult parse(PerformanceTestResult performanceTestResult) {
        if (performanceTestResult == null || performanceTestResult.performanceInspectionResults == null
                || performanceTestResult.performanceInspectionResults.isEmpty()) {
            return null;
        }

        List<PerformanceInspectionResult> inspectionResults = performanceTestResult.performanceInspectionResults;
        for (PerformanceInspectionResult inspectionResult : inspectionResults) {
            File logFile = inspectionResult.rawResultFile;
            AndroidBatteryInfo batteryInfo = parseRawResultFile(logFile, inspectionResult.inspection.appId);
            if (batteryInfo != null) {
                batteryInfo.setAppPackageName(inspectionResult.inspection.appId);
                batteryInfo.setDescription(inspectionResult.inspection.description);
                batteryInfo.setTimeStamp(inspectionResult.timestamp);
            }
            inspectionResult.parsedData = batteryInfo;
        }

        // Use the battery usage at the end of the test as a summary
        performanceTestResult.resultSummary = inspectionResults.get(inspectionResults.size() - 1).parsedData;
        return performanceTestResult;
    }

    public AndroidBatteryInfo parseRawResultFile(File rawFile, String packageName) {
        if (!rawFile.isFile() || !rawFile.exists()) return null;

        AndroidBatteryInfo batteryInfo = new AndroidBatteryInfo();
        float totalUsage = 0.0f;
        float appUsage = 0.0f;
        // 1. android version lower than 12   2. android version higher than or equal to 12
        int mode = 1;
        String uid = null;
        boolean isCalculateTotalUsage = false;

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(rawFile), StandardCharsets.UTF_8));
            List<String> contents = new ArrayList<>();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                inputLine = inputLine.trim().toLowerCase();
                contents.add(inputLine);
            }
            in.close();

            //Find Uid
            for (String line : contents) {
                if (Pattern.matches("u0.*:", line)) {
                    uid = line.split(":")[0];
                }
            }
            if (uid == null) {
                classLogger.error(String.format("Could not parse the battery usage of %s in battery info file: %s", packageName, rawFile.getAbsolutePath()));
                return null;
            }


            for (String line : contents) {
                //Parse total usage
                if (mode == 1) {
                    if (Pattern.matches("capacity:.*computed drain:.*", line)) {
                        // find total usage
                        totalUsage = Float.parseFloat(line.split("computed drain: ")[1].split(",")[0]);
                        if (totalUsage == 0.0) {
                            mode = 2;
                        }
                    }
                } else {
                    if (Pattern.matches(".*global.*", line)) {
                        isCalculateTotalUsage = true;
                        continue;
                    }
                    if (isCalculateTotalUsage && line.startsWith("uid")) {
                        isCalculateTotalUsage = false;
                    }
                    if (isCalculateTotalUsage) {
                        if (!line.startsWith("screen:")) {
                            totalUsage += Float.parseFloat(line.split(" ")[1].trim());
                        }
                    }
                }

                // Parse app usage
                if (Pattern.matches("uid " + uid + ":.*", line)) {
                    appUsage = Float.parseFloat(line.split(": ")[1].split(" ")[0]);
                    batteryInfo.setAppUsage(appUsage);
                    batteryInfo.setRatio(appUsage / totalUsage);
                    batteryInfo.setCpu(parseAppDetails(line, "cpu="));
                    batteryInfo.setSystemService(parseAppDetails(line, "system_services="));
                    batteryInfo.setScreen(parseAppDetails(line, "screen="));
                    batteryInfo.setWakeLock(parseAppDetails(line, "wakelock="));
                    batteryInfo.setWifi(parseAppDetails(line, "wifi="));
                }

                batteryInfo.setTotal(totalUsage);
            }
        } catch (IOException e) {
            classLogger.error("Failed to parse the battery info file: " + rawFile.getAbsolutePath());
            return null;
        }

        return batteryInfo;

    }

    float parseAppDetails(String line, String keyword) {
        if (!line.contains(keyword)) return 0;
        return Float.parseFloat(line.split(keyword)[1].split(" ")[0]);
    }

}
