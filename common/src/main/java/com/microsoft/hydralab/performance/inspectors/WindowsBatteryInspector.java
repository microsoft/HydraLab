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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Objects;

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
    private final static String SCRIPT_NAME = "WindowsBatteryInspector.ps1";
    private final static File SCRIPT_FILE = new File(SCRIPT_NAME);
    private final static String PARAMETER_FORMAT = " -output %s";

    protected Logger classLogger = LoggerFactory.getLogger(getClass());

    public void initializeIfNeeded(PerformanceInspection performanceInspection) {
        if (!SCRIPT_FILE.exists()) {
            try {
                InputStream resourceAsStream = FileUtils.class.getClassLoader().getResourceAsStream(SCRIPT_NAME);
                OutputStream out = new FileOutputStream(SCRIPT_FILE);
                IOUtils.copy(Objects.requireNonNull(resourceAsStream), out);
                out.close();
            } catch (IOException e) {
                classLogger.error("Failed to find app handler script", e);
            }
        }
    }

    @Override
    public PerformanceInspectionResult inspect(PerformanceInspection performanceInspection) {
        initializeIfNeeded(performanceInspection);

        ITestRun testRun = TestRunThreadContext.getTestRun();
        if (testRun == null)
        {
            classLogger.error("TestRunThreadContext.getTestRun() return null.");
            return null;
        }

        File rawResultFile = new File(performanceInspection.resultFolder,
                String.format(RAW_RESULT_FILE_NAME_FORMAT, getClass().getSimpleName(), TimeUtils.getTimestampForFilename()));
        Process process = ShellUtils.execLocalCommand(SCRIPT_FILE.getAbsolutePath() +
                String.format(PARAMETER_FORMAT, rawResultFile), false, classLogger);
        PerformanceInspectionResult result = new PerformanceInspectionResult(rawResultFile, performanceInspection);

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