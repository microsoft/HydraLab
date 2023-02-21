package com.microsoft.hydralab.agent.runner.xctest;

import com.android.ddmlib.IShellOutputReceiver;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.FileUtil;
import com.microsoft.hydralab.common.util.LogUtils;
import com.microsoft.hydralab.common.util.ShellUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;

import cn.hutool.log.Log;

public class XctestRunner extends TestRunner {
    private static String zipPath = "";
    private static String folderPath = "";
    private static String folderNamePath = "";

    public XctestRunner(DeviceManager deviceManager, TestTaskRunCallback testTaskRunCallback) {
        super(deviceManager, testTaskRunCallback);
    }

    @Override
    protected void run(DeviceInfo deviceInfo, TestTask testTask, TestRun testRun) throws Exception {
        Logger reportLogger = testRun.getLogger();
        // Need a location here
        unzipXctestFolder("location", reportLogger);
        runXctest();
    }

    private void unzipXctestFolder(String location, Logger reportLogger) {
        File testBaseDir = new File(location);
        String zipName = Const.SmartTestConfig.XCTEST_ZIP_FILE_NAME;
        String folderName = Const.SmartTestConfig.XCTEST_ZIP_FOLDER_NAME;

        zipPath = testBaseDir.getAbsolutePath() + "/" + Const.SmartTestConfig.ZIP_FOLDER_NAME + "/";
        folderPath = testBaseDir.getAbsolutePath() + "/" + Const.SmartTestConfig.XCTEST_ZIP_FOLDER_NAME
                + "/";

        try {
            reportLogger.info("Start install xctest files");
            InputStream resourceAsStream = FileUtils.class.getClassLoader().getResourceAsStream(zipName);
            if (resourceAsStream == null) {
                return;
            }
            File xctestZip = new File(testBaseDir, zipName);
            File xctestFolder = new File(testBaseDir, folderName);
            if (xctestZip.exists()) {
                FileUtil.deleteFileRecursively(xctestZip);
            }
            if (xctestFolder.exists()) {
                FileUtil.deleteFileRecursively(xctestFolder);
            }
            OutputStream out = new FileOutputStream(xctestZip);
            IOUtils.copy(Objects.requireNonNull(resourceAsStream), out);
            out.close();
            FileUtil.unzipFile(xctestZip.getAbsolutePath(), testBaseDir.getAbsolutePath());
            if (xctestZip.exists()) {
                FileUtil.deleteFileRecursively(xctestZip);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String runXctest(DeviceInfo deviceInfo, String scope, String suiteName,
                             String testPkgName, String testRunnerName, Logger logger,
                             IShellOutputReceiver receiver,
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
            instrumentationArgs.forEach((k, v) -> argString.append(" -e ").append(k.replaceAll("\\s|\"", "")).append(" ").append(v.replaceAll("\\s|\"", "")));
        }
        String commFormat;
        if (StringUtils.isBlank(argString.toString())) {
            commFormat = "xcodebuild test-without-building";
        } else {
            commFormat = "xcodebuild test-without-building" + argString;
        }

        try {
            String destination = "name=iPhone 14 Pro";
            String resultPath = folderPath + "/" + "result";

            commFormat += " -destination %s -resultBundlePath %s";
            String command = String.format(commFormat, destination, resultPath);

            String result = ShellUtils.execLocalCommandWithResult(command, logger);
            return Const.TaskResult.SUCCESS;
        } catch (Exception e) {
            if (logger != null) {
                logger.error(e.getMessage(), e);
            }
            return e.getMessage();
        }
    }
}
