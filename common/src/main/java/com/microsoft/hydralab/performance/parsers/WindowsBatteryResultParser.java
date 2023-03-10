// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance.parsers;

import com.microsoft.hydralab.common.util.MachineInfoUtils;
import com.microsoft.hydralab.performance.Entity.WindowsBatteryParsedData;
import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceResultParser;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import lombok.NonNull;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * To change the default sampling interval, from an elevated command line, run: `reg add
 * "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\SRUM\Parameters" /v Tier1Period /t REG_DWORD /d
 * <seconds> /f`. Then restarting the PC to make it take effect.
 *
 * NOTE:
 * 1. To minimize the overhead, you should gather data as infrequently as possible for your measurement. Changing the
 * sampling rate affects all SRUM providers and should only be done for testing purposes. Changing Tier1Period to < 10
 * seconds or > 180 seconds will result in the default value of 60 seconds being used.
 * 2. It is recommended to start parsing the result file 20 seconds after the last power data collection. This is
 * because even though the execution of the corresponding powershell has done, the results may not have been written to
 * the hard disk completely because the OS has not yet completed the last step of writing due to big size of the result
 * file is too large.
 */

public class WindowsBatteryResultParser implements PerformanceResultParser {

    private final static String APP_ID_KEYWORD = "YourPhone";
    private final static String DELIMITER = ", ";

    private final Logger classLogger = LoggerFactory.getLogger(getClass());

    @Override
    public PerformanceTestResult parse(PerformanceTestResult performanceTestResult) {
        if (!MachineInfoUtils.isOnWindowsLaptop()) {
            classLogger.error("Windows battery test must be run on Windows Laptop!");
            return null;
        }

        try {
            // Wait 20 seconds for the results to be completely written to the hard disk.
            Thread.sleep(20 * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        boolean baseLineFound = false;
        String baseLine = "";

        for (PerformanceInspectionResult inspectionResult : performanceTestResult.performanceInspectionResults)
        {
            if (inspectionResult == null) {
                continue;
            }

            try (ReversedLinesFileReader reversedReader = new ReversedLinesFileReader(inspectionResult.rawResultFile,
                    StandardCharsets.UTF_8);) {
                WindowsBatteryParsedData windowsBatteryParsedData = new WindowsBatteryParsedData();
                inspectionResult.parsedData = windowsBatteryParsedData;
                WindowsBatteryParsedData.WindowsBatteryMetrics summarizedWindowsBatteryMetrics =
                        new WindowsBatteryParsedData.WindowsBatteryMetrics();
                windowsBatteryParsedData.setSummarizedWindowsBatteryMetrics(summarizedWindowsBatteryMetrics);

                Map<String, Integer> columnNameToIndexMap = getColumnNameToIndexMap(inspectionResult.rawResultFile);
                String line;

                while ((line = reversedReader.readLine()) != null)
                {
                    if (!line.contains(APP_ID_KEYWORD))
                    {
                        continue;
                    }

                    if (!baseLineFound)
                    {
                        baseLineFound = true;
                        baseLine = line;
                        break;
                    }

                    if (line.equals(baseLine))
                    {
                        break;
                    }

                    String[] fieldValues = line.split(DELIMITER);
                    WindowsBatteryParsedData.WindowsBatteryMetrics windowsBatteryMetrics = getWindowsBatteryMetrics(
                            fieldValues, columnNameToIndexMap);
                    windowsBatteryParsedData.getWindowsBatteryMetricsList().add(windowsBatteryMetrics);
                    summarizedWindowsBatteryMetrics.accumulate(windowsBatteryMetrics);

                    String appId = getOneStringField(fieldValues, "AppId", columnNameToIndexMap);
                    windowsBatteryParsedData.getAppIdSet().add(appId);
                }

            } catch (IOException e) {
                classLogger.error("Failed to read data from the file.", e);
            }
        }

        return performanceTestResult;
    }

    private Map<String, Integer> getColumnNameToIndexMap(File file)
    {
        Map<String, Integer> columnNameToIndexMap = new ConcurrentHashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String columnNamesLine = reader.readLine();
            String[] columnNamesList = columnNamesLine.split(DELIMITER);

            int index = 0;
            for (String columnName : columnNamesList) {
                columnNameToIndexMap.put(columnName, index++);
            }

        } catch (IOException e) {
            classLogger.error("Failed to read data from the file.", e);
        }

        return columnNameToIndexMap;
    }

    private WindowsBatteryParsedData.WindowsBatteryMetrics getWindowsBatteryMetrics(String[] fieldValues,
                                                                                    Map<String, Integer>columnNameToIndexMap)
    {
        WindowsBatteryParsedData.WindowsBatteryMetrics windowsBatteryMetrics =
                new WindowsBatteryParsedData.WindowsBatteryMetrics();

        windowsBatteryMetrics.setEnergyLoss(
                getOneMetric(fieldValues, WindowsBatteryParsedData.METRICS_NAME[0], columnNameToIndexMap));
        windowsBatteryMetrics.setCPUEnergyConsumption(
                getOneMetric(fieldValues, WindowsBatteryParsedData.METRICS_NAME[1], columnNameToIndexMap));
        windowsBatteryMetrics.setSocEnergyConsumption(
                getOneMetric(fieldValues, WindowsBatteryParsedData.METRICS_NAME[2], columnNameToIndexMap));
        windowsBatteryMetrics.setDisplayEnergyConsumption(
                getOneMetric(fieldValues, WindowsBatteryParsedData.METRICS_NAME[3], columnNameToIndexMap));
        windowsBatteryMetrics.setDiskEnergyConsumption(
                getOneMetric(fieldValues, WindowsBatteryParsedData.METRICS_NAME[4], columnNameToIndexMap));
        windowsBatteryMetrics.setNetworkEnergyConsumption(
                getOneMetric(fieldValues, WindowsBatteryParsedData.METRICS_NAME[5], columnNameToIndexMap));
        windowsBatteryMetrics.setMBBEnergyConsumption(
                getOneMetric(fieldValues, WindowsBatteryParsedData.METRICS_NAME[6], columnNameToIndexMap));
        windowsBatteryMetrics.setOtherEnergyConsumption(
                getOneMetric(fieldValues, WindowsBatteryParsedData.METRICS_NAME[7], columnNameToIndexMap));
        windowsBatteryMetrics.setEmiEnergyConsumption(
                getOneMetric(fieldValues, WindowsBatteryParsedData.METRICS_NAME[8], columnNameToIndexMap));
        windowsBatteryMetrics.setCPUEnergyConsumptionWorkOnBehalf(
                getOneMetric(fieldValues, WindowsBatteryParsedData.METRICS_NAME[9], columnNameToIndexMap));
        windowsBatteryMetrics.setCPUEnergyConsumptionAttributed(
                getOneMetric(fieldValues, WindowsBatteryParsedData.METRICS_NAME[10], columnNameToIndexMap));
        windowsBatteryMetrics.setTotalEnergyConsumption(
                getOneMetric(fieldValues, WindowsBatteryParsedData.METRICS_NAME[11], columnNameToIndexMap));

        windowsBatteryMetrics.setTimeStamp(getOneStringField(fieldValues, "TimeStamp", columnNameToIndexMap));
        return windowsBatteryMetrics;
    }

    private long getOneMetric(String[] fieldValues, String metricName, Map<String, Integer>columnNameToIndexMap)
    {
        int index = columnNameToIndexMap.getOrDefault(metricName, -1);
        if (index != -1) {
            try {
                return Long.parseLong(fieldValues[index]);
            } catch (NumberFormatException e) {
                classLogger.error("Failed to parse long data from the field value: " + fieldValues[index], e);
                return 0;
            }
        }
        return 0;
    }

    @NonNull
    private String getOneStringField(String[] fieldValues, String fieldName, Map<String, Integer>columnNameToIndexMap)
    {
        int index = columnNameToIndexMap.getOrDefault(fieldName, -1);
        if (index != -1)
        {
            return fieldValues[index];
        }
        return "";
    }

}
