package com.microsoft.hydralab.agent.runner.xctest;

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
import com.microsoft.hydralab.performance.PerformanceTestManagementService;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.util.Collection;
import java.util.Map;

public class XctestRunner extends TestRunner {
    private static String folderPath = "";

    public XctestRunner(DeviceManager deviceManager, TestTaskRunCallback testTaskRunCallback,
                        PerformanceTestManagementService performanceTestManagementService) {
        super(deviceManager, testTaskRunCallback, performanceTestManagementService);
    }

    @Override
    protected void run(DeviceInfo deviceInfo, TestTask testTask, TestRun testRun) throws Exception {
        Logger reportLogger = testRun.getLogger();
        ScreenRecorder deviceScreenRecorder = deviceManager.getScreenRecorder(deviceInfo, testRun.getResultFolder(), reportLogger);
        deviceScreenRecorder.setupDevice();
        deviceScreenRecorder.startRecord(testTask.getTimeOutSecond());
        unzipXctestFolder(testTask.getAppFile(), testRun, reportLogger);
        String result = runXctest(deviceInfo, reportLogger, testTask, testRun);
        deviceScreenRecorder.finishRecording();
        analysisXctestResult(result);
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

        String result;
        try {
            String deviceId = "id=" + deviceInfo.getDeviceId();
            String resultPath = testRun.getResultFolder().getAbsolutePath() + "/" + "result";

            commFormat += " -destination \"id=%s\" -resultBundlePath %s";
            String command = String.format(commFormat, deviceId, resultPath);
            result = ShellUtils.execLocalCommandWithResult(command, logger);
        } catch (Exception e) {
            if (logger != null) {
                logger.error(e.getMessage(), e);
            }
            return e.getMessage();
        }
        return result;
    }

    private void analysisXctestResult(String result) {

    }

    private File getXctestrunFile(File unzippedFolder) {
        try {
            Collection<File> files = FileUtils.listFiles(unzippedFolder, null, false);
            for (File file : files) {
                if (file.getAbsolutePath().endsWith(".xctestrun")){
                    return file;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }
}
