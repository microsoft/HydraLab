// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance.parsers;

import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceResultParser;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import com.microsoft.hydralab.performance.entity.WindowsMemoryParsedData;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WindowsMemoryResultParser implements PerformanceResultParser {

    private static final Pattern pattern = Pattern.compile("^Id=(.*?) .*?ProcessName=(.*?) " +
            ".*?NonpagedSystemMemorySize64=(.*?) .*?PagedMemorySize64=(.*?) .*?PagedSystemMemorySize64=(.*?) " +
            ".*?PeakPagedMemorySize64=(.*?) .*?PeakVirtualMemorySize64=(.*?) .*?PeakWorkingSet64=(.*?) " +
            ".*?PrivateMemorySize64=(.*?) .*?WorkingSet64=(.*?) .*?Description=(.*?) .*?Path=(.*?) .*?Product=(.*?) " +
            ".*?ProductVersion=(.*?)$");

    @Override
    public PerformanceTestResult parse(PerformanceTestResult performanceTestResult, Logger logger) {
        WindowsMemoryParsedData averagedData = new WindowsMemoryParsedData();
        performanceTestResult.setResultSummary(averagedData);

        Map<Long, Integer> metricsCountPerProcess = new ConcurrentHashMap<>();
        Map<Long, BigInteger[]> metricsSumPerProcess = new ConcurrentHashMap<>();

        for (PerformanceInspectionResult inspectionResult : performanceTestResult.performanceInspectionResults) {
            if (inspectionResult == null) {
                continue;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(inspectionResult.rawResultFile,
                    StandardCharsets.UTF_16))) {
                WindowsMemoryParsedData parsedData = new WindowsMemoryParsedData();
                inspectionResult.parsedData = parsedData;
                String line;

                while ((line = reader.readLine()) != null)
                {
                    Matcher matcher = pattern.matcher(line);
                    while (matcher.find())
                    {
                        Long processId = Long.parseLong(matcher.group(1));
                        String processName = matcher.group(2);
                        WindowsMemoryParsedData.WindowsMemoryMetrics windowsMemoryMetrics =
                                getWindowsMemoryMetrics(matcher);

                        parsedData.getProcessIdProcessNameMap().put(processId, processName);
                        parsedData.getProcessIdWindowsMemoryMetricsMap().put(processId, windowsMemoryMetrics);

                        if (!metricsSumPerProcess.containsKey(processId)) {
                            BigInteger[] sumOfTheData = new BigInteger[8];
                            Arrays.fill(sumOfTheData, BigInteger.ZERO);
                            metricsSumPerProcess.put(processId, sumOfTheData);
                        }
                        accumulateToTheSum(windowsMemoryMetrics, metricsSumPerProcess.get(processId));
                        int count = metricsCountPerProcess.getOrDefault(processId, 0);
                        metricsCountPerProcess.put(processId, count + 1);

                        averagedData.getProcessIdProcessNameMap().putIfAbsent(processId, processName);
                    }
                }

            } catch (FileNotFoundException e) {
                logger.error("Failed to find the file.", e);
            } catch (IOException e) {
                logger.error("Failed to read data from the file.", e);
            }
        }

        calculateTheAverage(metricsCountPerProcess, metricsSumPerProcess,
                averagedData.getProcessIdWindowsMemoryMetricsMap());

        return performanceTestResult;
    }

    private WindowsMemoryParsedData.WindowsMemoryMetrics getWindowsMemoryMetrics(Matcher matcher)
    {
        long nonpagedSystemMemorySize64 = Long.parseLong(matcher.group(3));
        long pagedMemorySize64 = Long.parseLong(matcher.group(4));
        long pagedSystemMemorySize64 = Long.parseLong(matcher.group(5));
        long peakPagedMemorySize64 = Long.parseLong(matcher.group(6));
        long peakVirtualMemorySize64 = Long.parseLong(matcher.group(7));
        long peakWorkingSet64 = Long.parseLong(matcher.group(8));
        long privateMemorySize64 = Long.parseLong(matcher.group(9));
        long workingSet64 = Long.parseLong(matcher.group(10));

        WindowsMemoryParsedData.WindowsMemoryMetrics windowsMemoryMetrics =
                new WindowsMemoryParsedData.WindowsMemoryMetrics();
        windowsMemoryMetrics.setNonpagedSystemMemorySize64(nonpagedSystemMemorySize64);
        windowsMemoryMetrics.setPagedMemorySize64(pagedMemorySize64);
        windowsMemoryMetrics.setPagedSystemMemorySize64(pagedSystemMemorySize64);
        windowsMemoryMetrics.setPeakPagedMemorySize64(peakPagedMemorySize64);
        windowsMemoryMetrics.setPeakVirtualMemorySize64(peakVirtualMemorySize64);
        windowsMemoryMetrics.setPeakWorkingSet64(peakWorkingSet64);
        windowsMemoryMetrics.setPrivateMemorySize64(privateMemorySize64);
        windowsMemoryMetrics.setWorkingSet64(workingSet64);

        return windowsMemoryMetrics;
    }

    private BigInteger[] accumulateToTheSum(WindowsMemoryParsedData.WindowsMemoryMetrics metrics,
                                            BigInteger[] sumOfTheData)
    {
        sumOfTheData[0] = sumOfTheData[0].add(BigInteger.valueOf(metrics.getNonpagedSystemMemorySize64()));
        sumOfTheData[1] = sumOfTheData[1].add(BigInteger.valueOf(metrics.getPagedMemorySize64()));
        sumOfTheData[2] = sumOfTheData[2].add(BigInteger.valueOf(metrics.getPagedSystemMemorySize64()));
        sumOfTheData[3] = sumOfTheData[3].add(BigInteger.valueOf(metrics.getPeakPagedMemorySize64()));
        sumOfTheData[4] = sumOfTheData[4].add(BigInteger.valueOf(metrics.getPeakVirtualMemorySize64()));
        sumOfTheData[5] = sumOfTheData[5].add(BigInteger.valueOf(metrics.getPeakWorkingSet64()));
        sumOfTheData[6] = sumOfTheData[6].add(BigInteger.valueOf(metrics.getPrivateMemorySize64()));
        sumOfTheData[7] = sumOfTheData[7].add(BigInteger.valueOf(metrics.getWorkingSet64()));

        return sumOfTheData;
    }

    private void calculateTheAverage(Map<Long, Integer> metricsCountPerProcess,
                                     Map<Long, BigInteger[]> metricsSumPerProcess,
                                     Map<Long, WindowsMemoryParsedData.WindowsMemoryMetrics>
                                             processIdWindowsMemoryMetricsMap)
    {
        metricsCountPerProcess.forEach((processId, count) -> {
            BigInteger[] summedMetrics = metricsSumPerProcess.get(processId);
            if (count <= 0) {
                throw new ArithmeticException("The divisor cannot be less than or equal to zero.");
            }

            for (int i = 0; i < summedMetrics.length; ++i) {
                summedMetrics[i] = summedMetrics[i].divide(BigInteger.valueOf(count));
            }
            BigInteger[] averagedMetrics = summedMetrics;

            WindowsMemoryParsedData.WindowsMemoryMetrics windowsMemoryMetrics =
                    new WindowsMemoryParsedData.WindowsMemoryMetrics();
            processIdWindowsMemoryMetricsMap.put(processId, windowsMemoryMetrics);

            windowsMemoryMetrics.setNonpagedSystemMemorySize64(averagedMetrics[0].longValue());
            windowsMemoryMetrics.setPagedMemorySize64(averagedMetrics[1].longValue());
            windowsMemoryMetrics.setPagedSystemMemorySize64(averagedMetrics[2].longValue());
            windowsMemoryMetrics.setPeakPagedMemorySize64(averagedMetrics[3].longValue());
            windowsMemoryMetrics.setPeakVirtualMemorySize64(averagedMetrics[4].longValue());
            windowsMemoryMetrics.setPeakWorkingSet64(averagedMetrics[5].longValue());
            windowsMemoryMetrics.setPrivateMemorySize64(averagedMetrics[6].longValue());
            windowsMemoryMetrics.setWorkingSet64(averagedMetrics[7].longValue());
        });
    }

}
