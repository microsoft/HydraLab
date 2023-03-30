package com.microsoft.hydralab.performance.parsers;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.util.IOSPerfTestHelper;
import com.microsoft.hydralab.performance.PerformanceInspection;
import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceResultParser;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import com.microsoft.hydralab.performance.entity.IOSEnergyGaugeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class IOSEnergyGaugeResultParser implements PerformanceResultParser {
    private static final long ONE_SECOND_TIMESTAMP = 1000;
    protected Logger classLogger = LoggerFactory.getLogger(getClass());
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
            File rawFile = IOSPerfTestHelper.getInstance().getResultFile(inspectionKey);
            IOSPerfTestHelper.getInstance().stop(inspectionKey);
            long startTimeStamp = IOSPerfTestHelper.getInstance().getStartTime(inspectionKey);
            if (rawFile != null) {
                try (BufferedReader bufferedReader = new BufferedReader(new FileReader(rawFile))) {
                    String line;
                    long lineNumber = 0;
                    int currentInspection = 0;
                    List<PerformanceInspectionResult> newPerfInspectionResults = new CopyOnWriteArrayList<>();

                    while ((line = bufferedReader.readLine()) != null) {
                        long timestampForThisLine = startTimeStamp + lineNumber * ONE_SECOND_TIMESTAMP;
                        long timestampForNextLine = startTimeStamp + (lineNumber + 1) * ONE_SECOND_TIMESTAMP;
                        String description = "";
                        if ((currentInspection < inspectionSize)
                                && (currentInspection == 0
                                || oldInspectionResults.get(currentInspection).timestamp > timestampForThisLine)
                                && oldInspectionResults.get(currentInspection).timestamp < timestampForNextLine) {
                            description = oldInspectionResults.get(currentInspection).inspection.description;
                            currentInspection++;
                        }
                        String lineSubString = line.substring(line.indexOf("{")).replace("'", "\"").replace("None", "null");
                        classLogger.info("JsonLine: " + lineSubString);
                        IOSEnergyGaugeInfo energyInfo = JSON.parseObject(lineSubString, IOSEnergyGaugeInfo.class);
                        JSONObject dataLineObj = JSON.parseObject(lineSubString);
                        // Todo: Remove this block when typo from energy gauge is fixed
                        if (dataLineObj.containsKey("energy.networking.overhead")) {
                            energyInfo.setNetworkingOverhead(dataLineObj.getFloat("energy.networking.overhead"));
                        }
                        energyInfo.setTimeStamp(timestampForThisLine);
                        energyInfo.setAppPackageName(appId);
                        energyInfo.setDescription(description);

                        PerformanceInspection newInspection = PerformanceInspection.createIOSEnergyInspection(appId, deviceIdentifier, description, false);
                        newInspection.resultFolder = resultFolder;
                        PerformanceInspectionResult result = new PerformanceInspectionResult(rawFile, newInspection, timestampForThisLine);
                        result.parsedData = energyInfo;
                        newPerfInspectionResults.add(result);
                        lineNumber++;
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
