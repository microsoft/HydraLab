package com.microsoft.hydralab.agent.runner.xctest;

import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.FileUtil;
import com.microsoft.hydralab.common.util.ShellUtils;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.util.Collection;
import java.util.Map;

public class XCTestRunner extends TestRunner {
    private static String folderPath = "";

    public XCTestRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                        PerformanceTestManagementService performanceTestManagementService) {
        super(agentManagementService, testTaskRunCallback, performanceTestManagementService);
    }

    @Override
    protected void run(DeviceInfo deviceInfo, TestTask testTask, TestRun testRun) throws Exception {
        Logger reportLogger = testRun.getLogger();
        ScreenRecorder deviceScreenRecorder = deviceInfo.getTestDeviceManager().getScreenRecorder(deviceInfo, testRun.getResultFolder(), reportLogger);
        deviceScreenRecorder.setupDevice();
        deviceScreenRecorder.startRecord(testTask.getTimeOutSecond());
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        unzipXctestFolder(testTask.getAppFile(), testRun, reportLogger);
        String result = runXctest(deviceInfo, reportLogger, testTask, testRun);
        deviceScreenRecorder.finishRecording();
        analysisXctestResult(result, testRun);
        FileUtil.deleteFile(new File(folderPath));
        testRun.onTestEnded();
    }

    @Override
    protected void reInstallApp(DeviceInfo deviceInfo, TestTask testTask, Logger reportLogger) {
    }

    private void unzipXctestFolder(File zipFile, TestRun testRun, Logger reportLogger) {
        reportLogger.info("start unzipping file");
        folderPath = testRun.getResultFolder().getAbsolutePath() + "/" + Const.SmartTestConfig.XCTEST_ZIP_FOLDER_NAME
                + "/";

        FileUtil.unzipFile(zipFile.getAbsolutePath(), folderPath);
    }

    private String runXctest(DeviceInfo deviceInfo, Logger logger,
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
        String resultPath = testRun.getResultFolder().getAbsolutePath() + "/" + "result.xcresult";

        commFormat += " -destination \"id=%s\" -resultBundlePath %s";
        String command = String.format(commFormat, deviceId, resultPath);

        String result = ShellUtils.execLocalCommandWithResult(command, logger);

        if (result == null) {
            throw new RuntimeException("Execute XCTest failed");
        }
        return result;
    }

    private void analysisXctestResult(String result, TestRun testRun) {
        String[] resultList = result.split("/n");

        int totalCases = getTotalTestCase(result);

        for (String resultLine : resultList
        ) {
            if (resultLine.startsWith("Test case") && !resultLine.contains("started")) {
                AndroidTestUnit ongoingXctest = new AndroidTestUnit();
                ongoingXctest.setNumtests(totalCases);
                String testInfo = resultLine.split("'")[1];
                ongoingXctest.setTestName(testInfo.split("\\.")[1].replaceAll("[^a-zA-Z0-9_]", ""));
                ongoingXctest.setTestedClass(testInfo.split("\\.")[0].replaceAll("[^a-zA-Z0-9_]", ""));
                ongoingXctest.setDeviceTestResultId(testRun.getId());
                ongoingXctest.setTestTaskId(testRun.getTestTaskId());
                if (resultLine.contains("passed")) {
                    ongoingXctest.setStatusCode(AndroidTestUnit.StatusCodes.OK);
                    ongoingXctest.setSuccess(true);
                } else {
                    ongoingXctest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
                    ongoingXctest.setSuccess(false);
                }
                testRun.addNewTestUnit(ongoingXctest);
            }
        }
    }

    private int getTotalTestCase(String result) {
        String targetString = "Test case";
        int len = targetString.length();
        int count = 0;
        String localResult = result;
        while (localResult.contains(targetString)) {
            count += 1;
            localResult = result.substring(result.indexOf(targetString) + len);
        }
        return count;
    }

    private File getXctestrunFile(File unzippedFolder) {
        Collection<File> files = FileUtils.listFiles(unzippedFolder, null, false);
        for (File file : files) {
            if (file.getAbsolutePath().endsWith(".xctestrun")) {
                return file;
            }
        }
        return null;

    }
}
