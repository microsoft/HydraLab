// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab;

import com.google.gson.JsonObject;
import com.microsoft.hydralab.config.DeviceConfig;
import com.microsoft.hydralab.config.HydraLabAPIConfig;
import com.microsoft.hydralab.config.TestConfig;
import com.microsoft.hydralab.entity.*;
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

    @Test
    public void checkGeneralTestRequiredParam() {
        HydraLabAPIConfig apiConfig = new HydraLabAPIConfig();
        DeviceConfig deviceConfig = new DeviceConfig();
        TestConfig testConfig = new TestConfig();

        deviceConfig.deviceIdentifier = "";
        testConfig.runningType = "";
        testConfig.appPath = "";
        testConfig.runTimeOutSeconds = -1;
        apiConfig.authToken = "";
        testConfig.pkgName = "";
        testConfig.testAppPath = "./testAppPath/testApp.apk";
        testConfig.testSuiteName = "com.example.test.suite";
        testConfig.testPkgName = "TestPkgName";
        testConfig.testScope = ClientUtilsPlugin.TestScope.CLASS;

        generalParamCheck(apiConfig, deviceConfig, testConfig);

        testConfig.runningType = "INSTRUMENTATION";
        generalParamCheck(apiConfig, deviceConfig, testConfig);

        testConfig.appPath = "./appPath/app.apk";
        generalParamCheck(apiConfig, deviceConfig, testConfig);

        deviceConfig.deviceIdentifier = "TESTDEVICESN001";
        generalParamCheck(apiConfig, deviceConfig, testConfig);

        testConfig.runTimeOutSeconds = 1000;
        generalParamCheck(apiConfig, deviceConfig, testConfig);

        testConfig.pkgName = "PkgName";
        generalParamCheck(apiConfig, deviceConfig, testConfig);

        apiConfig.authToken = "thisisanauthtokenonlyfortest";
        clientUtilsPlugin.requiredParamCheck(apiConfig, deviceConfig, testConfig);
    }

    @Test
    public void checkInstrumentationTestRequiredParam() {
        HydraLabAPIConfig apiConfig = new HydraLabAPIConfig();
        DeviceConfig deviceConfig = new DeviceConfig();
        TestConfig testConfig = new TestConfig();

        testConfig.runningType = "INSTRUMENTATION";
        testConfig.appPath = "./appPath/app.apk";
        testConfig.runTimeOutSeconds = 1000;
        testConfig.pkgName = "PkgName";
        testConfig.testAppPath = "";
        testConfig.testSuiteName = "";
        testConfig.testPkgName = "";
        testConfig.testScope = "";
        apiConfig.authToken = "thisisanauthtokenonlyfortest";
        deviceConfig.deviceIdentifier = "TESTDEVICESN001";

        typeSpecificParamCheck(apiConfig, deviceConfig, testConfig, "testAppPath");
        testConfig.testAppPath = "./testAppPath/testApp.apk";

        typeSpecificParamCheck(apiConfig, deviceConfig, testConfig, "testPkgName");
        testConfig.testPkgName = "TestPkgName";

        testConfig.testScope = ClientUtilsPlugin.TestScope.TEST_APP;
        clientUtilsPlugin.requiredParamCheck(apiConfig, deviceConfig, testConfig);

        testConfig.testScope = ClientUtilsPlugin.TestScope.PACKAGE;
        typeSpecificParamCheck(apiConfig, deviceConfig, testConfig, "testSuiteName");

        testConfig.testScope = ClientUtilsPlugin.TestScope.CLASS;
        typeSpecificParamCheck(apiConfig, deviceConfig, testConfig, "testSuiteName");

        testConfig.testScope = ClientUtilsPlugin.TestScope.PACKAGE;
        testConfig.testSuiteName = "com.example.test.suite";
        clientUtilsPlugin.requiredParamCheck(apiConfig, deviceConfig, testConfig);

        testConfig.testScope = ClientUtilsPlugin.TestScope.CLASS;
        clientUtilsPlugin.requiredParamCheck(apiConfig, deviceConfig, testConfig);
    }

    @Test
    public void checkAppiumTestRequiredParam() {
        HydraLabAPIConfig apiConfig = new HydraLabAPIConfig();
        DeviceConfig deviceConfig = new DeviceConfig();
        TestConfig testConfig = new TestConfig();

        testConfig.runningType = "APPIUM";
        testConfig.appPath = "./appPath/app.apk";
        testConfig.runTimeOutSeconds = 1000;
        testConfig.pkgName = "PkgName";
        testConfig.testAppPath = "";
        testConfig.testSuiteName = "";
        apiConfig.authToken = "thisisanauthtokenonlyfortest";
        deviceConfig.deviceIdentifier = "TESTDEVICESN001";

        typeSpecificParamCheck(apiConfig, deviceConfig, testConfig, "testAppPath");
        testConfig.testAppPath = "./testAppPath/testApp.apk";

        typeSpecificParamCheck(apiConfig, deviceConfig, testConfig, "testSuiteName");
        testConfig.testSuiteName = "com.example.test.suite";

        clientUtilsPlugin.requiredParamCheck(apiConfig, deviceConfig, testConfig);
    }

    @Test
    public void checkAppiumCrossTestRequiredParam() {
        HydraLabAPIConfig apiConfig = new HydraLabAPIConfig();
        DeviceConfig deviceConfig = new DeviceConfig();
        TestConfig testConfig = new TestConfig();

        testConfig.runningType = "APPIUM";
        testConfig.appPath = "./appPath/app.apk";
        testConfig.runTimeOutSeconds = 1000;
        testConfig.pkgName = "PkgName";
        testConfig.testAppPath = "";
        testConfig.testSuiteName = "";
        apiConfig.authToken = "thisisanauthtokenonlyfortest";
        deviceConfig.deviceIdentifier = "TESTDEVICESN001";

        typeSpecificParamCheck(apiConfig, deviceConfig, testConfig, "testAppPath");
        testConfig.testAppPath = "./testAppPath/testApp.apk";

        typeSpecificParamCheck(apiConfig, deviceConfig, testConfig, "testSuiteName");
        testConfig.testSuiteName = "com.example.test.suite";

        clientUtilsPlugin.requiredParamCheck(apiConfig, deviceConfig, testConfig);
    }

    @Test
    public void runTestOnDeviceWithApp() {
        String reportFolderPath = "./reportFolder";
        Map<String, String> instrumentationArgs = new HashMap<>();
        Map<String, String> extraArgs = new HashMap<>();
        HydraLabAPIConfig apiConfig = Mockito.mock(HydraLabAPIConfig.class);
        DeviceConfig deviceConfig = Mockito.mock(DeviceConfig.class);
        TestConfig testConfig = Mockito.mock(TestConfig.class);
        HydraLabAPIClient client = Mockito.mock(HydraLabAPIClient.class);

        String returnId = "id123456";
        when(client.uploadApp(Mockito.any(HydraLabAPIConfig.class), Mockito.any(TestConfig.class), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.any(File.class), Mockito.any(File.class)))
                .thenReturn(returnId);

        JsonObject returnJson = new JsonObject();
        returnJson.addProperty("code", "200");
        returnJson.addProperty("message", "OK!");
        when(client.addAttachment(Mockito.any(HydraLabAPIConfig.class), Mockito.anyString(),
                Mockito.any(AttachmentInfo.class), Mockito.any(File.class)))
                .thenReturn(returnJson);

        when(client.generateAccessKey(Mockito.any(HydraLabAPIConfig.class), Mockito.any(DeviceConfig.class)))
                .thenReturn("accessKey");

        returnJson = new JsonObject();
        returnJson.addProperty("code", "200");
        returnJson.addProperty("message", "OK!");
        JsonObject subJsonObject = new JsonObject();
        subJsonObject.addProperty("devices", "device1,device2");
        subJsonObject.addProperty("testTaskId", "test_task_id");
        returnJson.add("content", subJsonObject);
        when(client.triggerTestRun(Mockito.any(TestConfig.class), Mockito.any(DeviceConfig.class), Mockito.any(HydraLabAPIConfig.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap()))
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
        HydraLabClientUtils.runTestOnDeviceWithApp(reportFolderPath, instrumentationArgs, extraArgs, apiConfig, deviceConfig, testConfig);

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

    private void generalParamCheck(HydraLabAPIConfig apiConfig, DeviceConfig deviceConfig, TestConfig testConfig) {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            clientUtilsPlugin.requiredParamCheck(apiConfig, deviceConfig, testConfig);
        }, "IllegalArgumentException was expected");
        Assertions.assertEquals("Required params not provided! Make sure the following params are all provided correctly: authToken, appPath, pkgName, runningType, deviceIdentifier, runTimeOutSeconds.", thrown.getMessage());
    }

    private void typeSpecificParamCheck(HydraLabAPIConfig apiConfig, DeviceConfig deviceConfig, TestConfig testConfig, String requiredParamName) {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            clientUtilsPlugin.requiredParamCheck(apiConfig, deviceConfig, testConfig);
        }, "IllegalArgumentException was expected");
        Assertions.assertEquals("Required param " + requiredParamName + " not provided!", thrown.getMessage());
    }
}