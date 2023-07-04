// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.espresso;

import cn.hutool.core.lang.Assert;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.microsoft.hydralab.agent.runner.TestRunDeviceOrchestrator;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.performance.PerformanceTestListener;
import org.slf4j.Logger;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Map;

public class EspressoTestInfoProcessorListener extends XmlTestRunListener {
    private final TestRunDevice testRunDevice;
    private final TestRun testRun;
    private final TestTask testTask;
    private final Logger logger;
    private final String pkgName;
    private final AgentManagementService agentManagementService;
    private final PerformanceTestListener performanceTestListener;
    private TestRunDeviceOrchestrator testRunDeviceOrchestrator;
    ADBOperateUtil adbOperateUtil;
    private long recordingStartTimeMillis;
    private int index;
    private boolean alreadyEnd = false;
    private AndroidTestUnit ongoingTestUnit;
    private int numTests;
    private int pid;

    public EspressoTestInfoProcessorListener(AgentManagementService agentManagementService, ADBOperateUtil adbOperateUtil,
                                             TestRunDevice testRunDevice, TestRun testRun, TestTask testTask,
                                             TestRunDeviceOrchestrator testRunDeviceOrchestrator,
                                             PerformanceTestListener performanceTestListener) {
        this.testRunDevice = testRunDevice;
        this.testRunDeviceOrchestrator = testRunDeviceOrchestrator;
        this.agentManagementService = agentManagementService;
        this.adbOperateUtil = adbOperateUtil;
        this.testRun = testRun;
        this.testTask = testTask;
        this.logger = testRun.getLogger();
        this.pkgName = testTask.getPkgName();
        this.performanceTestListener = performanceTestListener;
        setReportDir(testRun.getResultFolder());
        try {
            setHostName(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public File getGifFile() {
        return testRunDevice.getGifFile();
    }

    public void startRecording(int maxTime) {
        logger.info("Start adb logcat collection");
        testRunDeviceOrchestrator.startLogCollector(testRunDevice, pkgName, testRun, logger);
        testRun.setLogcatPath(agentManagementService.getTestBaseRelPathInUrl(new File(testRunDevice.getLogPath())));
        logger.info("Start record screen");
        if (!testTask.isDisableRecording()) {
            testRunDeviceOrchestrator.startScreenRecorder(testRunDevice, testRun.getResultFolder(), maxTime <= 0 ? 30 * 60 : maxTime, logger);
        }
        recordingStartTimeMillis = System.currentTimeMillis();
        final String initializing = "Initializing";
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, initializing);
        testRun.addNewTimeTag(initializing, 0);
        if (testTask.isEnableNetworkMonitor()) {
            testRunDeviceOrchestrator.startNetworkMonitor(testRunDevice, testTask.getNetworkMonitorRule(), testRun.getResultFolder(), logger);
        }
    }

    @Override
    public void testRunStarted(String runName, int numTests) {
        infoLogEnter("testRunStarted", runName, numTests);
        this.numTests = numTests;
        testRun.setTotalCount(numTests);
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        testRun.addNewTimeTag("testRunStarted", System.currentTimeMillis() - recordingStartTimeMillis);
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, runName.substring(runName.lastIndexOf('.') + 1) + ".testRunStarted");
        super.testRunStarted(runName, numTests);
        infoLogEnter(runName, numTests);
        startTools(runName);
        performanceTestListener.testRunStarted();
    }

    private void startTools(String runName) {
        try {
            pid = adbOperateUtil.getPackagePid(testRunDevice.getDeviceInfo(), pkgName, logger);
            logger.info("{} is running test with pid {}", pkgName, pid);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        logger.info("Start gif frames collection");
        testRunDeviceOrchestrator.startGifEncoder(testRunDevice, testRun.getResultFolder(), runName + ".gif");
    }

    private void infoLogEnter(Object... args) {
        StringBuilder builder = new StringBuilder();
        for (Object arg : args) {
            builder.append(" >").append(arg);
        }
        logger.info("TestRunListener: {}", builder);
    }

    private void errorLogEnter(Object... args) {
        StringBuilder builder = new StringBuilder();
        for (Object arg : args) {
            builder.append(" >").append(arg);
        }
        logger.error("TestRunListener: {}", builder);
    }

    @Override
    public void testStarted(TestIdentifier test) {
        infoLogEnter("testStarted", test);
        super.testStarted(test);
        ongoingTestUnit = new AndroidTestUnit();
        ongoingTestUnit.setNumtests(numTests);

        ongoingTestUnit.setStartTimeMillis(System.currentTimeMillis());
        ongoingTestUnit.setRelStartTimeInVideo(ongoingTestUnit.getStartTimeMillis() - recordingStartTimeMillis);

        final int unitIndex = ++index;
        ongoingTestUnit.setCurrentIndexNum(unitIndex);

        String method = test.getTestName();
        if (method == null) {
            ongoingTestUnit.setTestName("testInitialization");
        } else {
            ongoingTestUnit.setTestName(method);
        }
        ongoingTestUnit.setTestedClass(test.getClassName());

        testRun.addNewTimeTag(unitIndex + ". " + ongoingTestUnit.getTitle(), System.currentTimeMillis() - recordingStartTimeMillis);
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, ongoingTestUnit.getTitle());

        ongoingTestUnit.setDeviceTestResultId(testRun.getId());
        ongoingTestUnit.setTestTaskId(testRun.getTestTaskId());

        testRun.addNewTestUnit(ongoingTestUnit);

        testRunDeviceOrchestrator.addGifFrameAsyncDelay(testRunDevice, agentManagementService.getScreenshotDir(), 5, logger);

        performanceTestListener.testStarted(ongoingTestUnit.getTitle());
    }

    @Override
    public void testFailed(TestIdentifier test, String trace) {
        errorLogEnter("testFailed", test, trace);
        super.testFailed(test, trace);
        ongoingTestUnit.setStack(trace);
        ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
        performanceTestListener.testFailure(ongoingTestUnit.getTitle());
        testRun.addNewTimeTag(ongoingTestUnit.getTitle() + ".fail", System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.oneMoreFailure();
    }

    @Override
    public void testAssumptionFailure(TestIdentifier test, String trace) {
        errorLogEnter("testAssumptionFailure", test, trace);
        super.testAssumptionFailure(test, trace);
        ongoingTestUnit.setStack(trace);
        testRun.addNewTimeTag(ongoingTestUnit.getTitle() + ".assumptionFail", System.currentTimeMillis() - recordingStartTimeMillis);
        ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.ASSUMPTION_FAILURE);
    }

    @Override
    public void testIgnored(TestIdentifier test) {
        infoLogEnter("testIgnored", test);
        super.testIgnored(test);
        ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.IGNORED);
    }

    @Override
    public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
        infoLogEnter("testEnded", test, testMetrics);
        testRun.addNewTimeTag(ongoingTestUnit.getTitle() + ".end", System.currentTimeMillis() - recordingStartTimeMillis);
        super.testEnded(test, testMetrics);
        if (ongoingTestUnit.getStatusCode() == 0 || ongoingTestUnit.getStatusCode() == AndroidTestUnit.StatusCodes.ASSUMPTION_FAILURE ||
                ongoingTestUnit.getStatusCode() == AndroidTestUnit.StatusCodes.IGNORED) {
            ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.OK);
            ongoingTestUnit.setSuccess(true);
            performanceTestListener.testSuccess(ongoingTestUnit.getTitle());
        }
        ongoingTestUnit.setEndTimeMillis(System.currentTimeMillis());
        ongoingTestUnit.setRelEndTimeInVideo(ongoingTestUnit.getEndTimeMillis() - recordingStartTimeMillis);
    }

    @Override
    public void testRunFailed(String errorMessage) {
        errorLogEnter("testRunFailed", errorMessage);
        testRun.addNewTimeTag("testRunFailed", System.currentTimeMillis() - recordingStartTimeMillis);
        Assert.isTrue(testRunDevice.getDeviceInfo().isAlive(), Const.TaskResult.ERROR_DEVICE_OFFLINE);
        super.testRunFailed(errorMessage);
        testRun.setTestErrorMessage(errorMessage);
        if (errorMessage != null && errorMessage.toLowerCase(Locale.US).contains("process crash")) {
            if (testRun.getCrashStack() == null) {
                testRun.setCrashStack(errorMessage);
            }
        }
        // releaseResource();
    }

    @Override
    public void testRunStopped(long elapsedTime) {
        infoLogEnter("testRunStopped", elapsedTime);
        testRun.addNewTimeTag("testRunStopped", System.currentTimeMillis() - recordingStartTimeMillis);
        super.testRunStopped(elapsedTime);
        // releaseResource();
    }

    @Override
    public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
        infoLogEnter("testRunEnded", elapsedTime, runMetrics, Thread.currentThread().getName());
        synchronized (this) {
            if (alreadyEnd) {
                return;
            }
            performanceTestListener.testRunFinished();
            testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
            super.testRunEnded(elapsedTime, runMetrics);
            testRun.onTestEnded();
            testRunDeviceOrchestrator.setRunningTestName(testRunDevice, null);
            releaseResource();
            alreadyEnd = true;
        }
    }

    private void releaseResource() {
        if (testTask.isEnableNetworkMonitor()) {
            testRunDeviceOrchestrator.stopNetworkMonitor(testRunDevice, logger);
        }
        testRunDeviceOrchestrator.stopGitEncoder(testRunDevice, agentManagementService.getScreenshotDir(), logger);
        if (!testTask.isDisableRecording()) {
            testRunDeviceOrchestrator.stopScreenRecorder(testRunDevice, testRun.getResultFolder(), logger);
        }
        testRunDeviceOrchestrator.stopLogCollector(testRunDevice);
    }
}
