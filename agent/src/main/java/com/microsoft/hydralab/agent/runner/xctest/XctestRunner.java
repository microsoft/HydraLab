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
import java.util.Objects;

import cn.hutool.log.Log;

public class XctestRunner extends TestRunner {
    private static String filePath = "";
    private static String folderPath = "";

    public XctestRunner(DeviceManager deviceManager, TestTaskRunCallback testTaskRunCallback) {
        super(deviceManager, testTaskRunCallback);
    }

    @Override
    protected void run(DeviceInfo deviceInfo, TestTask testTask, TestRun testRun) throws Exception {
        Logger reportLogger = testRun.getLogger();
        unzipXctestFolder("location", reportLogger);
        runXctest();
    }

    private void unzipXctestFolder(String location, Logger reportLogger) {
        File testBaseDir = new File(location);
        String name = Const.SmartTestConfig.XCTEST_ZIP_FILE_NAME;

        folderPath = testBaseDir.getAbsolutePath() + "/" + Const.SmartTestConfig.ZIP_FOLDER_NAME + "/";
        stringFolderPath = testBaseDir.getAbsolutePath() + "/" + Const.SmartTestConfig.XCTEST_ZIP_FOLDER_NAME
                + "/";


        try {
            reportLogger.info("Start instrumenting the test");
            InputStream resourceAsStream = FileUtils.class.getClassLoader().getResourceAsStream(name);
            if (resourceAsStream == null) {
                return;
            }
            File xctestZip = new File(testBaseDir, name);
            File smartTestFolder = new File(testBaseDir, folderName);
            if (xctestZip.exists()) {
                FileUtil.deleteFileRecursively(xctestZip);
            }
            if (smartTestFolder.exists()) {
                FileUtil.deleteFileRecursively(smartTestFolder);
            }
            OutputStream out = new FileOutputStream(xctestZip);
            IOUtils.copy(Objects.requireNonNull(resourceAsStream), out);
            out.close();
            FileUtil.unzipFile(xctestZip.getAbsolutePath(), testBaseDir.getAbsolutePath());
            if (smartTestZip.exists()) {
                FileUtil.deleteFileRecursively(smartTestZip);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private String runXctest(DeviceInfo deviceInfo, String scope, String suiteName, String testPkgName, String testRunnerName, Logger logger, IShellOutputReceiver
            receiver,
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
            commFormat = "am instrument -w -r -e debug false";
        } else {
            commFormat = "am instrument -w -r" + argString + " -e debug false";
        }

        try {
            String command;
            switch (scope) {
                case TestTask.TestScope.TEST_APP:
                    commFormat += " %s/%s";
                    command = String.format(commFormat, testPkgName, testRunnerName);
                    break;
                case TestTask.TestScope.PACKAGE:
                    commFormat += " -e package %s %s/%s";
                    command = String.format(commFormat, suiteName, testPkgName, testRunnerName);
                    break;
                // Const.TestScope.CLASS
                default:
                    commFormat += " -e class %s %s/%s";
                    command = String.format(commFormat, suiteName, testPkgName, testRunnerName);
                    break;
            }

            if (logger != null) {
                // make sure pass is not printed
                logger.info(">> adb -s {} shell {}", deviceInfo.getSerialNum(), LogUtils.scrubSensitiveArgs(command));
            }
            String result = ShellUtils.execLocalCommandWithResult(
                    "xcodebuild test-without-building -destination " + destination + " -resultBundlePath ", logger);
            return Const.TaskResult.SUCCESS;
        } catch (Exception e) {
            if (logger != null) {
                logger.error(e.getMessage(), e);
            }
            return e.getMessage();
        }
    }
}
