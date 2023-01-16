// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab;

import com.google.gson.JsonObject;
import com.microsoft.hydralab.entity.AttachmentInfo;
import com.microsoft.hydralab.entity.HydraLabAPIConfig;
import com.microsoft.hydralab.entity.TestTask;
import com.microsoft.hydralab.utils.HydraLabAPIClient;
import com.microsoft.hydralab.utils.HydraLabClientUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;


public class ClientUtilsPluginTest {
    ClientUtilsPlugin clientUtilsPlugin = new ClientUtilsPlugin();

    String appPath = "src/test/resources/app.txt";

    String testAppPath = "src/test/resources/test_app.txt";

    @Test
    public void checkGeneralTestRequiredParam() {
        String runningType = "";
        String appPath = "";
        String deviceIdentifier = "";
        String runTimeOutSeconds = "";
        HydraLabAPIConfig apiConfig = new HydraLabAPIConfig();
        apiConfig.pkgName = "";
        apiConfig.authToken = "";
        String testAppPath = "./testAppPath/testApp.apk";
        String testSuiteName = "com.example.test.suite";
        apiConfig.testPkgName = "TestPkgName";
        apiConfig.testScope = ClientUtilsPlugin.TestScope.CLASS;

        generalParamCheck(appPath, deviceIdentifier, runTimeOutSeconds, apiConfig, testAppPath, testSuiteName, runningType);

        runningType = "INSTRUMENTATION";
        generalParamCheck(appPath, deviceIdentifier, runTimeOutSeconds, apiConfig, testAppPath, testSuiteName, runningType);

        appPath = "./appPath/app.apk";
        generalParamCheck(appPath, deviceIdentifier, runTimeOutSeconds, apiConfig, testAppPath, testSuiteName, runningType);

        deviceIdentifier = "TESTDEVICESN001";
        generalParamCheck(appPath, deviceIdentifier, runTimeOutSeconds, apiConfig, testAppPath, testSuiteName, runningType);

        runTimeOutSeconds = "1000";
        generalParamCheck(appPath, deviceIdentifier, runTimeOutSeconds, apiConfig, testAppPath, testSuiteName, runningType);

        apiConfig.pkgName = "PkgName";
        generalParamCheck(appPath, deviceIdentifier, runTimeOutSeconds, apiConfig, testAppPath, testSuiteName, runningType);

        apiConfig.authToken = "thisisanauthtokenonlyfortest";
        clientUtilsPlugin.requiredParamCheck(runningType, appPath, testAppPath, deviceIdentifier, runTimeOutSeconds, testSuiteName, apiConfig);
    }

    @Test
    public void checkInstrumentationTestRequiredParam() {
        String runningType = "INSTRUMENTATION";
        String appPath = "./appPath/app.apk";
        String deviceIdentifier = "TESTDEVICESN001";
        String runTimeOutSeconds = "1000";
        HydraLabAPIConfig apiConfig = new HydraLabAPIConfig();
        apiConfig.pkgName = "PkgName";
        apiConfig.authToken = "thisisanauthtokenonlyfortest";
        String testAppPath = "";
        String testSuiteName = "";
        apiConfig.testPkgName = "";
        apiConfig.testScope = "";

        typeSpecificParamCheck(appPath, deviceIdentifier, runTimeOutSeconds, apiConfig, testAppPath, testSuiteName, runningType, "testAppPath");
        testAppPath = "./testAppPath/testApp.apk";

        typeSpecificParamCheck(appPath, deviceIdentifier, runTimeOutSeconds, apiConfig, testAppPath, testSuiteName, runningType, "testPkgName");
        apiConfig.testPkgName = "TestPkgName";

        apiConfig.testScope = ClientUtilsPlugin.TestScope.TEST_APP;
        clientUtilsPlugin.requiredParamCheck(runningType, appPath, testAppPath, deviceIdentifier, runTimeOutSeconds, testSuiteName, apiConfig);

        apiConfig.testScope = ClientUtilsPlugin.TestScope.PACKAGE;
        typeSpecificParamCheck(appPath, deviceIdentifier, runTimeOutSeconds, apiConfig, testAppPath, testSuiteName, runningType, "testSuiteName");

        apiConfig.testScope = ClientUtilsPlugin.TestScope.CLASS;
        typeSpecificParamCheck(appPath, deviceIdentifier, runTimeOutSeconds, apiConfig, testAppPath, testSuiteName, runningType, "testSuiteName");

        apiConfig.testScope = ClientUtilsPlugin.TestScope.PACKAGE;
        testSuiteName = "com.example.test.suite";
        clientUtilsPlugin.requiredParamCheck(runningType, appPath, testAppPath, deviceIdentifier, runTimeOutSeconds, testSuiteName, apiConfig);

        apiConfig.testScope = ClientUtilsPlugin.TestScope.CLASS;
        clientUtilsPlugin.requiredParamCheck(runningType, appPath, testAppPath, deviceIdentifier, runTimeOutSeconds, testSuiteName, apiConfig);
    }

    @Test
    public void checkAppiumTestRequiredParam() {
        String runningType = "APPIUM";
        String appPath = "./appPath/app.apk";
        String deviceIdentifier = "TESTDEVICESN001";
        String runTimeOutSeconds = "1000";
        HydraLabAPIConfig apiConfig = new HydraLabAPIConfig();
        apiConfig.pkgName = "PkgName";
        apiConfig.authToken = "thisisanauthtokenonlyfortest";
        String testAppPath = "";
        String testSuiteName = "";

        typeSpecificParamCheck(appPath, deviceIdentifier, runTimeOutSeconds, apiConfig, testAppPath, testSuiteName, runningType, "testAppPath");
        testAppPath = "./testAppPath/testApp.apk";

        typeSpecificParamCheck(appPath, deviceIdentifier, runTimeOutSeconds, apiConfig, testAppPath, testSuiteName, runningType, "testSuiteName");
        testSuiteName = "com.example.test.suite";

        clientUtilsPlugin.requiredParamCheck(runningType, appPath, testAppPath, deviceIdentifier, runTimeOutSeconds, testSuiteName, apiConfig);
    }

    @Test
    public void checkAppiumCrossTestRequiredParam() {
        String runningType = "APPIUM_CROSS";
        String appPath = "./appPath/app.apk";
        String deviceIdentifier = "TESTDEVICESN001";
        String runTimeOutSeconds = "1000";
        HydraLabAPIConfig apiConfig = new HydraLabAPIConfig();
        apiConfig.pkgName = "PkgName";
        apiConfig.authToken = "thisisanauthtokenonlyfortest";
        String testAppPath = "";
        String testSuiteName = "";

        typeSpecificParamCheck(appPath, deviceIdentifier, runTimeOutSeconds, apiConfig, testAppPath, testSuiteName, runningType, "testAppPath");
        testAppPath = "./testAppPath/testApp.apk";

        typeSpecificParamCheck(appPath, deviceIdentifier, runTimeOutSeconds, apiConfig, testAppPath, testSuiteName, runningType, "testSuiteName");
        testSuiteName = "com.example.test.suite";

        clientUtilsPlugin.requiredParamCheck(runningType, appPath, testAppPath, deviceIdentifier, runTimeOutSeconds, testSuiteName, apiConfig);
    }

    @Test
    public void runTestOnDeviceWithApp() {
        String runningType = "INSTRUMENTATION";
        String attachmentConfigPath = "";
        String testSuiteName = "com.example.test.suite";
        String deviceIdentifier = "TESTDEVICESN001";
        int queueTimeoutSec = 1000;
        int runTimeoutSec = 1000;
        String reportFolderPath = "./reportFolder";
        Map<String, String> instrumentationArgs = new HashMap<>();
        Map<String, String> extraArgs = new HashMap<>();
        String tag = "";
        HydraLabAPIConfig apiConfig = Mockito.mock(HydraLabAPIConfig.class);
        HydraLabAPIClient client = Mockito.mock(HydraLabAPIClient.class);

        String returnId = "id123456";
        when(client.uploadApp(Mockito.any(HydraLabAPIConfig.class), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.any(File.class), Mockito.any(File.class)))
                .thenReturn(returnId);

        JsonObject returnJson = new JsonObject();
        returnJson.addProperty("code", "200");
        returnJson.addProperty("message", "OK!");
        when(client.addAttachment(Mockito.any(HydraLabAPIConfig.class), Mockito.anyString(),
                Mockito.any(AttachmentInfo.class), Mockito.any(File.class)))
                .thenReturn(returnJson);

        when(client.generateAccessKey(Mockito.any(HydraLabAPIConfig.class), Mockito.anyString()))
                .thenReturn("accessKey");

        returnJson = new JsonObject();
        returnJson.addProperty("code", "200");
        returnJson.addProperty("message", "OK!");
        JsonObject subJsonObject = new JsonObject();
        subJsonObject.addProperty("devices", "device1,device2");
        subJsonObject.addProperty("testTaskId", "test_task_id");
        returnJson.add("content", subJsonObject);
        when(client.triggerTestRun(Mockito.anyString(), Mockito.any(HydraLabAPIConfig.class), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyMap(), Mockito.anyMap()))
                .thenReturn(returnJson);

        TestTask returnTestTask = new TestTask();
        returnTestTask.status = TestTask.TestStatus.FINISHED;
        returnTestTask.retryTime = 1;
        returnTestTask.deviceTestResults = new ArrayList<>();
        returnTestTask.message = "message";
        returnTestTask.id = "id";
        returnTestTask.testDevicesCount = 1;
        returnTestTask.totalTestCount = 5;
        returnTestTask.totalFailCount = 1;
        returnTestTask.reportImagePath = "./image_path/image";
        when(client.getTestStatus(Mockito.any(HydraLabAPIConfig.class), Mockito.anyString()))
                .thenReturn(returnTestTask);

        String returnBlobSAS = "SAS";
        when(client.getBlobSAS(Mockito.any(HydraLabAPIConfig.class)))
                .thenReturn(returnBlobSAS);

        HydraLabClientUtils.switchClientInstance(client);
        HydraLabClientUtils.runTestOnDeviceWithApp(runningType, appPath, testAppPath, attachmentConfigPath,
                testSuiteName, deviceIdentifier, queueTimeoutSec, runTimeoutSec, reportFolderPath, instrumentationArgs,
                extraArgs, tag, apiConfig);

        verify(client, times(0)).cancelTestTask(Mockito.any(HydraLabAPIConfig.class), Mockito.anyString(), Mockito.anyString());
        verify(client, times(0)).downloadToFile(Mockito.anyString(), Mockito.any(File.class));
    }

    @Test
    public void getLatestCommitInfo() {
        String commitId = null;
        String commitCount = null;
        String commitMsg = null;
        File commandDir = new File(".");
        try {
            commitId = HydraLabClientUtils.getLatestCommitHash(commandDir);
            commitCount = HydraLabClientUtils.getCommitCount(commandDir, commitId);
            commitMsg = HydraLabClientUtils.getCommitMessage(commandDir, commitId);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assertions.assertNotNull(commitId, "Get commit id error");
        Assertions.assertNotNull(commitCount, "Get commit count error");
        Assertions.assertNotNull(commitMsg, "Get commit message error");
    }

    private void generalParamCheck(String appPath, String deviceIdentifier, String runTimeOutSeconds, HydraLabAPIConfig apiConfig, String testAppPath, String testSuiteName, String runningType) {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            clientUtilsPlugin.requiredParamCheck(runningType, appPath, testAppPath, deviceIdentifier, runTimeOutSeconds, testSuiteName, apiConfig);
        }, "IllegalArgumentException was expected");
        Assertions.assertEquals("Required params not provided! Make sure the following params are all provided correctly: authToken, appPath, pkgName, runningType, deviceIdentifier, runTimeOutSeconds.", thrown.getMessage());
    }

    private void typeSpecificParamCheck(String appPath, String deviceIdentifier, String runTimeOutSeconds, HydraLabAPIConfig apiConfig, String testAppPath, String testSuiteName, String runningType, String requiredParamName) {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            clientUtilsPlugin.requiredParamCheck(runningType, appPath, testAppPath, deviceIdentifier, runTimeOutSeconds, testSuiteName, apiConfig);
        }, "IllegalArgumentException was expected");
        Assertions.assertEquals("Required param " + requiredParamName + " not provided!", thrown.getMessage());
    }
}