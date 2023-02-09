// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance.inspectors;

import com.microsoft.hydralab.common.util.ShellUtils;
import com.microsoft.hydralab.common.util.TimeUtils;
import com.microsoft.hydralab.performance.PerformanceInspection;
import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.File;

public class AndroidBatteryInfoInspector implements PerformanceInspector {
    private static final String RAW_RESULT_FILE_NAME_FORMAT = "%s_%s_%s.txt";
    protected Logger classLogger = LoggerFactory.getLogger(getClass());

    @Override
    public PerformanceInspectionResult inspect(PerformanceInspection performanceInspection) {
        if (performanceInspection.isReset) {
            initialize(performanceInspection);
        }

        File rawResultFolder = new File(performanceInspection.resultFolder, performanceInspection.appId);
        Assert.isTrue(rawResultFolder.exists() || rawResultFolder.mkdir(), "rawResultFolder.mkdirs() failed in" + rawResultFolder.getAbsolutePath());
        File rawResultFile = new File(rawResultFolder,
                String.format(RAW_RESULT_FILE_NAME_FORMAT, getClass().getSimpleName(), performanceInspection.appId, TimeUtils.getTimestampForFilename()));

        ShellUtils.execLocalCommandWithResult(String.format("adb -s %s shell dumpsys batterystats %s | out-file %s -encoding utf8",
                performanceInspection.deviceIdentifier, performanceInspection.appId, rawResultFile.getAbsolutePath()), classLogger);
        return new PerformanceInspectionResult(rawResultFile, performanceInspection);
    }

    private void initialize(PerformanceInspection performanceInspection) {
        String device = performanceInspection.deviceIdentifier;
        ShellUtils.execLocalCommand(String.format("adb -s %s shell dumpsys battery unplug", device), classLogger);
        ShellUtils.execLocalCommand(String.format("adb -s %s shell dumpsys batterystats --reset", device), classLogger);
    }

    // TODO: Do we need to add the method to the Interface, or add clear flag to the PerformanceInspection?
    public void clearEnv(PerformanceInspection performanceInspection) {
        ShellUtils.execLocalCommand(String.format("adb -s %s shell dumpsys battery reset", performanceInspection.deviceIdentifier), classLogger);
    }

}
