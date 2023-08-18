// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.python;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.agent.runner.TestRunDeviceOrchestrator;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.common.entity.agent.EnvCapability;
import com.microsoft.hydralab.common.entity.agent.EnvCapabilityRequirement;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.util.CommandOutputReceiver;
import com.microsoft.hydralab.common.util.FileUtil;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.LogUtils;
import com.microsoft.hydralab.common.util.PythonUtil;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.slf4j.Logger;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author zhoule
 * @date 08/17/2023
 */

public class PythonRunner extends TestRunner {
    private static final int MAJOR_PYTHON_VERSION = 3;
    private static final int MINOR_PYTHON_VERSION = 8;
    private static final String TEST_RUN_NAME = "Python test";
    private static final String PY_REQUIRE_FILE_NAME = "requirements.txt";
    private static final String PY_FILE_NAME = "main.py";
    private static final String PY_FOLDER_NAME = "pythonRunner";
    private long recordingStartTimeMillis;
    private AndroidTestUnit ongoingPythonTest;
    private String pkgName;

    public PythonRunner(AgentManagementService agentManagementService,
                        TestTaskRunCallback testTaskRunCallback,
                        TestRunDeviceOrchestrator testRunDeviceOrchestrator,
                        PerformanceTestManagementService performanceTestManagementService) {
        super(agentManagementService, testTaskRunCallback, testRunDeviceOrchestrator, performanceTestManagementService);
    }

    @Override
    protected List<EnvCapabilityRequirement> getEnvCapabilityRequirements() {
        return List.of(new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.python, MAJOR_PYTHON_VERSION, MINOR_PYTHON_VERSION));
    }

    @Override
    protected void run(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun) throws Exception {
        testRun.setTotalCount(1);
        Logger logger = testRun.getLogger();
        pkgName = testTask.getPkgName();
        startTools(testRunDevice, testTask, testRun, testTask.getTimeOutSecond(), logger);

        /* run the test */
        logger.info("Start ");
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        performanceTestManagementService.testRunStarted();
        checkTestTaskCancel(testTask);
        performanceTestManagementService.testStarted(TEST_RUN_NAME);

        long checkTime = runPythonTest(testRunDevice, testTask, testRun, logger);

        /* after running */
        releaseResource(testTask, testRunDevice, testRun);
        if (checkTime > 0) {
            String crashStack = testRun.getCrashStack();
            if (StringUtils.isEmpty(crashStack)) {
                performanceTestManagementService.testSuccess(ongoingPythonTest.getTitle());
            } else {
                ongoingPythonTest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
                ongoingPythonTest.setSuccess(false);
                ongoingPythonTest.setStack(crashStack);
                performanceTestManagementService.testFailure(ongoingPythonTest.getTitle());
                testRun.addNewTimeTagBeforeLast(ongoingPythonTest.getTitle() + ".fail", checkTime);
                testRun.oneMoreFailure();
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
        recordingStartTimeMillis = System.currentTimeMillis();
        final String initializing = "Initializing";
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, initializing);
        testRun.addNewTimeTag(initializing, 0);
        testRunDeviceOrchestrator.startLogCollector(testRunDevice, pkgName, testRun, logger);
        testRun.setLogcatPath(agentManagementService.getTestBaseRelPathInUrl(new File(testRunDevice.getLogPath())));

        logger.info("Start gif frames collection");
        testRunDeviceOrchestrator.startGifEncoder(testRunDevice, testRun.getResultFolder(), pkgName + ".gif");
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
        testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.onTestEnded();
    }

    public long runPythonTest(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun, Logger logger) {
        long checkTime = 0;
        final int unitIndex = 1;
        String title = "Monkey_Test";

        ongoingPythonTest = new AndroidTestUnit();
        ongoingPythonTest.setNumtests(testRun.getTotalCount());
        ongoingPythonTest.setStartTimeMillis(System.currentTimeMillis());
        ongoingPythonTest.setRelStartTimeInVideo(ongoingPythonTest.getStartTimeMillis() - recordingStartTimeMillis);
        ongoingPythonTest.setCurrentIndexNum(unitIndex);
        ongoingPythonTest.setTestName(title);
        ongoingPythonTest.setTestedClass(pkgName);
        ongoingPythonTest.setDeviceTestResultId(testRun.getId());
        ongoingPythonTest.setTestTaskId(testRun.getTestTaskId());
        testRun.addNewTestUnit(ongoingPythonTest);

        logger.info(ongoingPythonTest.getTitle());
        testRunDeviceOrchestrator.addGifFrameAsyncDelay(testRunDevice, agentManagementService.getScreenshotDir(), 2, logger);
        //run Python test
        testRun.addNewTimeTag(unitIndex + ". " + ongoingPythonTest.getTitle(),
                System.currentTimeMillis() - recordingStartTimeMillis);
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, ongoingPythonTest.getTitle());
        /* load python package */
        File pythonMainFile = loadPythonPackage(testTask, testRun);
        if (pythonMainFile == null) {
            throw new HydraLabRuntimeException("Can not find main.py file in python package");
        }

        String[] runArgs = buildCommandArgs(testRunDevice, testRun, testTask);
        try {
            Process process = Runtime.getRuntime().exec(runArgs);
            CommandOutputReceiver receiver = new CommandOutputReceiver(process.getInputStream(), logger);
            receiver.start();
            process.waitFor();
            checkTime = System.currentTimeMillis() - recordingStartTimeMillis;
            ongoingPythonTest.setStatusCode(AndroidTestUnit.StatusCodes.OK);
            ongoingPythonTest.setSuccess(true);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);

            ongoingPythonTest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
            ongoingPythonTest.setSuccess(false);
            ongoingPythonTest.setStack(e.toString());
            testRun.setSuccess(false);
            testRun.addNewTimeTagBeforeLast(ongoingPythonTest.getTitle() + ".fail",
                    System.currentTimeMillis() - recordingStartTimeMillis);
            testRun.oneMoreFailure();
        }

        logger.info(ongoingPythonTest.getTitle() + ".end");
        ongoingPythonTest.setEndTimeMillis(System.currentTimeMillis());
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, null);
        testRun.addNewTimeTag(ongoingPythonTest.getTitle() + ".end",
                System.currentTimeMillis() - recordingStartTimeMillis);
        return checkTime;
    }

    private File loadPythonPackage(TestTask testTask, TestRun testRun) {
        File pythonPackage = testTask.getAppFile();
        File pythonFolder = new File(testRun.getResultFolder(), PY_FOLDER_NAME);
        File pythonMainFile = null;
        FileUtil.unzipFile(pythonPackage.getAbsolutePath(), pythonFolder.getAbsolutePath());
        for (File tempFile : Objects.requireNonNull(pythonFolder.listFiles())) {
            if (PY_FILE_NAME.equals(tempFile.getName())) {
                testRun.getLogger().info("python main file found: {}", tempFile.getAbsolutePath());
                pythonMainFile = tempFile;
            } else if (PY_REQUIRE_FILE_NAME.equals(tempFile.getName())) {
                PythonUtil.installRequirements(tempFile, testRun.getLogger());
            }
        }
        return pythonMainFile;
    }

    /**
     * @return {"python","path of main.py","path of test app package","device info","path of result folder","customize param 1","customize param 1",...}
     */
    private String[] buildCommandArgs(TestRunDevice testRunDevice, TestRun testRun, TestTask testTask) {
        // sample: python  d://folder/code/main.py "d://folder/package/dataset.zip" "{'deviceType':'Windows','deviceName':'***'}" "d://folder/resultRoot" "modelA" "modelB"
        Map<String, String> customArgMap = testTask.getInstrumentationArgs();
        if (customArgMap == null) {
            customArgMap = new HashMap<>();
        }
        String[] customArgs = customArgMap.values().toArray(new String[0]);

        // copy testAppPackage to result folder
        File testAppPackage = FileUtil.copyFileToFolder(testTask.getTestAppFile(), new File(testRun.getResultFolder(), PY_FOLDER_NAME));

        // build command
        String[] args = new String[5 + customArgs.length];
        args[0] = "python";
        args[1] = new File(testRun.getResultFolder(), PY_FOLDER_NAME + "/" + PY_FILE_NAME).getAbsolutePath();
        args[2] = testAppPackage.getAbsolutePath();
        args[3] = JSONObject.toJSONString(testRunDevice.getDeviceInfo()).replaceAll("\"", "'");
        args[4] = testRun.getResultFolder().getAbsolutePath();
        testRun.getLogger().info(String.format("Python default commands: %s", Arrays.toString(args)));

        for (int i = 0; i < customArgs.length; i++) {
            args[5 + i] = customArgs[i].replaceAll("\"", "'");
        }
        testRun.getLogger().info("Python custom commands: {} ", LogUtils.scrubSensitiveArgs(customArgMap.toString()));
        return args;
    }
}
