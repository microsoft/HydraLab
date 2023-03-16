package com.microsoft.hydralab.performance.parsers;

import com.alibaba.fastjson.JSON;
import com.microsoft.hydralab.common.util.IOSPerfTestHelper;
import com.microsoft.hydralab.performance.PerformanceInspection;
import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceResultParser;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import com.microsoft.hydralab.performance.entity.IOSMemoryPerfInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class IOSMemoryPerfResultParser implements PerformanceResultParser {
    @Override
    public PerformanceTestResult parse(PerformanceTestResult performanceTestResult) {
        int inspectionSize = performanceTestResult.performanceInspectionResults.size();
        if (inspectionSize > 0) {
            List<PerformanceInspectionResult> oldInspectionResults = performanceTestResult.performanceInspectionResults;
            PerformanceInspectionResult firstInspectionResult = oldInspectionResults.get(0);
            PerformanceInspection firstInspection = firstInspectionResult.inspection;
            String inspectionKey = firstInspection.inspectionKey;
            String appId = firstInspection.appId;
            String deviceIdentifier = firstInspection.deviceIdentifier;
            File resultFolder = firstInspection.resultFolder;
            IOSPerfTestHelper.getInstance().stop(inspectionKey);
            File rawFile = IOSPerfTestHelper.getInstance().getResultFile(inspectionKey);
            long startTimeStamp = IOSPerfTestHelper.getInstance().getStartTime(inspectionKey);
            if (rawFile != null) {
                try (BufferedReader bufferedReader = new BufferedReader(new FileReader(rawFile))) {
                    String line;
                    int currentInspection = 0;
                    List<PerformanceInspectionResult> newPerfInspectionResults = new CopyOnWriteArrayList<>();

                    long timestampForThisLine = 0;
                    long timestampForLastLine;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.startsWith("memory")) {
                            IOSMemoryPerfInfo energyInfo = JSON.parseObject(line.substring(line.indexOf("{")).replace("'", "\""), IOSMemoryPerfInfo.class);
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
                            PerformanceInspectionResult result = new PerformanceInspectionResult(rawFile, newInspection);
                            result.parsedData = energyInfo;
                            newPerfInspectionResults.add(result);
                        }
                    }
                    performanceTestResult.performanceInspectionResults = newPerfInspectionResults;
                    oldInspectionResults.clear();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return performanceTestResult;
    }
}
