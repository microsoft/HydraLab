// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner.appium;

import com.microsoft.hydralab.agent.runner.RunningControlService;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestRunningCallback;
import com.microsoft.hydralab.appium.AppiumParam;
import com.microsoft.hydralab.appium.ThreadParam;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.impl.IOSDeviceManager;
import com.microsoft.hydralab.common.util.IOSUtils;
import com.microsoft.hydralab.common.util.LogUtils;
import org.junit.internal.TextListener;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.runner.JUnitCore;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@Service("appiumRunner")
public class AppiumRunner extends TestRunner {

    @Override
    public RunningControlService.DeviceTask getDeviceTask(TestTask testTask, TestRunningCallback testRunningCallback) {

        return (deviceInfo, logger) -> {
            runAppiumTest(testTask.getTestAppFile(), testTask.getTestSuite(), deviceInfo, testTask, testRunningCallback, logger);
            return true;
        };
    }

    public void runAppiumTest(File appiumJarFile, String appiumCommand, DeviceInfo deviceInfo,
                              TestTask testTask,
                              TestRunningCallback testRunningCallback, Logger logger) {
        checkTestTaskCancel(testTask);
        logger.info("Start running tests {}, timeout {}s", testTask.getTestSuite(), testTask.getTimeOutSecond());

        DeviceTestTask deviceTestTask = initDeviceTestTask(deviceInfo, testTask, logger);
        File deviceTestResultFolder = deviceTestTask.getDeviceTestResultFolder();
        testTask.addTestedDeviceResult(deviceTestTask);
        checkTestTaskCancel(testTask);

        Logger reportLogger = null;

        try {
            reportLogger = initReportLogger(deviceTestTask, testTask, logger);
            initDevice(deviceInfo, testTask, reportLogger);

            File gifFile = runAndGetGif(appiumJarFile, appiumCommand, deviceInfo, testTask, deviceTestTask, deviceTestResultFolder, reportLogger);
            if (gifFile != null && gifFile.exists() && gifFile.length() > 0) {
                deviceTestTask.setTestGifPath(deviceManager.getTestBaseRelPathInUrl(gifFile));
            }

        } catch (Exception e) {
            e.printStackTrace();
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
            //clear config
            ThreadParam.clean();
            afterTest(deviceInfo, testTask, deviceTestTask, testRunningCallback, reportLogger);
        }
    }

    @Override
    protected void afterTest(DeviceInfo deviceInfo, TestTask testTask, DeviceTestTask deviceTestTask, TestRunningCallback testRunningCallback, Logger reportLogger) {
        quitAppiumDrivers(deviceInfo, testTask, reportLogger);
        super.afterTest(deviceInfo, testTask, deviceTestTask, testRunningCallback, reportLogger);
    }

    protected void quitAppiumDrivers(DeviceInfo deviceInfo, TestTask testTask, Logger reportLogger) {
        deviceManager.quitMobileAppiumDriver(deviceInfo, reportLogger);
    }

    protected File runAndGetGif(File appiumJarFile, String appiumCommand, DeviceInfo deviceInfo, TestTask testTask, DeviceTestTask deviceTestTask, File deviceTestResultFolder, Logger reportLogger) {
        //set appium test property
        reportLogger.info("Start set appium test property");
        Map<String, String> instrumentationArgs = testTask.getInstrumentationArgs();
        if (instrumentationArgs == null) {
            instrumentationArgs = new HashMap<>();
        }
        AppiumParam appiumParam = new AppiumParam(deviceInfo.getSerialNum(), deviceInfo.getName(), deviceInfo.getOsVersion(), IOSUtils.getWdaPortByUdid(deviceInfo.getSerialNum(), reportLogger), testTask.getAppFile().getAbsolutePath(), deviceTestResultFolder.getAbsolutePath());
        ThreadParam.init(appiumParam, instrumentationArgs);
        reportLogger.info("ThreadParam init success, AppiumParam is {} , args is {}", appiumParam, LogUtils.scrubSensitiveArgs(instrumentationArgs.toString()));
        File gifFile = null;
        if (TestTask.TestFrameworkType.JUNIT5.equals(testTask.getFrameworkType())) {
            reportLogger.info("Start init listener");
            Junit5Listener junit5Listener = new Junit5Listener(deviceManager, deviceInfo, deviceTestTask, testTask.getPkgName(), reportLogger);

            /** run the test */
            reportLogger.info("Start appium test with junit5");
            junit5Listener.startRecording(testTask.getTimeOutSecond());
            checkTestTaskCancel(testTask);
            startJunit5(appiumJarFile, appiumCommand, junit5Listener, reportLogger);
            checkTestTaskCancel(testTask);
            gifFile = junit5Listener.getGifFile();
        } else {
            /** xml report: parse listener */
            reportLogger.info("Start init listener");
            AppiumListener listener = new AppiumListener(deviceManager, deviceInfo, deviceTestTask, testTask.getPkgName(), reportLogger);

            /** run the test */
            reportLogger.info("Start appium test with junit4");
            listener.startRecording(testTask.getTimeOutSecond());
            checkTestTaskCancel(testTask);
            startJunit(appiumJarFile, appiumCommand, listener, reportLogger);
            checkTestTaskCancel(testTask);
            gifFile = listener.getGifFile();
        }
        /** set paths */
        String absoluteReportPath = deviceTestResultFolder.getAbsolutePath();
        deviceTestTask.setTestXmlReportPath(deviceManager.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
        return gifFile;
    }

    public boolean startJunit(File appiumJarFile, String appiumCommand, AppiumListener listener, Logger logger) {
        JUnitCore junit = new JUnitCore();
        junit.addListener(new TextListener(System.out));
        junit.addListener(listener);
        URL url = null;
        try {
            url = new URL("file:" + appiumJarFile);
        } catch (MalformedURLException e) {
            logger.error("runAppiumTest error", e);
        }
        URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{url}, Thread.currentThread().getContextClassLoader());
        Class<?> myClass;
        try {
            myClass = urlClassLoader.loadClass(appiumCommand);
            junit.run(myClass);
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean startJunit5(File appiumJarFile, String appiumCommand, TestExecutionListener listener, Logger logger) {
        Launcher launcher = LauncherFactory.create();

        URL url = null;
        try {
            url = new URL("file:" + appiumJarFile);
        } catch (MalformedURLException e) {
            logger.error("runAppiumTest error", e);
        }
        URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{url}, Thread.currentThread().getContextClassLoader());
        Class<?> myClass;
        try {
            myClass = urlClassLoader.loadClass(appiumCommand);
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectClass(myClass))
                    .build();
            launcher.discover(request);
            launcher.registerTestExecutionListeners(listener);
            launcher.execute(request);
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void reInstallApp(DeviceInfo deviceInfo, TestTask testTask, Logger reportLogger) throws Exception {
        if (testTask.getRequireReinstall() || deviceManager instanceof IOSDeviceManager) {
            deviceManager.uninstallApp(deviceInfo, testTask.getPkgName(), reportLogger);
            deviceManager.safeSleep(1000);
        } else if (testTask.getRequireClearData()) {
            deviceManager.resetPackage(deviceInfo, testTask.getPkgName(), reportLogger);
        }
        checkTestTaskCancel(testTask);

        deviceManager.installApp(deviceInfo, testTask.getAppFile().getAbsolutePath(), reportLogger);
    }
}
