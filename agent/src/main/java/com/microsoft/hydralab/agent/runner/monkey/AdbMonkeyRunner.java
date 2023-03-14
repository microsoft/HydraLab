// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.monkey;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.img.gif.AnimatedGifEncoder;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.logger.MultiLineNoCancelLoggingReceiver;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.device.TestDevice;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.common.util.LogUtils;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AdbMonkeyRunner extends TestRunner {
    private static final String TEST_RUN_NAME = "ADB monkey test";
    @SuppressWarnings("constantname")
    static final Logger classLogger = LoggerFactory.getLogger(AdbMonkeyRunner.class);
    private final AnimatedGifEncoder e = new AnimatedGifEncoder();
    final ADBOperateUtil adbOperateUtil;
    private long recordingStartTimeMillis;
    private int index;
    private String pkgName;
    private File gifFile;
    private AndroidTestUnit ongoingMonkeyTest;

    private TestDevice testDevice;

    public AdbMonkeyRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                           PerformanceTestManagementService performanceTestManagementService,
                           ADBOperateUtil adbOperateUtil) {
        super(agentManagementService, testTaskRunCallback, performanceTestManagementService);
        this.adbOperateUtil = adbOperateUtil;
    }

    @Override
    protected void run(TestDevice testDevice, TestTask testTask, TestRun testRun) throws Exception {
        this.testDevice = testDevice;

        testRun.setTotalCount(1);
        Logger reportLogger = testRun.getLogger();

        pkgName = testTask.getPkgName();
        startTools(testRun, testTask.getTimeOutSecond(), reportLogger);

        /** run the test */
        reportLogger.info("Start " + TEST_RUN_NAME);
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        performanceTestManagementService.testRunStarted();
        checkTestTaskCancel(testTask);
        performanceTestManagementService.testStarted(TEST_RUN_NAME);
        long checkTime = runMonkeyTestOnce(testRun, reportLogger, testTask.getInstrumentationArgs(),
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
        testRunEnded(testDevice, testRun);

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
        testDevice.startScreenRecorder(testRun.getResultFolder(), maxTime, logger);
        logger.info("Start record screen");
        recordingStartTimeMillis = System.currentTimeMillis();
        final String initializing = "Initializing";
        testDevice.setRunningTestName(initializing);
        testRun.addNewTimeTag(initializing, 0);

        logger.info("Start adb logcat collection");
        String logcatFilePath = testDevice.startLogCollector(pkgName, testRun, logger);
        testRun.setLogcatPath(agentManagementService.getTestBaseRelPathInUrl(new File(logcatFilePath)));

        logger.info("Start gif frames collection");
        gifFile = new File(testRun.getResultFolder(), pkgName + ".gif");
        e.start(gifFile.getAbsolutePath());
        e.setDelay(1000);
        e.setRepeat(0);
    }

    public File getGifFile() {
        return gifFile;
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

        logger.info(ongoingMonkeyTest.getTitle());
        testDevice.updateScreenshotImageAsyncDelay(TimeUnit.SECONDS.toMillis(2),
                (imagePNGFile -> {
                    if (imagePNGFile == null || !e.isStarted()) {
                        return;
                    }
                    try {
                        e.addFrame(ImgUtil.toBufferedImage(ImgUtil.scale(ImageIO.read(imagePNGFile), 0.3f)));
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }), logger);
        //run monkey test
        testRun.addNewTimeTag(unitIndex + ". " + ongoingMonkeyTest.getTitle(),
                System.currentTimeMillis() - recordingStartTimeMillis);
        testDevice.setRunningTestName(ongoingMonkeyTest.getTitle());
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
            logger.info(">> adb -s {} shell {}", testDevice.getSerialNum(), LogUtils.scrubSensitiveArgs(command));
            adbOperateUtil.executeShellCommandOnDevice(testDevice.getDeviceInfo(), command,
                    new MultiLineNoCancelLoggingReceiver(logger), -1);
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
        testDevice.setRunningTestName(null);
        testRun.addNewTestUnit(ongoingMonkeyTest);
        testRun.addNewTimeTag(ongoingMonkeyTest.getTitle() + ".end",
                System.currentTimeMillis() - recordingStartTimeMillis);
        return checkTime;
    }

    public void testRunEnded(TestDevice testDevice, TestRun testRun) {
        testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.onTestEnded();
        testDevice.setRunningTestName(null);
        releaseResource();
    }

    private void releaseResource() {
        e.finish();
        testDevice.stopLogCollector();
        testDevice.stopScreenRecorder();
    }

}
