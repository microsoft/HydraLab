// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.inspectors;

import com.microsoft.hydralab.common.util.ShellUtils;
import com.microsoft.hydralab.common.util.TimeUtils;
import com.microsoft.hydralab.performance.PerformanceInspection;
import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceInspector;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import java.io.File;

public class AndroidMemoryHprofInspector implements PerformanceInspector  {

    private static final String RAW_RESULT_FILE_NAME_FORMAT = "memory_%s.hprof";
    private static final String HPROF_FILE_PREFIX = "/data/local/tmp/";


    @Override
    public PerformanceInspectionResult inspect(PerformanceInspection performanceInspection, Logger logger) {

        File rawResultFolder = new File(performanceInspection.resultFolder, performanceInspection.appId);
        Assert.isTrue(rawResultFolder.exists() || rawResultFolder.mkdir(), "rawResultFolder.mkdirs() failed in" + rawResultFolder.getAbsolutePath());
        String tmpTime = TimeUtils.getTimestampForFilename();
        String hprofFileName = String.format(RAW_RESULT_FILE_NAME_FORMAT, tmpTime);
        File rawResultFile = new File(rawResultFolder,
                hprofFileName);
        String sdHprofFilePath = HPROF_FILE_PREFIX + hprofFileName;
        String dumpCommand = String.format(getMemHprofCommand(), performanceInspection.deviceIdentifier, performanceInspection.appId, sdHprofFilePath);
        if (!isDebuggable(performanceInspection.deviceIdentifier, performanceInspection.appId, logger)) {
            return new PerformanceInspectionResult(null, performanceInspection);
        } else {
            Process process = ShellUtils.execLocalCommand(dumpCommand, false, logger);
            if (process != null) {
                try {
                    int ret = process.waitFor();
                    if (ret == 0) {
                        Process pullProcess = ShellUtils.execLocalCommand("adb pull " + sdHprofFilePath + " " + rawResultFile.getAbsolutePath(), false, logger);
                        if (pullProcess != null) {
                            ret = pullProcess.waitFor();
                            if (ret == 0) {
                                return new PerformanceInspectionResult(rawResultFile, performanceInspection);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                    return new PerformanceInspectionResult(null, performanceInspection);
                }
        }


        }
        return new PerformanceInspectionResult(null, performanceInspection);
    }

    private String getMemHprofCommand() {
        return "adb -s %s shell am dumpheap %s %s";
    }


    private boolean isDebuggable(String deviceId, String packageName, Logger logger) {
        String cmd = String.format("adb -s %s shell dumpsys package %s | findstr flags", deviceId, packageName);
        String ret = ShellUtils.execLocalCommandWithResult(cmd, logger);
        return ret != null && ret.contains("DEBUGGABLE");
    }
}
