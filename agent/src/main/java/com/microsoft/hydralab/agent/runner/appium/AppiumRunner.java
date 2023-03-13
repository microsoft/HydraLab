// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.appium;

import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.appium.AppiumParam;
import com.microsoft.hydralab.appium.ThreadParam;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.device.TestDevice;
import com.microsoft.hydralab.common.util.IOSUtils;
import com.microsoft.hydralab.common.util.LogUtils;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.junit.internal.TextListener;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.runner.JUnitCore;
import org.slf4j.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class AppiumRunner extends TestRunner {

    public AppiumRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                        PerformanceTestManagementService performanceTestManagementService) {
        super(agentManagementService, testTaskRunCallback, performanceTestManagementService);
    }

    @Override
    protected void run(TestDevice testDevice, TestTask testTask, TestRun testRun) throws Exception {

        Logger reportLogger = testRun.getLogger();
        try {
            File gifFile =
                    runAndGetGif(testTask.getTestAppFile(), testTask.getTestSuite(), testDevice, testTask, testRun,
                            testRun.getResultFolder(), reportLogger);
            if (gifFile != null && gifFile.exists() && gifFile.length() > 0) {
                testRun.setTestGifPath(agentManagementService.getTestBaseRelPathInUrl(gifFile));
            }
        } finally {
            //clear config
            ThreadParam.clean();
        }
    }

    @Override
    protected void tearDown(TestDevice testDevice, TestTask testTask, TestRun testRun) {
        quitAppiumDrivers(testDevice, testTask, testRun.getLogger());
        super.tearDown(testDevice, testTask, testRun);
    }

    protected void quitAppiumDrivers(TestDevice testDevice, TestTask testTask, Logger reportLogger) {
        testDevice.quitMobileAppiumDriver(reportLogger);
    }

    protected File runAndGetGif(File appiumJarFile, String appiumCommand, TestDevice testDevice, TestTask testTask,
                                TestRun testRun, File deviceTestResultFolder, Logger reportLogger) {
        //set appium test property
        reportLogger.info("Start set appium test property");
        Map<String, String> instrumentationArgs = testTask.getInstrumentationArgs();
        if (instrumentationArgs == null) {
            instrumentationArgs = new HashMap<>();
        }
        AppiumParam appiumParam =
                new AppiumParam(testDevice.getSerialNum(), testDevice.getName(), testDevice.getOsVersion(),
                        IOSUtils.getWdaPortByUdid(testDevice.getSerialNum(), reportLogger),
                        testTask.getAppFile().getAbsolutePath(), deviceTestResultFolder.getAbsolutePath());
        ThreadParam.init(appiumParam, instrumentationArgs);
        reportLogger.info("ThreadParam init success, AppiumParam is {} , args is {}", appiumParam,
                LogUtils.scrubSensitiveArgs(instrumentationArgs.toString()));
        File gifFile = null;
        if (TestTask.TestFrameworkType.JUNIT5.equals(testTask.getFrameworkType())) {
            reportLogger.info("Start init listener");
            Junit5Listener junit5Listener =
                    new Junit5Listener(agentManagementService, testDevice, testRun, testTask.getPkgName(),
                            performanceTestManagementService, reportLogger);

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
            AppiumListener listener =
                    new AppiumListener(agentManagementService, testDevice, testRun, testTask.getPkgName(),
                            performanceTestManagementService, reportLogger);

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
        testRun.setTestXmlReportPath(agentManagementService.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
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
        URLClassLoader urlClassLoader =
                new URLClassLoader(new URL[]{url}, Thread.currentThread().getContextClassLoader());
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

    public boolean startJunit5(File appiumJarFile, String appiumCommand, TestExecutionListener listener,
                               Logger logger) {
        Launcher launcher = LauncherFactory.create();

        URL url = null;
        try {
            url = new URL("file:" + appiumJarFile);
        } catch (MalformedURLException e) {
            logger.error("runAppiumTest error", e);
        }
        URLClassLoader urlClassLoader =
                new URLClassLoader(new URL[]{url}, Thread.currentThread().getContextClassLoader());
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

}
