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

                    // Uncomment the following line to get the sum of the energy consumption
                    // performanceTestResult.setResultSummary(getSumIOSEnergy(newPerfInspectionResults));
                    performanceTestResult.setResultSummary(getAverageIOSEnergy(newPerfInspectionResults));
                    oldInspectionResults.clear();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return performanceTestResult;
    }

    private IOSEnergyGaugeInfo getAverageIOSEnergy(List<PerformanceInspectionResult> inspectionResults) {
        if (inspectionResults == null || inspectionResults.size() == 0) {
            return null;
        }

        IOSEnergyGaugeInfo averageEnergyInfo = new IOSEnergyGaugeInfo();
        averageEnergyInfo.setAppPackageName(inspectionResults.get(0).inspection.appId);
        averageEnergyInfo.setTimeStamp(System.currentTimeMillis());

        for (int i = 0; i < inspectionResults.size(); i++) {
            PerformanceInspectionResult result = inspectionResults.get(i);
            IOSEnergyGaugeInfo energyInfo = (IOSEnergyGaugeInfo) result.parsedData;
            averageEnergyInfo.setTotalCost(averageEnergyInfo.getTotalCost() * i / (i + 1) + energyInfo.getTotalCost() / (i + 1));
            averageEnergyInfo.setCpuCost(averageEnergyInfo.getCpuCost() * i / (i + 1) + energyInfo.getCpuCost() / (i + 1));
            averageEnergyInfo.setGpuCost(averageEnergyInfo.getGpuCost() * i / (i + 1) + energyInfo.getGpuCost() / (i + 1));
            averageEnergyInfo.setNetworkingCost(averageEnergyInfo.getNetworkingCost() * i / (i + 1) + energyInfo.getNetworkingCost() / (i + 1));
            averageEnergyInfo.setAppStateCost(averageEnergyInfo.getAppStateCost() * i / (i + 1) + energyInfo.getAppStateCost() / (i + 1));
            averageEnergyInfo.setLocationCost(averageEnergyInfo.getLocationCost() * i / (i + 1) + energyInfo.getLocationCost() / (i + 1));
            averageEnergyInfo.setThermalStateCost(averageEnergyInfo.getThermalStateCost() * i / (i + 1) + energyInfo.getThermalStateCost() / (i + 1));

            averageEnergyInfo.setTotalOverhead(averageEnergyInfo.getTotalOverhead() * i / (i + 1) + energyInfo.getTotalOverhead() / (i + 1));
            averageEnergyInfo.setCpuOverhead(averageEnergyInfo.getCpuOverhead() * i / (i + 1) + energyInfo.getCpuOverhead() / (i + 1));
            averageEnergyInfo.setGpuOverhead(averageEnergyInfo.getGpuOverhead() * i / (i + 1) + energyInfo.getGpuOverhead() / (i + 1));
            averageEnergyInfo.setNetworkingOverhead(averageEnergyInfo.getNetworkingOverhead() * i / (i + 1) + energyInfo.getNetworkingOverhead() / (i + 1));
            averageEnergyInfo.setAppStateOverhead(averageEnergyInfo.getAppStateOverhead() * i / (i + 1) + energyInfo.getAppStateOverhead() / (i + 1));
            averageEnergyInfo.setLocationOverhead(averageEnergyInfo.getLocationOverhead() * i / (i + 1) + energyInfo.getLocationOverhead() / (i + 1));
            averageEnergyInfo.setThermalStateOverhead(averageEnergyInfo.getThermalStateOverhead() * i / (i + 1) + energyInfo.getThermalStateOverhead() / (i + 1));
        }

        return averageEnergyInfo;
    }

    private IOSEnergyGaugeInfo getSumIOSEnergy(List<PerformanceInspectionResult> inspectionResults) {
        if (inspectionResults == null || inspectionResults.size() == 0) {
            return null;
        }

        IOSEnergyGaugeInfo sumEnergyInfo = new IOSEnergyGaugeInfo();
        sumEnergyInfo.setAppPackageName(inspectionResults.get(0).inspection.appId);
        sumEnergyInfo.setTimeStamp(System.currentTimeMillis());

        for (PerformanceInspectionResult result : inspectionResults) {
            IOSEnergyGaugeInfo energyInfo = (IOSEnergyGaugeInfo) result.parsedData;
            sumEnergyInfo.setTotalCost(sumEnergyInfo.getTotalCost() + energyInfo.getTotalCost());
            sumEnergyInfo.setCpuCost(sumEnergyInfo.getCpuCost() + energyInfo.getCpuCost());
            sumEnergyInfo.setGpuCost(sumEnergyInfo.getGpuCost() + energyInfo.getGpuCost());
            sumEnergyInfo.setNetworkingCost(sumEnergyInfo.getNetworkingCost() + energyInfo.getNetworkingCost());
            sumEnergyInfo.setAppStateCost(sumEnergyInfo.getAppStateCost() + energyInfo.getAppStateCost());
            sumEnergyInfo.setLocationCost(sumEnergyInfo.getLocationCost() + energyInfo.getLocationCost());
            sumEnergyInfo.setThermalStateCost(sumEnergyInfo.getThermalStateCost() + energyInfo.getThermalStateCost());

            sumEnergyInfo.setTotalOverhead(sumEnergyInfo.getTotalOverhead() + energyInfo.getTotalOverhead());
            sumEnergyInfo.setCpuOverhead(sumEnergyInfo.getCpuOverhead() + energyInfo.getCpuOverhead());
            sumEnergyInfo.setGpuOverhead(sumEnergyInfo.getGpuOverhead() + energyInfo.getGpuOverhead());
            sumEnergyInfo.setNetworkingOverhead(sumEnergyInfo.getNetworkingOverhead() + energyInfo.getNetworkingOverhead());
            sumEnergyInfo.setAppStateOverhead(sumEnergyInfo.getAppStateOverhead() + energyInfo.getAppStateOverhead());
            sumEnergyInfo.setLocationOverhead(sumEnergyInfo.getLocationOverhead() + energyInfo.getLocationOverhead());
            sumEnergyInfo.setThermalStateOverhead(sumEnergyInfo.getThermalStateOverhead() + energyInfo.getThermalStateOverhead());
        }

        return sumEnergyInfo;
    }
}
