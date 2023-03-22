package com.microsoft.hydralab.agent.runner.xctest;

import cn.hutool.core.img.gif.AnimatedGifEncoder;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.device.TestDevice;
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
    private static String folderPath = "";
    private File gifFile;
    private final AnimatedGifEncoder gitEncoder = new AnimatedGifEncoder();
    private Logger reportLogger;
    private long recordingStartTimeMillis;

    public XCTestRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                        PerformanceTestManagementService performanceTestManagementService) {
        super(agentManagementService, testTaskRunCallback, performanceTestManagementService);
    }

    @Override
    protected void run(TestDevice testDevice, TestTask testTask, TestRun testRun) throws Exception {
        initializeTest(testDevice, testTask, testRun);
        unzipXctestFolder(testTask.getAppFile(), testRun, reportLogger);
        List<String> result = runXctest(testDevice.getDeviceInfo(), reportLogger, testTask, testRun);
        analysisXctestResult(result, testRun);
        FileUtil.deleteFile(new File(folderPath));
        finishTest(testDevice, testRun);
    }

    private void initializeTest(TestDevice testDevice, TestTask testTask, TestRun testRun) {
        reportLogger = testRun.getLogger();
        testDevice.startScreenRecorder(testRun.getResultFolder(), testTask.getTimeOutSecond(), reportLogger);
        recordingStartTimeMillis = System.currentTimeMillis();
        testRun.addNewTimeTag("Initializing", 0);
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        reportLogger.info("Start gif frames collection");
        gifFile = new File(testRun.getResultFolder(), testTask.getPkgName() + ".gif");
        gitEncoder.start(gifFile.getAbsolutePath());
        gitEncoder.setDelay(1000);
        gitEncoder.setRepeat(0);
    }

    @Override
    protected void reInstallApp(TestDevice testDevice, TestTask testTask, Logger reportLogger) {
    }

    private void unzipXctestFolder(File zipFile, TestRun testRun, Logger reportLogger) {
        reportLogger.info("start unzipping file");
        folderPath = testRun.getResultFolder().getAbsolutePath() + "/"
                + Const.XCTestConfig.XCTEST_ZIP_FOLDER_NAME + "/";

        String command = String.format("unzip -d %s %s", folderPath, zipFile.getAbsolutePath());
        ShellUtils.execLocalCommand(command, reportLogger);
    }

    private ArrayList<String> runXctest(DeviceInfo deviceInfo, Logger logger,
                                        TestTask testTask, TestRun testRun) {
        if (deviceInfo == null) {
            throw new RuntimeException("No such device: " + deviceInfo);
        }
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

        String deviceId = "id=" + deviceInfo.getDeviceId();
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

    private void finishTest(TestDevice testDevice, TestRun testRun) {
        testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.onTestEnded();
        String absoluteReportPath = testRun.getResultFolder().getAbsolutePath();
        testRun.setTestXmlReportPath(
                agentManagementService.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
        if (gifFile.exists() && gifFile.length() > 0) {
            testRun.setTestGifPath(agentManagementService.getTestBaseRelPathInUrl(gifFile));
        }
        gitEncoder.finish();
        testDevice.stopScreenRecorder();
    }
}
