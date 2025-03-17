// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.monkey;

import com.microsoft.hydralab.agent.runner.TestRunDeviceOrchestrator;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.common.entity.agent.EnvCapability;
import com.microsoft.hydralab.common.entity.agent.EnvCapabilityRequirement;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.logger.MultiLineNoCancelLoggingReceiver;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.common.util.LogUtils;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

public class AdbMonkeyRunner extends TestRunner {
    @SuppressWarnings("constantname")
    static final Logger classLogger = LoggerFactory.getLogger(AdbMonkeyRunner.class);
    private static final String TEST_RUN_NAME = "ADB monkey test";
    private static final int MAJOR_ADB_VERSION = 1;
    private static final int MINOR_ADB_VERSION = -1;
    final ADBOperateUtil adbOperateUtil;

    public AdbMonkeyRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                           TestRunDeviceOrchestrator testRunDeviceOrchestrator,
                           PerformanceTestManagementService performanceTestManagementService,
                           ADBOperateUtil adbOperateUtil) {
        super(agentManagementService, testTaskRunCallback, testRunDeviceOrchestrator, performanceTestManagementService);
        this.adbOperateUtil = adbOperateUtil;
    }

    @Override
    protected List<EnvCapabilityRequirement> getEnvCapabilityRequirements() {
        return List.of(new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.adb, MAJOR_ADB_VERSION, MINOR_ADB_VERSION));
    }

    @Override
    protected void run(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun) throws Exception {
        testRun.setTotalCount(1);
        Logger logger = testRun.getLogger();
        startTools(testRunDevice, testTask, testRun, testTask.getTimeOutSecond(), logger);

        /* run the test */
        logger.info("Start " + TEST_RUN_NAME);
        performanceTestManagementService.testRunStarted();
        checkTestTaskCancel(testTask);
        performanceTestManagementService.testStarted(TEST_RUN_NAME);
        AndroidTestUnit ongoingMonkeyTest = new AndroidTestUnit();
        long checkTime = runMonkeyTestOnce(testRunDevice, testTask, testRun, ongoingMonkeyTest, logger);

        releaseResource(testTask, testRunDevice, testRun);
        if (checkTime > 0) {
            String crashStack = testRun.getCrashStack();
            if (!StringUtils.isEmpty(crashStack)) {
                ongoingMonkeyTest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
                ongoingMonkeyTest.setSuccess(false);
                ongoingMonkeyTest.setStack(crashStack);
                performanceTestManagementService.testFailure(ongoingMonkeyTest.getTitle());
                testRun.addNewTimeTagBeforeLast(ongoingMonkeyTest.getTitle() + ".fail", checkTime);
                testRun.oneMoreFailure();
            } else {
                performanceTestManagementService.testSuccess(ongoingMonkeyTest.getTitle());
            }
        }
        performanceTestManagementService.testRunFinished();
        testRunEnded(testRun);

        /* set paths */
        String absoluteReportPath = testRun.getResultFolder().getAbsolutePath();
        testRun.setTestXmlReportPath(agentManagementService.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
        File gifFile = testRunDevice.getGifFile();
        if (gifFile.exists() && gifFile.length() > 0) {
            testRun.setTestGifPath(agentManagementService.getTestBaseRelPathInUrl(gifFile));
        }

    }

    public void startTools(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun, int maxTime, Logger logger) {
        /* start Record **/
        if (!testTask.isDisableRecording()) {
            testRunDeviceOrchestrator.startScreenRecorder(testRunDevice, testRun.getResultFolder(), maxTime, logger);
        }
        logger.info("Start record screen");
        final String initializing = "Initializing";
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, initializing);
        testRun.addNewTimeTag(initializing, 0);
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        logger.info("Start adb logcat collection");
        testRunDeviceOrchestrator.startLogCollector(testRunDevice, testTask.getPkgName(), testRun, logger);
        testRun.setLogcatPath(agentManagementService.getTestBaseRelPathInUrl(new File(testRunDevice.getLogPath())));

        logger.info("Start gif frames collection");
        testRunDeviceOrchestrator.startGifEncoder(testRunDevice, testRun.getResultFolder(), testTask.getPkgName() + ".gif");
    }

    public long runMonkeyTestOnce(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun, AndroidTestUnit ongoingMonkeyTest, Logger logger) {
        long checkTime = 0;
        final int unitIndex = 1;
        String title = "Monkey_Test";
        ongoingMonkeyTest.setNumtests(testRun.getTotalCount());
        ongoingMonkeyTest.setStartTimeMillis(System.currentTimeMillis());
        ongoingMonkeyTest.setRelStartTimeInVideo(ongoingMonkeyTest.getStartTimeMillis() - testRun.getTestStartTimeMillis());
        ongoingMonkeyTest.setCurrentIndexNum(unitIndex);
        ongoingMonkeyTest.setTestName(title);
        ongoingMonkeyTest.setTestedClass(testTask.getPkgName());
        ongoingMonkeyTest.setDeviceTestResultId(testRun.getId());
        ongoingMonkeyTest.setTestTaskId(testRun.getTestTaskId());
        ongoingMonkeyTest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
        ongoingMonkeyTest.setSuccess(false);
        testRun.addNewTestUnit(ongoingMonkeyTest);

        logger.info(ongoingMonkeyTest.getTitle());
        testRunDeviceOrchestrator.addGifFrameAsyncDelay(testRunDevice, agentManagementService.getScreenshotDir(), 2, logger);
        //run monkey test
        testRun.addNewTimeTag(unitIndex + ". " + ongoingMonkeyTest.getTitle(),
                System.currentTimeMillis() - testRun.getTestStartTimeMillis());
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, ongoingMonkeyTest.getTitle());
        StringBuilder argString = new StringBuilder();
        Map<String, String> taskRunArgs = testTask.getTaskRunArgs();
        if (taskRunArgs != null && !taskRunArgs.isEmpty()) {
            taskRunArgs.forEach((k, v) -> argString.append(" ").append(v));
        }
        String commFormat;
        if (StringUtils.isBlank(argString.toString())) {
            commFormat = "monkey -p %s %d";
        } else {
            commFormat = "monkey -p %s " + argString + " %d";
        }
        try {
            String command = String.format(commFormat, testTask.getPkgName(), testTask.getMaxStepCount());
            // make sure pass is not printed
            logger.info(">> adb -s {} shell {}", testRunDevice.getDeviceInfo().getSerialNum(), LogUtils.scrubSensitiveArgs(command));
            adbOperateUtil.executeShellCommandOnDevice(testRunDevice.getDeviceInfo(), command,
                    new MultiLineNoCancelLoggingReceiver(logger), -1, -1);
            checkTime = System.currentTimeMillis() - testRun.getTestStartTimeMillis();
            ongoingMonkeyTest.setStatusCode(AndroidTestUnit.StatusCodes.OK);
            ongoingMonkeyTest.setSuccess(true);
        } catch (Exception e) {
            classLogger.error(e.getMessage(), e);
            ongoingMonkeyTest.setStack(e.toString());
            testRun.addNewTimeTagBeforeLast(ongoingMonkeyTest.getTitle() + ".fail",
                    System.currentTimeMillis() - testRun.getTestStartTimeMillis());
            testRun.oneMoreFailure();
        }

        logger.info(ongoingMonkeyTest.getTitle() + ".end");
        ongoingMonkeyTest.setEndTimeMillis(System.currentTimeMillis());
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, null);
        testRun.addNewTimeTag(ongoingMonkeyTest.getTitle() + ".end",
                System.currentTimeMillis() - testRun.getTestStartTimeMillis());
        return checkTime;
    }

    public void releaseResource(TestTask testTask, TestRunDevice testRunDevice, TestRun testRun) {
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, null);
        testRunDeviceOrchestrator.stopGitEncoder(testRunDevice, agentManagementService.getScreenshotDir(), testRun.getLogger());
        if (!testTask.isDisableRecording()) {
            testRunDeviceOrchestrator.stopScreenRecorder(testRunDevice, testRun.getResultFolder(), testRun.getLogger());
        }
        testRunDeviceOrchestrator.stopLogCollector(testRunDevice);
    }

    public void testRunEnded(TestRun testRun) {
        testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - testRun.getTestStartTimeMillis());
        testRun.onTestEnded();
    }
}
