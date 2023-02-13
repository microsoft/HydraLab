// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance.parsers;

import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceResultParser;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
class WindowsMemoryParsedData {

    @Data
    static
    class WindowsMemoryMetrics
    {
        private long nonpagedSystemMemorySize64;
        private long pagedMemorySize64;
        private long pagedSystemMemorySize64;
        private long peakPagedMemorySize64;
        private long peakVirtualMemorySize64;
        private long peakWorkingSet64;
        private long privateMemorySize64;
        private long workingSet64;

    }

    // Process ID to process name.
    private final Map<Long, String> processIdProcessNameMap = new ConcurrentHashMap<>();
    // Process ID to Windows memory metrics.
    private final Map<Long, WindowsMemoryMetrics> processIdWindowsMemoryMetricsMap = new ConcurrentHashMap<>();

}

public class WindowsMemoryResultParser implements PerformanceResultParser {

    private final Logger classLogger = LoggerFactory.getLogger(getClass());

    private static final Pattern pattern = Pattern.compile("^Id=(.*?) .*?ProcessName=(.*?) " +
            ".*?NonpagedSystemMemorySize64=(.*?) .*?PagedMemorySize64=(.*?) .*?PagedSystemMemorySize64=(.*?) " +
            ".*?PeakPagedMemorySize64=(.*?) .*?PeakVirtualMemorySize64=(.*?) .*?PeakWorkingSet64=(.*?) " +
            ".*?PrivateMemorySize64=(.*?) .*?WorkingSet64=(.*?) .*?Description=(.*?) .*?Path=(.*?) .*?Product=(.*?) " +
            ".*?ProductVersion=(.*?)$");

    @Override
    public PerformanceTestResult parse(PerformanceTestResult performanceTestResult) {
        for (PerformanceInspectionResult inspectionResult : performanceTestResult.performanceInspectionResults)
        {
            try {
                WindowsMemoryParsedData parsedData = new WindowsMemoryParsedData();
                inspectionResult.parsedData = parsedData;
                BufferedReader reader = new BufferedReader(new FileReader(inspectionResult.rawResultFile,
                        StandardCharsets.UTF_16));
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
                    }
                }

                reader.close();
            } catch (FileNotFoundException e) {
                classLogger.error("Failed to find the file.", e);
            } catch (IOException e) {
                classLogger.error("Failed to read data from the file.", e);
            }
        }

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

}
