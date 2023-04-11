// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.monkey;

import com.microsoft.hydralab.agent.runner.TestRunDeviceOrchestrator;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
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
import java.util.Map;

public class AdbMonkeyRunner extends TestRunner {
    private static final String TEST_RUN_NAME = "ADB monkey test";
    @SuppressWarnings("constantname")
    static final Logger classLogger = LoggerFactory.getLogger(AdbMonkeyRunner.class);
    final ADBOperateUtil adbOperateUtil;
    private long recordingStartTimeMillis;
    private int index;
    private String pkgName;
    private AndroidTestUnit ongoingMonkeyTest;

    private TestRunDevice testRunDevice;
    private Logger logger;

    public AdbMonkeyRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                           TestRunDeviceOrchestrator testRunDeviceOrchestrator,
                           PerformanceTestManagementService performanceTestManagementService,
                           ADBOperateUtil adbOperateUtil) {
        super(agentManagementService, testTaskRunCallback, testRunDeviceOrchestrator, performanceTestManagementService);
        this.adbOperateUtil = adbOperateUtil;
    }

    @Override
    protected void run(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun) throws Exception {
        this.testRunDevice = testRunDevice;
        testRun.setTotalCount(1);
        logger = testRun.getLogger();

        pkgName = testTask.getPkgName();
        startTools(testRun, testTask.getTimeOutSecond(), logger);

        /** run the test */
        logger.info("Start " + TEST_RUN_NAME);
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        performanceTestManagementService.testRunStarted();
        checkTestTaskCancel(testTask);
        performanceTestManagementService.testStarted(TEST_RUN_NAME);
        long checkTime = runMonkeyTestOnce(testRun, logger, testTask.getInstrumentationArgs(),
                testTask.getMaxStepCount());
        if (checkTime > 0) {
            String crashStack = testRun.getCrashStack();
            if (crashStack != null && !"".equals(crashStack)) {
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
        testRunEnded(testRunDevice, testRun);

        /** set paths */
        String absoluteReportPath = testRun.getResultFolder().getAbsolutePath();
        testRun.setTestXmlReportPath(agentManagementService.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
        File gifFile = getGifFile();
        if (gifFile.exists() && gifFile.length() > 0) {
            testRun.setTestGifPath(agentManagementService.getTestBaseRelPathInUrl(gifFile));
        }

    }

    public void startTools(TestRun testRun, int maxTime, Logger logger) {
        /** start Record **/
        testRunDeviceOrchestrator.startScreenRecorder(testRunDevice, testRun.getResultFolder(), maxTime, logger);
        logger.info("Start record screen");
        recordingStartTimeMillis = System.currentTimeMillis();
        final String initializing = "Initializing";
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, initializing);
        testRun.addNewTimeTag(initializing, 0);

        logger.info("Start adb logcat collection");
        testRunDeviceOrchestrator.startLogCollector(testRunDevice, pkgName, testRun, logger);
        testRun.setLogcatPath(agentManagementService.getTestBaseRelPathInUrl(new File(testRunDevice.getLogPath())));

        logger.info("Start gif frames collection");
        testRunDeviceOrchestrator.startGifEncoder(testRunDevice, testRun.getResultFolder(), pkgName + ".gif");
    }

    public File getGifFile() {
        return testRunDevice.getGifFile();
    }

    public long runMonkeyTestOnce(TestRun testRun, Logger logger,
                                  Map<String, String> instrumentationArgs, int maxStepCount) {
        long checkTime = 0;
        final int unitIndex = ++index;
        String title = "Monkey_Test";

        ongoingMonkeyTest = new AndroidTestUnit();
        ongoingMonkeyTest.setNumtests(testRun.getTotalCount());
        ongoingMonkeyTest.setStartTimeMillis(System.currentTimeMillis());
        ongoingMonkeyTest.setRelStartTimeInVideo(ongoingMonkeyTest.getStartTimeMillis() - recordingStartTimeMillis);
        ongoingMonkeyTest.setCurrentIndexNum(unitIndex);
        ongoingMonkeyTest.setTestName(title);
        ongoingMonkeyTest.setTestedClass(pkgName);
        ongoingMonkeyTest.setDeviceTestResultId(testRun.getId());
        ongoingMonkeyTest.setTestTaskId(testRun.getTestTaskId());
        testRun.addNewTestUnit(ongoingMonkeyTest);

        logger.info(ongoingMonkeyTest.getTitle());
        testRunDeviceOrchestrator.addGifFrameAsyncDelay(testRunDevice, agentManagementService.getScreenshotDir(), 2, logger);
        //run monkey test
        testRun.addNewTimeTag(unitIndex + ". " + ongoingMonkeyTest.getTitle(),
                System.currentTimeMillis() - recordingStartTimeMillis);
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, ongoingMonkeyTest.getTitle());
        StringBuilder argString = new StringBuilder();
        if (instrumentationArgs != null && !instrumentationArgs.isEmpty()) {
            instrumentationArgs.forEach((k, v) -> argString.append(" ").append(v));
        }
        String commFormat;
        if (StringUtils.isBlank(argString.toString())) {
            commFormat = "monkey -p %s %d";
        } else {
            commFormat = "monkey -p %s " + argString + " %d";
        }
        try {
            String command = String.format(commFormat, pkgName, maxStepCount);
            // make sure pass is not printed
            logger.info(">> adb -s {} shell {}", testRunDevice.getDeviceInfo().getSerialNum(), LogUtils.scrubSensitiveArgs(command));
            adbOperateUtil.executeShellCommandOnDevice(testRunDevice.getDeviceInfo(), command,
                    new MultiLineNoCancelLoggingReceiver(logger), -1, -1);
            checkTime = System.currentTimeMillis() - recordingStartTimeMillis;
            ongoingMonkeyTest.setStatusCode(AndroidTestUnit.StatusCodes.OK);
            ongoingMonkeyTest.setSuccess(true);
        } catch (Exception e) {
            classLogger.error(e.getMessage(), e);

            ongoingMonkeyTest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
            ongoingMonkeyTest.setSuccess(false);
            ongoingMonkeyTest.setStack(e.toString());
            testRun.setSuccess(false);
            testRun.addNewTimeTagBeforeLast(ongoingMonkeyTest.getTitle() + ".fail",
                    System.currentTimeMillis() - recordingStartTimeMillis);
            testRun.oneMoreFailure();
        }

        logger.info(ongoingMonkeyTest.getTitle() + ".end");
        ongoingMonkeyTest.setEndTimeMillis(System.currentTimeMillis());
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, null);
        testRun.addNewTimeTag(ongoingMonkeyTest.getTitle() + ".end",
                System.currentTimeMillis() - recordingStartTimeMillis);
        return checkTime;
    }

    public void testRunEnded(TestRunDevice testRunDevice, TestRun testRun) {
        testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.onTestEnded();
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, null);
        testRunDeviceOrchestrator.stopGitEncoder(testRunDevice, agentManagementService.getScreenshotDir(), logger);
        testRunDeviceOrchestrator.stopScreenRecorder(testRunDevice, testRun.getResultFolder(), logger);
        testRunDeviceOrchestrator.stopLogCollector(testRunDevice);
    }
}
