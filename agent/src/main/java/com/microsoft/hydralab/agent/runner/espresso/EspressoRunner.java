// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner.espresso;

import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.testrunner.InstrumentationResultParser;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.LogUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Map;

@Service("espressoRunner")
public class EspressoRunner extends TestRunner {
    @Resource
    ADBOperateUtil adbOperateUtil;

    @Override
    public void runTestOnDevice(TestTask testTask, DeviceInfo deviceInfo, Logger logger) {
        runEspressoTest(deviceInfo, testTask, testTaskRunCallback, logger);
    }

    public void runEspressoTest(DeviceInfo deviceInfo, TestTask testTask, TestTaskRunCallback testTaskRunCallback, Logger logger) {
        checkTestTaskCancel(testTask);
        logger.info("Start running tests {}, timeout {}s", testTask.getTestSuite(), testTask.getTimeOutSecond());

        DeviceTestTask deviceTestTask = initDeviceTestTask(deviceInfo, testTask, logger);
        File deviceTestResultFolder = deviceTestTask.getDeviceTestResultFolder();
        testTask.addTestedDeviceResult(deviceTestTask);
        checkTestTaskCancel(testTask);

        InstrumentationResultParser instrumentationResultParser = null;
        Logger reportLogger = null;

        try {
            reportLogger = initReportLogger(deviceTestTask, testTask, logger);
            initDevice(deviceInfo, testTask, reportLogger);

            /** xml report: parse listener */
            reportLogger.info("Start xml report: parse listener");
            EspressoListener listener = new EspressoListener(deviceManager, adbOperateUtil, deviceInfo, deviceTestTask, testTask.getPkgName(), reportLogger);
            listener.setHostName(InetAddress.getLocalHost().getHostName());
            listener.setReportDir(deviceTestResultFolder);
            instrumentationResultParser = new InstrumentationResultParser(testTask.getTestSuite(), Collections.singletonList(listener)) {
                @Override
                public boolean isCancelled() {
                    return testTask.isCanceled();
                }
            };

            /** run the test */
            reportLogger.info("Start instrumenting the test");
            checkTestTaskCancel(testTask);
            listener.startRecording(testTask.getTimeOutSecond());
            String result = startInstrument(deviceInfo, testTask.getTestScope(), testTask.getTestSuite(), testTask.getTestPkgName(), testTask.getTestRunnerName(), reportLogger, instrumentationResultParser, testTask.getTimeOutSecond(), testTask.getInstrumentationArgs());
            if (Const.TaskResult.error_device_offline.equals(result)) {
                testTaskRunCallback.onDeviceOffline(testTask);
                return;
            }
            checkTestTaskCancel(testTask);


            /** set paths */
            String absoluteReportPath = listener.getAbsoluteReportPath();
            deviceTestTask.setTestXmlReportPath(deviceManager.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
            File gifFile = listener.getGifFile();
            if (gifFile.exists() && gifFile.length() > 0) {
                deviceTestTask.setTestGifPath(deviceManager.getTestBaseRelPathInUrl(gifFile));
            }

        } catch (Exception e) {
            if (reportLogger != null) {
                reportLogger.info(deviceInfo.getSerialNum() + ": " + e.getMessage(), e);
            } else {
                logger.info(deviceInfo.getSerialNum() + ": " + e.getMessage(), e);
            }
            String errorStr = e.getClass().getName() + ": " + e.getMessage();
            if (errorStr.length() > 255) {
                errorStr = errorStr.substring(0, 254);
            }
            deviceTestTask.setErrorInProcess(errorStr);
        } finally {
            if (instrumentationResultParser != null) {
                instrumentationResultParser.flush();
            }
            afterTest(deviceInfo, testTask, deviceTestTask, testTaskRunCallback, reportLogger);
        }
    }

    public String startInstrument(DeviceInfo deviceInfo, String scope, String suiteName, String testPkgName, String testRunnerName, Logger logger, IShellOutputReceiver receiver,
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
            adbOperateUtil.executeShellCommandOnDevice(deviceInfo, command, receiver, testTimeOutSec);
            return Const.TaskResult.success;
        } catch (Exception e) {
            if (logger != null) logger.error(e.getMessage(), e);
            return e.getMessage();
        }
    }

}
