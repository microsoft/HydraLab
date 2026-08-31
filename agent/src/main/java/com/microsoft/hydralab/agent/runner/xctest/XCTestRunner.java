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
import com.microsoft.hydralab.common.util.IOSUtils;
import com.microsoft.hydralab.common.util.ShellUtils;
import com.microsoft.hydralab.common.util.ThreadUtils;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class XCTestRunner extends TestRunner {
    private static final int MAJOR_APPIUM_VERSION = 1;
    private static final int MINOR_APPIUM_VERSION = -1;
    private static final int MAJOR_TIDEVICE_VERSION = 0;
    private static final int MINOR_TIDEVICE_VERSION = 10;

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
        try {
            unzipXctestFolder(testTask.getAppFile(), testRun, testRun.getLogger());
            List<String> result = runXctest(testRunDevice, testRun.getLogger(), testTask, testRun);
            analysisXctestResult(result, testRun);
            FileUtil.deleteFile(new File(testRun.getResultFolder().getAbsolutePath(), Const.XCTestConfig.XCTEST_ZIP_FOLDER_NAME));
        } finally {
            // finishTest must always run to gracefully stop ffmpeg, kill WDA,
            // release port forwarding, and stop log collection.
            finishTest(testRunDevice, testTask, testRun);
        }
    }

    private void initializeTest(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun) {
        if (!testTask.isDisableRecording()) {
            testRunDeviceOrchestrator.startScreenRecorder(testRunDevice, testRun.getResultFolder(), testTask.getTimeOutSecond(), testRun.getLogger());
        }
        testRunDeviceOrchestrator.startLogCollector(testRunDevice, testTask.getPkgName(), testRun, testRun.getLogger());
        if(!testTask.isDisableGifEncoder()) {
            testRunDeviceOrchestrator.startGifEncoder(testRunDevice, testRun.getResultFolder(), testTask.getPkgName() + ".gif");
        }
        testRun.setLogcatPath(agentManagementService.getTestBaseRelPathInUrl(new File(testRunDevice.getLogPath())));
        testRun.addNewTimeTag("Initializing", 0);
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
    }

    @Override
    protected void reInstallApp(TestRunDevice testRunDevice, TestTask testTask, Logger logger) {
        checkTestTaskCancel(testTask);
        if (testTask.getNeedUninstall()) {
            testRunDeviceOrchestrator.uninstallApp(testRunDevice, testTask.getPkgName(), logger);
            ThreadUtils.safeSleep(3000);
        } else if (testTask.getNeedClearData()) {
            testRunDeviceOrchestrator.resetPackage(testRunDevice, testTask.getPkgName(), logger);
        }
    }

    private void unzipXctestFolder(File zipFile, TestRun testRun, Logger logger) {
        logger.info("start unzipping file");
        String folderPath = testRun.getResultFolder().getAbsolutePath() + "/"
                + Const.XCTestConfig.XCTEST_ZIP_FOLDER_NAME + "/";

        String command = String.format("unzip -d %s %s", folderPath, zipFile.getAbsolutePath());
        ShellUtils.execLocalCommand(command, logger);
    }

    private ArrayList<String> runXctest(TestRunDevice testRunDevice, Logger logger,
                                        TestTask testTask, TestRun testRun) {
        if (testRunDevice.getDeviceInfo() == null) {
            throw new RuntimeException("No such device: " + testRunDevice.getDeviceInfo());
        }
        if (!testTask.isDisableGifEncoder()) {
            testRunDeviceOrchestrator.addGifFrameAsyncDelay(testRunDevice, agentManagementService.getScreenshotDir(), 0, logger);
        }
        StringBuilder argString = new StringBuilder();
        Map<String, String> instrumentationArgs = testTask.getTaskRunArgs();
        if (instrumentationArgs != null && !instrumentationArgs.isEmpty()) {
            instrumentationArgs.forEach((k, v) -> argString.append(" ").append(k).append(" ").append(v));
        }
        String commFormat;
        if (StringUtils.isBlank(argString.toString())) {
            commFormat = "xcodebuild test-without-building";
        } else {
            commFormat = "xcodebuild test-without-building" + argString;
        }

        File xctestproducts = getXctestproductsFile(new File(testRun.getResultFolder().getAbsolutePath(), Const.XCTestConfig.XCTEST_ZIP_FOLDER_NAME));
        if (xctestproducts != null) {
            commFormat += " -testProductsPath " + xctestproducts.getAbsolutePath();

            if (StringUtils.isNotBlank(testTask.getTestPlan())) {
                commFormat += " -testPlan " + testTask.getTestPlan();
            }
        } else {
            File xctestrun = getXctestrunFile(new File(testRun.getResultFolder().getAbsolutePath(), Const.XCTestConfig.XCTEST_ZIP_FOLDER_NAME));
            if (xctestrun == null) {
                throw new RuntimeException("xctestrun file not found");
            }

            commFormat += " -xctestrun " + xctestrun.getAbsolutePath();
        }

        String deviceId = "id=" + testRunDevice.getDeviceInfo().getDeviceId();
        String resultPath = testRun.getResultFolder().getAbsolutePath()
                + "/" + Const.XCTestConfig.XCTEST_RESULT_FILE_NAME;

        commFormat += " -destination %s -resultBundlePath %s";
        String command = String.format(commFormat, deviceId, resultPath);
        testRun.addNewTimeTag("testRunStarted", System.currentTimeMillis() - testRun.getTestStartTimeMillis());
        ArrayList<String> result;
        try {
            Process proc = Runtime.getRuntime().exec(command);
            XCTestCommandReceiver err = new XCTestCommandReceiver(proc.getErrorStream(), logger);
            XCTestCommandReceiver out = new XCTestCommandReceiver(proc.getInputStream(), logger);
            err.start();
            out.start();
            // On iOS 17+, WDA and test both run via xcodebuild on the same device
            // (Apple changed testmanagerd.lockdown.secure, making xcodebuild the only
            // way to launch WDA). This can cause xcodebuild to hang at cleanup.
            // Poll for test completion in output rather than waiting for process exit,
            // since xcodebuild may hang indefinitely at cleanup.
            long deadline = System.currentTimeMillis() + testTask.getTimeOutSecond() * 1000L;
            while (System.currentTimeMillis() < deadline) {
                // Check both stdout and stderr — xcodebuild may emit completion
                // markers (e.g. "** TEST SUCCEEDED **") on either stream
                if (!proc.isAlive() || out.isTestComplete() || err.isTestComplete()) {
                    break;
                }
                Thread.sleep(1000);
            }
            // Give a short grace period for xcodebuild to finish cleanly
            if (proc.isAlive()) {
                boolean exited = proc.waitFor(30, TimeUnit.SECONDS);
                if (!exited) {
                    logger.warn("xcodebuild hanging at cleanup, force killing");
                    proc.destroyForcibly();
                    proc.waitFor(5, TimeUnit.SECONDS);
                }
            }
            result = out.getResult();
            if (!testTask.isDisableGifEncoder()) {
                testRunDeviceOrchestrator.addGifFrameAsyncDelay(testRunDevice, agentManagementService.getScreenshotDir(), 0, logger);
            }
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

    private File getXctestproductsFile(File unzippedFolder) {
        Collection<File> files = FileUtils.listFilesAndDirs(unzippedFolder,
                new NotFileFilter(TrueFileFilter.INSTANCE),
                DirectoryFileFilter.DIRECTORY);
        for (File file : files) {
            if (file.getAbsolutePath().endsWith(".xctestproducts")
                    && file.isDirectory()
                    && !file.getAbsolutePath().contains("__MACOSX")) {
                return file;
            }
        }
        return null;
    }

    private void analysisXctestResult(List<String> resultList, TestRun testRun) {
        int totalCases = 0;
        for (String resultLine : resultList) {
            if (resultLine.toLowerCase().startsWith("test case") && !resultLine.contains("started")) {
                AndroidTestUnit ongoingXctest = new AndroidTestUnit();
                String testInfo = resultLine.split("'")[1];
                String testedClass;
                String testName;
                if (testInfo.contains(".")) {
                    // Swift format: "ClassName.testMethodName"
                    String[] parts = testInfo.split("\\.");
                    testedClass = parts[0].replaceAll("[^a-zA-Z0-9_]", "");
                    testName = parts.length > 1 ? parts[1].replaceAll("[^a-zA-Z0-9_]", "") : testedClass;
                } else {
                    // Objective-C format: "-[ClassName testMethodName]"
                    String cleaned = testInfo.replaceAll("[\\[\\]\\-]", "").trim();
                    String[] parts = cleaned.split("\\s+", 2);
                    testedClass = parts[0];
                    testName = parts.length > 1 ? parts[1] : testedClass;
                }
                ongoingXctest.setTestName(testName);
                ongoingXctest.setTestedClass(testedClass);
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

    private void finishTest(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun) {
        testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - testRun.getTestStartTimeMillis());
        testRun.onTestEnded();
        String absoluteReportPath = testRun.getResultFolder().getAbsolutePath();
        testRun.setTestXmlReportPath(agentManagementService.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
        if (!testTask.isDisableGifEncoder()) {
            testRun.setTestGifPath(agentManagementService.getTestBaseRelPathInUrl(testRunDevice.getGifFile()));
        }
        if (!testTask.isDisableGifEncoder()) {
            testRunDeviceOrchestrator.stopGitEncoder(testRunDevice, agentManagementService.getScreenshotDir(), testRun.getLogger());
        }
        if (!testTask.isDisableRecording()) {
            String videoFilePath = testRunDeviceOrchestrator.stopScreenRecorder(testRunDevice, testRun.getResultFolder(), testRun.getLogger());
            testRun.setVideoPath(agentManagementService.getTestBaseRelPathInUrl(videoFilePath));
        }
        // Kill WDA proxy to release device resources. On iOS 17+, WDA runs via
        // xcodebuild which holds device resources after test completion.
        IOSUtils.killProxyWDA(testRunDevice.getDeviceInfo(), testRun.getLogger());
        testRunDeviceOrchestrator.stopLogCollector(testRunDevice);
    }
}
