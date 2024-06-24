// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.appium;

import com.microsoft.hydralab.agent.runner.TestRunDeviceOrchestrator;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.appium.AppiumParam;
import com.microsoft.hydralab.appium.ThreadParam;
import com.microsoft.hydralab.common.entity.agent.EnvCapability;
import com.microsoft.hydralab.common.entity.agent.EnvCapabilityRequirement;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
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
import java.util.List;
import java.util.Map;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class AppiumRunner extends TestRunner {
    private static final int MAJOR_APPIUM_VERSION = 1;
    private static final int MINOR_APPIUM_VERSION = -1;

    public AppiumRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                        TestRunDeviceOrchestrator testRunDeviceOrchestrator,
                        PerformanceTestManagementService performanceTestManagementService) {
        super(agentManagementService, testTaskRunCallback, testRunDeviceOrchestrator, performanceTestManagementService);
    }

    @Override
    protected List<EnvCapabilityRequirement> getEnvCapabilityRequirements() {
        return List.of(new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.appium, MAJOR_APPIUM_VERSION, MINOR_APPIUM_VERSION));
    }

    @Override
    protected void run(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun) throws Exception {

        Logger reportLogger = testRun.getLogger();
        try {
            File gifFile =
                    runAndGetGif(testTask.getTestAppFile(), testTask.getTestSuite(), testRunDevice, testTask, testRun,
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
    public void teardown(TestTask testTask, TestRun testRun) {
        TestRunDevice testRunDevice = testRun.getDevice();
        quitAppiumDrivers(testRunDevice, testTask, testRun.getLogger());
        super.teardown(testTask, testRun);
    }

    protected void quitAppiumDrivers(TestRunDevice testRunDevice, TestTask testTask, Logger reportLogger) {
        testRunDeviceOrchestrator.quitAppiumDriver(testRunDevice, reportLogger);
    }

    protected File runAndGetGif(File appiumJarFile, String appiumCommand, TestRunDevice testRunDevice, TestTask testTask,
                                TestRun testRun, File deviceTestResultFolder, Logger reportLogger) {
        //set appium test property
        reportLogger.info("Start set appium test property");
        Map<String, String> taskRunArgs = testTask.getTaskRunArgs();
        if (taskRunArgs == null) {
            taskRunArgs = new HashMap<>();
        }
        AppiumParam appiumParam = new AppiumParam(
                testRunDevice.getDeviceInfo().getSerialNum(),
                testRunDevice.getDeviceInfo().getName(),
                testRunDevice.getDeviceInfo().getOsVersion(),
                IOSUtils.getWdaPortByUdid(testRunDevice.getDeviceInfo().getSerialNum(), reportLogger),
                testTask.getAppFile().getAbsolutePath(),
                deviceTestResultFolder.getAbsolutePath());
        ThreadParam.init(appiumParam, taskRunArgs);
        reportLogger.info("ThreadParam init success, AppiumParam is {} , args is {}", appiumParam,
                LogUtils.scrubSensitiveArgs(taskRunArgs.toString()));
        File gifFile = null;
        if (TestTask.TestFrameworkType.JUNIT5.equals(testTask.getFrameworkType())) {
            reportLogger.info("Start init listener");
            Junit5Listener junit5Listener =
                    new Junit5Listener(agentManagementService, testRunDevice, testRun, testTask,
                            testRunDeviceOrchestrator, performanceTestManagementService, reportLogger);

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
                    new AppiumListener(agentManagementService, testRunDevice, testRun, testTask,
                            testRunDeviceOrchestrator, performanceTestManagementService, reportLogger);

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
        } catch (Exception e) {
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
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
