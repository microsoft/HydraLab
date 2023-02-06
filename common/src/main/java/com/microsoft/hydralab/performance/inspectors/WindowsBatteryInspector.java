// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance.inspectors;

import com.microsoft.hydralab.agent.runner.ITestRun;
import com.microsoft.hydralab.agent.runner.TestRunThreadContext;
import com.microsoft.hydralab.common.util.ShellUtils;
import com.microsoft.hydralab.common.util.TimeUtils;
import com.microsoft.hydralab.performance.PerformanceInspection;
import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * WindowsBatteryInspector is only suitable for the Windows devices with battery,
 * since powercfg command runs only on those devices.
 *
 * Note:
 * powercfg command needs the elevated privileges of powershell, and UAC (User Account Control) dialog may pop up during
 * the elevation process to block the testing. There is a workaround to disable the UAC dialog by setting "Never notify"
 * in the UAC settings panel.
 *
 * TODO:
 * Need to verify if the agent configured with elevated privileges can bypass the UAC popup without changing the UAC
 * configuration.
 * Add a new method in ShellUtils to run the admin command if the TODO is not feasible.
 */
public class WindowsBatteryInspector implements PerformanceInspector {
    private final static String RAW_RESULT_FILE_NAME_FORMAT = "%s_%s.csv";
    private final static String COMMAND_FORMAT = "Start-Process -FilePath Powershell.exe -Verb RunAs -ArgumentList " +
            "'-command \"powercfg /srumutil /OUTPUT %s /CSV \"'";

    protected Logger classLogger = LoggerFactory.getLogger(getClass());

    @Override
    public PerformanceInspectionResult inspect(PerformanceInspection performanceInspection) {
        ITestRun testRun = TestRunThreadContext.getTestRun();
        if (testRun == null)
        {
            classLogger.error("TestRunThreadContext.getTestRun() return null.");
            return null;
        }

        File rawResultFile = new File(performanceInspection.resultFolder,
                String.format(RAW_RESULT_FILE_NAME_FORMAT, getClass().getSimpleName(), TimeUtils.getTimestampForFilename()));
        Process process = ShellUtils.execLocalCommand(String.format(COMMAND_FORMAT, rawResultFile), false, classLogger);
        PerformanceInspectionResult result = new PerformanceInspectionResult(rawResultFile);

        try {
            if (process != null && process.waitFor() != 0)
            {
                classLogger.error("Exit code: " + process.exitValue());
            }
        } catch (InterruptedException e) {
            classLogger.error("InterruptedException caught on process.waitFor().");
            return null;
        }

        return result;
    }

}