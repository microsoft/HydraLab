// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.espresso;

import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.testrunner.InstrumentationResultParser;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.LogUtils;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public class EspressoRunner extends TestRunner {
    final ADBOperateUtil adbOperateUtil;

    public EspressoRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                          PerformanceTestManagementService performanceTestManagementService,
                          ADBOperateUtil adbOperateUtil) {
        super(agentManagementService, testTaskRunCallback, performanceTestManagementService);
        this.adbOperateUtil = adbOperateUtil;
    }

    @Override
    protected void run(DeviceInfo deviceInfo, TestTask testTask, TestRun testRun) throws Exception {
        InstrumentationResultParser instrumentationResultParser = null;
        Logger reportLogger = testRun.getLogger();

        try {
            /** xml report: parse listener */
            reportLogger.info("Start xml report: parse listener");
            EspressoTestInfoProcessorListener listener =
                    new EspressoTestInfoProcessorListener(agentManagementService,
                            adbOperateUtil, deviceInfo, testRun, testTask.getPkgName(),
                            performanceTestManagementService);
            instrumentationResultParser =
                    new InstrumentationResultParser(testTask.getTestSuite(), Collections.singletonList(listener)) {
                        @Override
                        public boolean isCancelled() {
                            return testTask.isCanceled();
                        }
                    };

            /** run the test */
            reportLogger.info("Start instrumenting the test");
            checkTestTaskCancel(testTask);
            listener.startRecording(testTask.getTimeOutSecond());
            String command = buildCommand(testTask.getTestSuite(), testTask.getTestPkgName(), testTask.getTestRunnerName(),
                    testTask.getTestScope(), testTask.getInstrumentationArgs());
            String result = startInstrument(deviceInfo, reportLogger,
                    instrumentationResultParser, testTask.getTimeOutSecond(), command);
            if (Const.TaskResult.ERROR_DEVICE_OFFLINE.equals(result)) {
                testTaskRunCallback.onDeviceOffline(testTask);
                return;
            }
            checkTestTaskCancel(testTask);

            /** set paths */
            String absoluteReportPath = listener.getAbsoluteReportPath();
            testRun.setTestXmlReportPath(
                    agentManagementService.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
            File gifFile = listener.getGifFile();
            if (gifFile.exists() && gifFile.length() > 0) {
                testRun.setTestGifPath(agentManagementService.getTestBaseRelPathInUrl(gifFile));
            }

        } finally {
            if (instrumentationResultParser != null) {
                instrumentationResultParser.flush();
            }
        }
    }

    @Override
    protected boolean shouldInstallTestPackageAsApp() {
        return true;
    }

    public String startInstrument(DeviceInfo deviceInfo, Logger logger, IShellOutputReceiver receiver,
                                  int testTimeOutSec, String command) {
        if (deviceInfo == null) {
            throw new RuntimeException("No such device: " + deviceInfo);
        }
        int testTimeOut = testTimeOutSec;
        if (testTimeOut <= 0) {
            // the test should not last longer than
            testTimeOut = 45 * 60;
        }
        try {
            if (logger != null) {
                // make sure pass is not printed
                logger.info(">> adb -s {} shell {}", deviceInfo.getSerialNum(),
                        LogUtils.scrubSensitiveArgs(command));
            }
            adbOperateUtil.executeShellCommandOnDevice(deviceInfo, command, receiver, testTimeOut);
            return Const.TaskResult.SUCCESS;
        } catch (Exception e) {
            if (logger != null) {
                logger.error(e.getMessage(), e);
            }
            return e.getMessage();
        }
    }

    @NotNull
    private String buildCommand(String suiteName, String testPkgName,
                                String testRunnerName, String scope, Map<String, String> instrumentationArgs) {
        StringBuilder argString = new StringBuilder();
        if (instrumentationArgs != null && !instrumentationArgs.isEmpty()) {
            instrumentationArgs.forEach(
                    (k, v) -> argString.append(" -e ").append(k.replaceAll("\\s|\"", "")).append(" ")
                            .append(v.replaceAll("\\s|\"", "")));
        }
        String commFormat;
        if (StringUtils.isBlank(argString.toString())) {
            commFormat = "am instrument -w -r -e debug false";
        } else {
            commFormat = "am instrument -w -r" + argString + " -e debug false";
        }
        String command;
        switch (scope) {
            case TestTask.TestScope.TEST_APP:
                commFormat += " %s/%s";
                command = String.format(commFormat, testPkgName, testRunnerName);
                break;
            case TestTask.TestScope.PACKAGE:
                commFormat += " -e package %s %s/%s";
                command = String.format(commFormat, suiteName, testPkgName, testRunnerName);
                break;
            // Const.TestScope.CLASS
            default:
                commFormat += " -e class %s %s/%s";
                command = String.format(commFormat, suiteName, testPkgName, testRunnerName);
                break;
        }
        return command;
    }

}
