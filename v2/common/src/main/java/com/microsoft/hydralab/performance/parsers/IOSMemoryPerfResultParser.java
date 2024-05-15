package com.microsoft.hydralab.performance.parsers;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.microsoft.hydralab.common.util.IOSPerfTestHelper;
import com.microsoft.hydralab.performance.PerformanceInspection;
import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceResultParser;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import com.microsoft.hydralab.performance.entity.IOSMemoryPerfInfo;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class IOSMemoryPerfResultParser implements PerformanceResultParser {

    @Override
    public PerformanceTestResult parse(PerformanceTestResult performanceTestResult, Logger logger) {
        int inspectionSize = performanceTestResult.performanceInspectionResults.size();
        if (inspectionSize > 0) {
            List<PerformanceInspectionResult> oldInspectionResults = performanceTestResult.performanceInspectionResults;
            PerformanceInspectionResult firstInspectionResult = oldInspectionResults.get(0);
            PerformanceInspection firstInspection = firstInspectionResult.inspection;
            String inspectionKey = firstInspection.inspectionKey;
            String appId = firstInspection.appId;
            String deviceIdentifier = firstInspection.deviceIdentifier;
            File resultFolder = firstInspection.resultFolder;
            File rawFile = IOSPerfTestHelper.getInstance().getResultFile(inspectionKey);
            IOSPerfTestHelper.getInstance().stop(inspectionKey);
            if (rawFile != null) {
                try (BufferedReader bufferedReader = new BufferedReader(new FileReader(rawFile))) {
                    String line;
                    int currentInspection = 0;
                    List<PerformanceInspectionResult> newPerfInspectionResults = new CopyOnWriteArrayList<>();

                    long timestampForThisLine = 0;
                    long timestampForLastLine;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.startsWith("memory")) {
                            String subJsonLine = null;
                            try {
                                subJsonLine = line.substring(line.indexOf("{")).replace("'", "\"").replace("None", "null");
                                IOSMemoryPerfInfo energyInfo = JSON.parseObject(subJsonLine, IOSMemoryPerfInfo.class);
                                energyInfo.setAppPackageName(appId);
                                timestampForLastLine = timestampForThisLine;
                                timestampForThisLine = energyInfo.getTimeStamp();
                                String description = "";
                                if ((currentInspection < inspectionSize)
                                        && oldInspectionResults.get(currentInspection).timestamp > timestampForLastLine
                                        && oldInspectionResults.get(currentInspection).timestamp < timestampForThisLine) {
                                    description = oldInspectionResults.get(currentInspection).inspection.description;
                                    currentInspection++;
                                }

                                energyInfo.setDescription(description);

                                PerformanceInspection newInspection = PerformanceInspection.createIOSEnergyInspection(appId, deviceIdentifier, description, false);
                                newInspection.resultFolder = resultFolder;
                                PerformanceInspectionResult result = new PerformanceInspectionResult(rawFile, newInspection, timestampForThisLine);
                                result.parsedData = energyInfo;
                                newPerfInspectionResults.add(result);
                            } catch (JSONException e) {
                                logger.info("Json parse error, " + subJsonLine);
                                logger.warn(Arrays.toString(e.getStackTrace()));
                            }
                        }
                    }
                    performanceTestResult.performanceInspectionResults = newPerfInspectionResults;
                    performanceTestResult.setResultSummary(getAverageIOSMemoryPerfInfo(newPerfInspectionResults));
                    oldInspectionResults.clear();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return performanceTestResult;
    }

    private IOSMemoryPerfInfo getAverageIOSMemoryPerfInfo(List<PerformanceInspectionResult> inspectionResults) {
        if (inspectionResults == null || inspectionResults.size() == 0) {
            return null;
        }

        IOSMemoryPerfInfo averageIOSMemoryPerfInfo = new IOSMemoryPerfInfo();
        averageIOSMemoryPerfInfo.setAppPackageName(inspectionResults.get(0).inspection.appId);
        averageIOSMemoryPerfInfo.setTimeStamp(System.currentTimeMillis());
        double averageMemoryMB = 0;
        for (int i = 0; i < inspectionResults.size(); i++) {
            double memoryMB = ((IOSMemoryPerfInfo) (inspectionResults.get(i).parsedData)).getMemoryMB();
            averageMemoryMB = memoryMB / (i + 1) + averageMemoryMB * i / (i + 1);
        }
        averageIOSMemoryPerfInfo.setMemoryMB((float) averageMemoryMB);
        return averageIOSMemoryPerfInfo;
    }
}
