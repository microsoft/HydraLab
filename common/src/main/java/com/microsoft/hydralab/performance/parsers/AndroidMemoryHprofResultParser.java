// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.parsers;

import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceResultParser;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import com.microsoft.hydralab.performance.entity.AndroidHprofMemoryInfo;
import com.microsoft.hydralab.performance.hprof.BitmapInfoExtractor;
import com.microsoft.hydralab.performance.hprof.HeapProfProcessor;
import com.microsoft.hydralab.performance.hprof.TopObjectInfoExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AndroidMemoryHprofResultParser implements PerformanceResultParser {
    private static final int MD_OBJECT_REPORT_COUNT = 50;

    @Override
    public PerformanceTestResult parse(PerformanceTestResult performanceTestResult, Logger logger) {
        if (performanceTestResult == null || performanceTestResult.performanceInspectionResults == null
                || performanceTestResult.performanceInspectionResults.isEmpty()) {
            return null;
        }
        List<PerformanceInspectionResult> inspectionResults = performanceTestResult.performanceInspectionResults;
        for (PerformanceInspectionResult inspectionResult : inspectionResults) {
            File hprofFile = inspectionResult.rawResultFile;
            if (hprofFile == null) {
                return performanceTestResult;
            }
            inspectionResult.parsedData = buildAndroidHprofMemoryInfo(hprofFile, inspectionResult.inspection.appId, inspectionResult.inspection.description,
                    inspectionResult.timestamp, logger);
        }

        return performanceTestResult;
    }



    private AndroidHprofMemoryInfo buildAndroidHprofMemoryInfo(File dumpFile, String packageName, String description, long timeStamp, Logger logger) {

        HeapProfProcessor profProcessor = new HeapProfProcessor(dumpFile);

        BitmapInfoExtractor bitmapInfoExtractor = new BitmapInfoExtractor();
        profProcessor.registerExtractor(bitmapInfoExtractor);

        TopObjectInfoExtractor topObjectInfoExtractor = new TopObjectInfoExtractor(MD_OBJECT_REPORT_COUNT);
        profProcessor.registerExtractor(topObjectInfoExtractor);
        try {
            profProcessor.loadAndExtract();
        } catch (IOException e) {
            logger.error("parseHprofFile", e);
            return null;
        }
        AndroidHprofMemoryInfo info = new AndroidHprofMemoryInfo();
        info.setAppPackageName(packageName);
        info.setDescription(description);
        info.setTimeStamp(timeStamp);
        info.setBitmapInfoList(bitmapInfoExtractor.getResultList());
        info.setTopObjectList(topObjectInfoExtractor.getResultList());
        return info;

    }
}
