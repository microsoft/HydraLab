// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.parsers;

import com.google.common.base.Strings;
import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceResultParser;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import com.microsoft.hydralab.performance.entity.AndroidMemoryInfo;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AndroidMemoryInfoResultParser implements PerformanceResultParser {
    private static final int MEM_INFO_LENGTH = 19;
    private static final String SUMMARY_DESCRIPTION = "Android memory info summary";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final Map<String, Integer> MEMORY_FILE_TO_DB_INDEX_MAP = new HashMap<>() {
        {
            put("Java Heap", 0);
            put("Native Heap", 1);
            put("Code", 2);
            put("Stack", 3);
            put("Graphics", 4);
            put("Private Other", 5);
            put("System", 6);
            put("Unknown", 7);
        }
    };

    @Override
    public PerformanceTestResult parse(PerformanceTestResult performanceTestResult) {
        if (performanceTestResult == null || performanceTestResult.performanceInspectionResults == null
                || performanceTestResult.performanceInspectionResults.isEmpty()) {
            return null;
        }
        List<PerformanceInspectionResult> inspectionResults = performanceTestResult.performanceInspectionResults;
        double[] averageMemoryInfo = new double[MEM_INFO_LENGTH];
        Arrays.fill(averageMemoryInfo, 0.0);
        int validDataSize = 0;
        for (PerformanceInspectionResult inspectionResult : inspectionResults) {
            File logFile = inspectionResult.rawResultFile;
            long[] memInfos = parseRawResultFile(logFile);
            inspectionResult.parsedData = buildMemoryInfo(inspectionResult.inspection.appId, inspectionResult.inspection.description, inspectionResult.timestamp, memInfos);
            if (isValidMem(memInfos)) {
                updateAverageMem(averageMemoryInfo, memInfos, validDataSize);
                validDataSize++;
            }
        }

        performanceTestResult.setResultSummary(buildAverageMemoryInfo(averageMemoryInfo, inspectionResults.get(0), SUMMARY_DESCRIPTION));

        return performanceTestResult;
    }

    private void updateAverageMem(double[] averageMemoryInfo, long[] memInfoArray, int index) {
        if (memInfoArray == null) {
            return;
        }
        for (int i = 0; i < MEM_INFO_LENGTH; i++) {
            averageMemoryInfo[i] = averageMemoryInfo[i] * index / (index + 1) + (double) memInfoArray[i] / (index + 1);
        }
    }

    private boolean isValidMem(long[] memInfo) {
        if (memInfo == null) {
            return false;
        }

        for (long i : memInfo) {
            if (i != -1) {
                return true;
            }
        }
        return false;
    }

    private AndroidMemoryInfo buildAverageMemoryInfo(double[] averageMemoryInfo, PerformanceInspectionResult inspectionResult, String description) {
        long[] averageMemoryInfoLong = new long[averageMemoryInfo.length];
        boolean isValidSummary = false;
        for (int i = 0; i < averageMemoryInfo.length; i++) {
            if (averageMemoryInfo[i] > 0) {
                isValidSummary = true;
            }
            averageMemoryInfoLong[i] = Math.round(averageMemoryInfo[i]);
        }
        return isValidSummary ? buildMemoryInfo(inspectionResult.inspection.appId, description, inspectionResult.timestamp, averageMemoryInfoLong) : null;
    }

    private AndroidMemoryInfo buildMemoryInfo(String packageName, String description, long timestamp, long[] memInfos) {
        if (memInfos == null || memInfos.length != MEM_INFO_LENGTH) {
            return null;
        }
        AndroidMemoryInfo androidMemoryInfo = new AndroidMemoryInfo();
        androidMemoryInfo.setAppPackageName(packageName);
        androidMemoryInfo.setDescription(description);
        androidMemoryInfo.setTimeStamp(timestamp);
        androidMemoryInfo.setJavaHeapPss(memInfos[0]);
        androidMemoryInfo.setJavaHeapRss(memInfos[1]);
        androidMemoryInfo.setNativeHeapPss(memInfos[2]);
        androidMemoryInfo.setNativeHeapRss(memInfos[3]);
        androidMemoryInfo.setCodePss(memInfos[4]);
        androidMemoryInfo.setCodeRss(memInfos[5]);
        androidMemoryInfo.setStackPss(memInfos[6]);
        androidMemoryInfo.setStackRss(memInfos[7]);
        androidMemoryInfo.setGraphicsPss(memInfos[8]);
        androidMemoryInfo.setGraphicsRss(memInfos[9]);
        androidMemoryInfo.setPrivateOtherPss(memInfos[10]);
        androidMemoryInfo.setPrivateOtherRss(memInfos[11]);
        androidMemoryInfo.setSystemPss(memInfos[12]);
        androidMemoryInfo.setSystemRss(memInfos[13]);
        androidMemoryInfo.setUnknownPss(memInfos[14]);
        androidMemoryInfo.setUnknownRss(memInfos[15]);
        androidMemoryInfo.setTotalPss(memInfos[16]);
        androidMemoryInfo.setTotalRss(memInfos[17]);
        androidMemoryInfo.setTotalSwapPss(memInfos[18]);
        return androidMemoryInfo;
    }

    private long[] parseRawResultFile(File rawFile) {
        String line;
        long[] memoryValueArr = new long[MEM_INFO_LENGTH];
        Arrays.fill(memoryValueArr, -1);
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(rawFile))) {
            while ((line = bufferedReader.readLine()) != null) {
                if (line.trim().startsWith("App Summary")) {
                    // PSS title line, used to anchor target index offset
                    line = bufferedReader.readLine();
                    int pssEndOffset = line.indexOf(")");
                    int rssEndOffset = line.lastIndexOf(")");

                    // move to data line
                    bufferedReader.readLine();
                    line = bufferedReader.readLine();
                    while (line != null) {
                        String lineStr = line.trim();

                        if (lineStr.startsWith("TOTAL ")) {
                            if (line.contains("TOTAL PSS:")) {
                                String pssValue = line.split("TOTAL PSS:")[1].split(" +")[1];
                                memoryValueArr[16] = NumberUtils.toLong(pssValue, -1);
                            }
                            if (line.contains("TOTAL RSS:")) {
                                String rssValue = line.split("TOTAL RSS:")[1].split(" +")[1];
                                memoryValueArr[17] = NumberUtils.toLong(rssValue, -1);
                            }
                            if (line.contains("TOTAL SWAP PSS:")) {
                                memoryValueArr[18] = NumberUtils.toLong(line.split("TOTAL SWAP PSS:")[1].split(" +")[1], -1);
                            }
                            break;
                        } else if (!Strings.isNullOrEmpty(lineStr)) {
                            String[] keyValue = lineStr.split(":");
                            String key = keyValue[0];
                            String values = keyValue[1];

                            // int in map to calculate offset in memoryValueArr array (typeIndex * 2 +0/+1 (implies PSS/RSS correspondingly))
                            int typeIndex = MEMORY_FILE_TO_DB_INDEX_MAP.get(key);

                            // for current memory type, PSS data exists
                            if (line.charAt(pssEndOffset) != ' ') {
                                memoryValueArr[typeIndex * 2] = NumberUtils.toLong(values.split(" +")[1]);

                                // for current memory type, RSS data exists
                                if (line.length() > rssEndOffset && line.charAt(rssEndOffset) != ' ') {
                                    memoryValueArr[typeIndex * 2 + 1] = NumberUtils.toLong(values.split(" +")[2]);
                                }
                            } else if (line.length() > rssEndOffset && line.charAt(rssEndOffset) != ' ') {
                                // for current memory type PSS data doesn't exist and RSS data exists
                                memoryValueArr[typeIndex * 2 + 1] = NumberUtils.toLong(values.split(" +")[1]);
                            }
                        }

                        line = bufferedReader.readLine();
                    }
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
        return memoryValueArr;
    }
}
