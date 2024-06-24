// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.espresso;

import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.testrunner.InstrumentationResultParser;
import com.microsoft.hydralab.agent.runner.TestRunDeviceOrchestrator;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.common.entity.agent.EnvCapability;
import com.microsoft.hydralab.common.entity.agent.EnvCapabilityRequirement;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.logger.MultiLineNoCancelReceiver;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.FlowUtil;
import com.microsoft.hydralab.common.util.LogUtils;
import com.microsoft.hydralab.common.util.ThreadUtils;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.microsoft.hydralab.common.util.AgentConstant.ESPRESSO_TEST_ORCHESTRATOR_APK;
import static com.microsoft.hydralab.common.util.AgentConstant.ESPRESSO_TEST_SERVICES_APK;

public class EspressoRunner extends TestRunner {
    private static final int MAJOR_ADB_VERSION = 1;
    private static final int MINOR_ADB_VERSION = -1;
    final ADBOperateUtil adbOperateUtil;

    public EspressoRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                          TestRunDeviceOrchestrator testRunDeviceOrchestrator, PerformanceTestManagementService performanceTestManagementService,
                          ADBOperateUtil adbOperateUtil) {
        super(agentManagementService, testTaskRunCallback, testRunDeviceOrchestrator, performanceTestManagementService);
        this.adbOperateUtil = adbOperateUtil;
    }

    @Override
    protected List<EnvCapabilityRequirement> getEnvCapabilityRequirements() {
        return List.of(new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.adb, MAJOR_ADB_VERSION, MINOR_ADB_VERSION));
    }

    @Override
    public void setup(TestTask testTask, TestRun testRun) throws Exception {
        super.setup(testTask, testRun);
        if (testTask.isEnableTestOrchestrator()) {
            reinstallOrchestratorDependency(testRun.getDevice(), testTask, testRun.getLogger());
        }
    }

    @Override
    protected void run(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun) throws Exception {
        InstrumentationResultParser instrumentationResultParser = null;
        Logger reportLogger = testRun.getLogger();

        try {
            /** xml report: parse listener */
            reportLogger.info("Start xml report: parse listener");
            EspressoTestInfoProcessorListener listener =
                    new EspressoTestInfoProcessorListener(agentManagementService,
                            adbOperateUtil, testRunDevice, testRun, testTask,
                            testRunDeviceOrchestrator, performanceTestManagementService);
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

            StringBuilder pathToTestServicePack = new StringBuilder();
            if (testTask.isEnableTestOrchestrator()) {
                adbOperateUtil.executeShellCommandOnDevice(testRunDevice.getDeviceInfo(), "pm path androidx.test.services", new MultiLineNoCancelReceiver() {
                    @Override
                    public void processNewLines(@NotNull String[] lines) {
                        for (String line : lines) {
                            pathToTestServicePack.append(line);
                        }
                    }
                }, testTask.getTimeOutSecond(), -1);
            }

            String command = buildCommand(testTask.getTestSuite(), testTask.getTestPkgName(), testTask.getTestRunnerName(),
                    testTask.getTestScope(), testTask.getTaskRunArgs(), testTask.isEnableTestOrchestrator(), pathToTestServicePack.toString());
            String result = startInstrument(testRunDevice.getDeviceInfo(), reportLogger,
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
            adbOperateUtil.executeShellCommandOnDevice(deviceInfo, command, receiver, testTimeOut, -1);
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
                                String testRunnerName, String scope, Map<String, String> taskRunArgs, boolean enableTestOrchestrator, String pathToTestServicePack) {
        StringBuilder argString = new StringBuilder();
        if (taskRunArgs != null && !taskRunArgs.isEmpty()) {
            taskRunArgs.forEach(
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
            case TestTask.TestScope.PACKAGE:
                commFormat += " -e package %s";
                commFormat = String.format(commFormat, suiteName);
                break;
            case TestTask.TestScope.CLASS:
                commFormat += " -e class %s";
                commFormat = String.format(commFormat, suiteName);
                break;
            default:
                break;
        }
        if (enableTestOrchestrator) {
            commFormat = "CLASSPATH='" + pathToTestServicePack + "' app_process / androidx.test.services.shellexecutor.ShellMain " + commFormat +
                    " -e targetInstrumentation %s/%s androidx.test.orchestrator/.AndroidTestOrchestrator";
        } else {
            commFormat += " %s/%s";
        }
        command = String.format(commFormat, testPkgName, testRunnerName);

        return command;
    }

    protected void reinstallOrchestratorDependency(TestRunDevice testRunDevice, TestTask testTask, Logger reportLogger) throws Exception {
        checkTestTaskCancel(testTask);

        String pathToTestOrchestratorApk = this.agentManagementService.copyPreinstallAPK(ESPRESSO_TEST_ORCHESTRATOR_APK);
        String pathToTestServicesApk = this.agentManagementService.copyPreinstallAPK(ESPRESSO_TEST_SERVICES_APK);

        testRunDeviceOrchestrator.uninstallApp(testRunDevice, "androidx.test.services", reportLogger);
        ThreadUtils.safeSleep(2000);
        testRunDeviceOrchestrator.uninstallApp(testRunDevice, "androidx.test.orchestrator", reportLogger);
        ThreadUtils.safeSleep(2000);

        String extraArgs = "--force-queryable";
        FlowUtil.retryAndSleepWhenFalse(3, 5,
                () -> testRunDeviceOrchestrator.installApp(testRunDevice, new File(pathToTestOrchestratorApk).getAbsolutePath(), extraArgs,
                        reportLogger));
        FlowUtil.retryAndSleepWhenFalse(3, 5,
                () -> testRunDeviceOrchestrator.installApp(testRunDevice, new File(pathToTestServicesApk).getAbsolutePath(), extraArgs,
                        reportLogger));
    }
}
