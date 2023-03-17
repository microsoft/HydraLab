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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AndroidMemoryInfoResultParser implements PerformanceResultParser {
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
        for (PerformanceInspectionResult inspectionResult : inspectionResults) {
            File logFile = inspectionResult.rawResultFile;
            long[] memInfos = parseRawResultFile(logFile);
            inspectionResult.parsedData = buildMemoryInfo(inspectionResult.inspection.appId, inspectionResult.inspection.description, inspectionResult.timestamp, memInfos);
        }

        performanceTestResult.setResultSummary(buildMemoryAverageSummary(inspectionResults));

        return performanceTestResult;
    }

    private AndroidMemoryInfo buildMemoryAverageSummary(List<PerformanceInspectionResult> inspectionResults) {
        if (inspectionResults == null || inspectionResults.isEmpty()) {
            return null;
        }

        AndroidMemoryInfo averageMemoryInfo = new AndroidMemoryInfo();
        List<AndroidMemoryInfo> memoryInfos = new ArrayList<>();
        for (PerformanceInspectionResult inspectionResult : inspectionResults) {
            if (inspectionResult.parsedData != null) {
                memoryInfos.add((AndroidMemoryInfo) inspectionResult.parsedData);
            }
        }

        averageMemoryInfo.setJavaHeapPss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getJavaHeapPss).average().orElse(-1)));
        averageMemoryInfo.setJavaHeapRss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getJavaHeapRss).average().orElse(-1)));
        averageMemoryInfo.setNativeHeapPss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getNativeHeapPss).average().orElse(-1)));
        averageMemoryInfo.setNativeHeapRss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getNativeHeapRss).average().orElse(-1)));
        averageMemoryInfo.setCodePss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getCodePss).average().orElse(-1)));
        averageMemoryInfo.setCodeRss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getCodeRss).average().orElse(-1)));
        averageMemoryInfo.setStackPss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getStackPss).average().orElse(-1)));
        averageMemoryInfo.setStackRss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getStackRss).average().orElse(-1)));
        averageMemoryInfo.setGraphicsPss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getGraphicsPss).average().orElse(-1)));
        averageMemoryInfo.setGraphicsRss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getGraphicsRss).average().orElse(-1)));
        averageMemoryInfo.setPrivateOtherPss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getPrivateOtherPss).average().orElse(-1)));
        averageMemoryInfo.setPrivateOtherRss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getPrivateOtherRss).average().orElse(-1)));
        averageMemoryInfo.setSystemPss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getSystemPss).average().orElse(-1)));
        averageMemoryInfo.setStackRss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getSystemRss).average().orElse(-1)));
        averageMemoryInfo.setUnknownPss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getUnknownPss).average().orElse(-1)));
        averageMemoryInfo.setUnknownRss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getUnknownRss).average().orElse(-1)));
        averageMemoryInfo.setTotalPss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getTotalPss).average().orElse(-1)));
        averageMemoryInfo.setTotalRss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getTotalRss).average().orElse(-1)));
        averageMemoryInfo.setTotalSwapPss((long) (memoryInfos.stream().mapToLong(AndroidMemoryInfo::getTotalSwapPss).average().orElse(-1)));

        return averageMemoryInfo;
    }

    private AndroidMemoryInfo buildMemoryInfo(String packageName, String description, long timestamp, long[] memInfos) {
        if (memInfos == null || memInfos.length != 19) {
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
        long[] memoryValueArr = new long[19];
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
