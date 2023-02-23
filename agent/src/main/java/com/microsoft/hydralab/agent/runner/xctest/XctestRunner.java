package com.microsoft.hydralab.agent.runner.xctest;

import com.android.ddmlib.IShellOutputReceiver;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.FileUtil;
import com.microsoft.hydralab.common.util.ShellUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class XctestRunner extends TestRunner {
    private static String zipPath = "";
    private static String folderPath = "";
    private static String resultPath = "";

    public XctestRunner(DeviceManager deviceManager, TestTaskRunCallback testTaskRunCallback) {
        super(deviceManager, testTaskRunCallback);
    }

    @Override
    protected void run(DeviceInfo deviceInfo, TestTask testTask, TestRun testRun) throws Exception {
        Logger reportLogger = testRun.getLogger();
        ScreenRecorder deviceScreenRecorder = deviceManager.getScreenRecorder(deviceInfo, testRun.getResultFolder(), reportLogger);
        deviceScreenRecorder.setupDevice();
        deviceScreenRecorder.startRecord(testTask.getTimeOutSecond());
        // Need a location here
        unzipXctestFolder(testTask.getAppFile(), testRun, reportLogger);
        runXctest();
        deviceScreenRecorder.finishRecording();
        analysisXctestResult();

    }

    private void unzipXctestFolder(File zipFile, TestRun testRun, Logger reportLogger) {
        reportLogger.info("start unzipping file");
        zipPath = zipFile.getAbsolutePath() + "/" + Const.SmartTestConfig.ZIP_FOLDER_NAME + "/";
        folderPath = testRun.getResultFolder().getAbsolutePath() + "/" + Const.SmartTestConfig.XCTEST_ZIP_FOLDER_NAME
                + "/";
        resultPath = testRun.getResultFolder().getAbsolutePath();

        FileUtil.unzipFile(zipFile.getAbsolutePath(), folderPath);
    }

    private String runXctest(DeviceInfo deviceInfo, String scope, String suiteName,
                             String testPkgName, String testRunnerName, Logger logger,
                             IShellOutputReceiver receiver, TestRun testRun,
                             int testTimeOutSec, Map<String, String> instrumentationArgs) {
        if (deviceInfo == null) {
            throw new RuntimeException("No such device: " + deviceInfo);
        }
        if (testTimeOutSec <= 0) {
            // the test should not last longer than
            testTimeOutSec = 45 * 60;
        }
        StringBuilder argString = new StringBuilder();
        if (instrumentationArgs != null && !instrumentationArgs.isEmpty()) {
            instrumentationArgs.forEach((k, v) -> argString.append(" ").append(k).append(" ").append(v));
        }
        String commFormat;
        if (StringUtils.isBlank(argString.toString())) {
            commFormat = "xcodebuild test-without-building";
        } else {
            commFormat = "xcodebuild test-without-building" + argString;
        }

        try {
            String deviceId = "id=" + deviceInfo.getDeviceId();
            String resultPath = testRun.getResultFolder().getAbsolutePath() + "/" + "result";

            commFormat += " -destination \"id=%s\" -resultBundlePath %s";
            String command = String.format(commFormat, deviceId, resultPath);

            String result = ShellUtils.execLocalCommandWithResult(command, logger);
            return Const.TaskResult.SUCCESS;
        } catch (Exception e) {
            if (logger != null) {
                logger.error(e.getMessage(), e);
            }
            return e.getMessage();
        }
    }

    private void analysisXctestResult() {

    }
}
