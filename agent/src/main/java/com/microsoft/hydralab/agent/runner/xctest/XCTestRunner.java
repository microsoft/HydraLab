package com.microsoft.hydralab.agent.runner.xctest;

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
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.FileUtil;
import com.microsoft.hydralab.common.util.ShellUtils;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class XCTestRunner extends TestRunner {
    private static final int MAJOR_APPIUM_VERSION = 1;
    private static final int MINOR_APPIUM_VERSION = -1;
    private static final int MAJOR_TIDEVICE_VERSION = 0;
    private static final int MINOR_TIDEVICE_VERSION = 10;
    private static String folderPath = "";
    private Logger logger;
    private long recordingStartTimeMillis;

    public XCTestRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                        TestRunDeviceOrchestrator testRunDeviceOrchestrator, PerformanceTestManagementService performanceTestManagementService) {
        super(agentManagementService, testTaskRunCallback, testRunDeviceOrchestrator, performanceTestManagementService);
    }

    @Override
    protected List<EnvCapabilityRequirement> getEnvCapabilityRequirements() {
        return List.of(new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.appium, MAJOR_APPIUM_VERSION, MINOR_APPIUM_VERSION),
                new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.tidevice, MAJOR_TIDEVICE_VERSION, MINOR_TIDEVICE_VERSION));
    }

    @Override
    protected void run(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun) throws Exception {
        initializeTest(testRunDevice, testTask, testRun);
        unzipXctestFolder(testTask.getAppFile(), testRun, logger);
        List<String> result = runXctest(testRunDevice, logger, testTask, testRun);
        analysisXctestResult(result, testRun);
        FileUtil.deleteFile(new File(folderPath));
        finishTest(testRunDevice, testRun);
    }

    private void initializeTest(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun) {
        logger = testRun.getLogger();
        testRunDeviceOrchestrator.startScreenRecorder(testRunDevice, testRun.getResultFolder(), testTask.getTimeOutSecond(), logger);
        testRunDeviceOrchestrator.startLogCollector(testRunDevice, testTask.getPkgName(), testRun, logger);
        testRunDeviceOrchestrator.startGifEncoder(testRunDevice, testRun.getResultFolder(), testTask.getPkgName() + ".gif");
        testRun.setLogcatPath(agentManagementService.getTestBaseRelPathInUrl(new File(testRunDevice.getLogPath())));
        recordingStartTimeMillis = System.currentTimeMillis();
        testRun.addNewTimeTag("Initializing", 0);
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
    }

    @Override
    protected void reInstallApp(TestRunDevice testRunDevice, TestTask testTask, Logger logger) {
    }

    private void unzipXctestFolder(File zipFile, TestRun testRun, Logger logger) {
        logger.info("start unzipping file");
        folderPath = testRun.getResultFolder().getAbsolutePath() + "/"
                + Const.XCTestConfig.XCTEST_ZIP_FOLDER_NAME + "/";

        String command = String.format("unzip -d %s %s", folderPath, zipFile.getAbsolutePath());
        ShellUtils.execLocalCommand(command, logger);
    }

    private ArrayList<String> runXctest(TestRunDevice testRunDevice, Logger logger,
                                        TestTask testTask, TestRun testRun) {
        if (testRunDevice.getDeviceInfo() == null) {
            throw new RuntimeException("No such device: " + testRunDevice.getDeviceInfo());
        }
        testRunDeviceOrchestrator.addGifFrameAsyncDelay(testRunDevice, agentManagementService.getScreenshotDir(), 0, logger);
        StringBuilder argString = new StringBuilder();
        Map<String, String> instrumentationArgs = testTask.getInstrumentationArgs();
        if (instrumentationArgs != null && !instrumentationArgs.isEmpty()) {
            instrumentationArgs.forEach((k, v) -> argString.append(" ").append(k).append(" ").append(v));
        }
        String commFormat;
        if (StringUtils.isBlank(argString.toString())) {
            commFormat = "xcodebuild test-without-building";
        } else {
            commFormat = "xcodebuild test-without-building" + argString;
        }
        File xctestrun = getXctestrunFile(new File(folderPath));
        if (xctestrun == null) {
            throw new RuntimeException("xctestrun file not found");
        }

        commFormat += " -xctestrun " + xctestrun.getAbsolutePath();

        String deviceId = "id=" + testRunDevice.getDeviceInfo().getDeviceId();
        String resultPath = testRun.getResultFolder().getAbsolutePath()
                + "/" + Const.XCTestConfig.XCTEST_RESULT_FILE_NAME;

        commFormat += " -destination %s -resultBundlePath %s";
        String command = String.format(commFormat, deviceId, resultPath);
        testRun.addNewTimeTag("testRunStarted", System.currentTimeMillis() - recordingStartTimeMillis);
        ArrayList<String> result;
        try {
            Process proc = Runtime.getRuntime().exec(command);
            XCTestCommandReceiver err = new XCTestCommandReceiver(proc.getErrorStream(), logger);
            XCTestCommandReceiver out = new XCTestCommandReceiver(proc.getInputStream(), logger);
            err.start();
            out.start();
            proc.waitFor();
            result = out.getResult();
            testRunDeviceOrchestrator.addGifFrameAsyncDelay(testRunDevice, agentManagementService.getScreenshotDir(), 0, logger);
        } catch (Exception e) {
            throw new RuntimeException("Execute XCTest failed");
        }

        if (result == null) {
            throw new RuntimeException("No result collected");
        }
        return result;
    }

    private File getXctestrunFile(File unzippedFolder) {
        Collection<File> files = FileUtils.listFiles(unzippedFolder, null, true);
        for (File file : files) {
            if (file.getAbsolutePath().endsWith(".xctestrun")
                    && !file.getAbsolutePath().contains("__MACOSX")) {
                return file;
            }
        }
        return null;
    }

    private void analysisXctestResult(List<String> resultList, TestRun testRun) {
        int totalCases = 0;
        for (String resultLine : resultList
        ) {
            if (resultLine.toLowerCase().startsWith("test case") && !resultLine.contains("started")) {
                AndroidTestUnit ongoingXctest = new AndroidTestUnit();
                String testInfo = resultLine.split("'")[1];
                ongoingXctest.setTestName(testInfo.split("\\.")[1].replaceAll("[^a-zA-Z0-9_]", ""));
                ongoingXctest.setTestedClass(testInfo.split("\\.")[0].replaceAll("[^a-zA-Z0-9_]", ""));
                ongoingXctest.setDeviceTestResultId(testRun.getId());
                ongoingXctest.setTestTaskId(testRun.getTestTaskId());
                if (resultLine.contains("passed")) {
                    ongoingXctest.setStatusCode(AndroidTestUnit.StatusCodes.OK);
                    ongoingXctest.setSuccess(true);
                } else if (resultLine.contains("skipped")) {
                    ongoingXctest.setStatusCode(AndroidTestUnit.StatusCodes.IGNORED);
                    ongoingXctest.setSuccess(true);
                } else {
                    ongoingXctest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
                    ongoingXctest.setSuccess(false);
                }
                testRun.addNewTestUnit(ongoingXctest);
                totalCases += 1;
            }
        }
        testRun.setTotalCount(totalCases);
    }

    private void finishTest(TestRunDevice testRunDevice, TestRun testRun) {
        testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.onTestEnded();
        String absoluteReportPath = testRun.getResultFolder().getAbsolutePath();
        testRun.setTestXmlReportPath(agentManagementService.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
        testRun.setTestGifPath(agentManagementService.getTestBaseRelPathInUrl(testRunDevice.getGifFile()));
        testRunDeviceOrchestrator.stopGitEncoder(testRunDevice, agentManagementService.getScreenshotDir(), logger);
        testRunDeviceOrchestrator.stopScreenRecorder(testRunDevice, testRun.getResultFolder(), logger);
        testRunDeviceOrchestrator.stopLogCollector(testRunDevice);
    }
}
