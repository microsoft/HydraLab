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
        TestConfig testConfig = new TestConfig();
        DeviceConfig deviceConfig = new DeviceConfig();
        testConfig.deviceConfig = deviceConfig;

        deviceConfig.deviceIdentifier = "";
        testConfig.runningType = "";
        testConfig.appPath = "";
        testConfig.runTimeOutSeconds = 0;
        apiConfig.authToken = "";
        testConfig.pkgName = "";
        testConfig.testAppPath = "./testAppPath/testApp.apk";
        testConfig.testPkgName = "TestPkgName";
        testConfig.testScope = ClientUtilsPlugin.TestScope.CLASS;
        testConfig.testSuiteName = "com.example.test.suite";

        generalParamCheck(apiConfig, testConfig);

        apiConfig.host = "www.test.host";
        generalParamCheck(apiConfig, testConfig);

        apiConfig.authToken = "thisisanauthtokenonlyfortest";
        generalParamCheck(apiConfig, testConfig);

        testConfig.appPath = "./appPath/app.apk";
        generalParamCheck(apiConfig, testConfig);

        testConfig.pkgName = "PkgName";
        generalParamCheck(apiConfig, testConfig);

        testConfig.runningType = "INSTRUMENTATION";
        generalParamCheck(apiConfig, testConfig);

        testConfig.runTimeOutSeconds = 1000;
        generalParamCheck(apiConfig, testConfig);

        deviceConfig.deviceIdentifier = "TESTDEVICESN001";
        clientUtilsPlugin.requiredParamCheck(apiConfig, testConfig);
    }

    @Test
    public void checkInstrumentationTestRequiredParam() {
        HydraLabAPIConfig apiConfig = new HydraLabAPIConfig();
        TestConfig testConfig = new TestConfig();
        DeviceConfig deviceConfig = new DeviceConfig();
        testConfig.deviceConfig = deviceConfig;

        testConfig.runningType = "INSTRUMENTATION";
        testConfig.appPath = "./appPath/app.apk";
        testConfig.runTimeOutSeconds = 1000;
        testConfig.pkgName = "PkgName";
        testConfig.testAppPath = "";
        testConfig.testSuiteName = "";
        testConfig.testPkgName = "";
        testConfig.testScope = "";
        testConfig.unblockDevice = false;
        testConfig.blockDevice = false;
        testConfig.unblockDeviceSecretKey = "";
        apiConfig.host = "www.test.host";
        apiConfig.authToken = "thisisanauthtokenonlyfortest";
        deviceConfig.deviceIdentifier = "TESTDEVICESN001";

        typeSpecificParamCheck(apiConfig, testConfig, "testAppPath");
        testConfig.testAppPath = "./testAppPath/testApp.apk";

        typeSpecificParamCheck(apiConfig, testConfig, "testPkgName");
        testConfig.testPkgName = "TestPkgName";

        testConfig.testScope = ClientUtilsPlugin.TestScope.TEST_APP;
        clientUtilsPlugin.requiredParamCheck(apiConfig, testConfig);

        testConfig.testScope = ClientUtilsPlugin.TestScope.PACKAGE;
        typeSpecificParamCheck(apiConfig, testConfig, "testSuiteName");

        testConfig.testScope = ClientUtilsPlugin.TestScope.CLASS;
        typeSpecificParamCheck(apiConfig, testConfig, "testSuiteName");

        testConfig.testScope = ClientUtilsPlugin.TestScope.PACKAGE;
        testConfig.testSuiteName = "com.example.test.suite";
        clientUtilsPlugin.requiredParamCheck(apiConfig, testConfig);

        testConfig.testScope = ClientUtilsPlugin.TestScope.CLASS;
        clientUtilsPlugin.requiredParamCheck(apiConfig, testConfig);

        testConfig.unblockDevice = true;
        typeSpecificParamCheck(apiConfig, testConfig, "unblockDeviceSecretKey");

        testConfig.unblockDeviceSecretKey = "UNBLOCKDEVICESECRET001";
        testConfig.blockDevice = true;
        typeSpecificParamCheck(apiConfig, testConfig, "blockUnblockDevice");
        testConfig.blockDevice = false;

        deviceConfig.deviceIdentifier = "G.GROUP001";
        typeSpecificParamCheck(apiConfig, testConfig, "unblockDeviceGroup");

        deviceConfig.deviceIdentifier = "TESTDEVICESN001";

        clientUtilsPlugin.requiredParamCheck(apiConfig, testConfig);
    }

    @Test
    public void checkAppiumTestRequiredParam() {
        HydraLabAPIConfig apiConfig = new HydraLabAPIConfig();
        TestConfig testConfig = new TestConfig();
        DeviceConfig deviceConfig = new DeviceConfig();
        testConfig.deviceConfig = deviceConfig;

        testConfig.runningType = "APPIUM";
        testConfig.appPath = "./appPath/app.apk";
        testConfig.runTimeOutSeconds = 1000;
        testConfig.pkgName = "PkgName";
        testConfig.testAppPath = "";
        testConfig.testSuiteName = "";
        apiConfig.host = "www.test.host";
        apiConfig.authToken = "thisisanauthtokenonlyfortest";
        deviceConfig.deviceIdentifier = "TESTDEVICESN001";

        typeSpecificParamCheck(apiConfig, testConfig, "testAppPath");
        testConfig.testAppPath = "./testAppPath/testApp.apk";

        typeSpecificParamCheck(apiConfig, testConfig, "testSuiteName");
        testConfig.testSuiteName = "com.example.test.suite";

        clientUtilsPlugin.requiredParamCheck(apiConfig, testConfig);
    }

    @Test
    public void checkAppiumCrossTestRequiredParam() {
        HydraLabAPIConfig apiConfig = new HydraLabAPIConfig();
        TestConfig testConfig = new TestConfig();
        DeviceConfig deviceConfig = new DeviceConfig();
        testConfig.deviceConfig = deviceConfig;

        testConfig.runningType = "APPIUM";
        testConfig.appPath = "./appPath/app.apk";
        testConfig.runTimeOutSeconds = 1000;
        testConfig.pkgName = "PkgName";
        testConfig.testAppPath = "";
        testConfig.testSuiteName = "";
        apiConfig.host = "www.test.host";
        apiConfig.authToken = "thisisanauthtokenonlyfortest";
        deviceConfig.deviceIdentifier = "TESTDEVICESN001";

        typeSpecificParamCheck(apiConfig, testConfig, "testAppPath");
        testConfig.testAppPath = "./testAppPath/testApp.apk";

        typeSpecificParamCheck(apiConfig, testConfig, "testSuiteName");
        testConfig.testSuiteName = "com.example.test.suite";

        clientUtilsPlugin.requiredParamCheck(apiConfig, testConfig);
    }

    @Test
    public void runTestOnDeviceWithApp() {
        String reportFolderPath = "./reportFolder";
        HydraLabAPIClient client = Mockito.mock(HydraLabAPIClient.class);
        HydraLabAPIConfig apiConfig = Mockito.mock(HydraLabAPIConfig.class);
        TestConfig testConfig = Mockito.mock(TestConfig.class);
        testConfig.runningType = "INSTRUMENTATION";
        testConfig.appPath = "src/test/resources/app.txt";
        testConfig.testAppPath = "src/test/resources/test_app.txt";
        testConfig.attachmentInfos = new ArrayList<>();
        testConfig.blockDevice = true;
        testConfig.unblockDevice = false;

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

        when(client.generateAccessKey(Mockito.any(HydraLabAPIConfig.class), Mockito.any(TestConfig.class)))
                .thenReturn("accessKey");

        returnJson = new JsonObject();
        returnJson.addProperty("code", "200");
        returnJson.addProperty("message", "OK!");
        JsonObject subJsonObject = new JsonObject();
        subJsonObject.addProperty("devices", "device1,device2");
        subJsonObject.addProperty("testTaskId", "test_task_id");
        returnJson.add("content", subJsonObject);
        when(client.triggerTestRun(Mockito.any(TestConfig.class), Mockito.any(HydraLabAPIConfig.class), Mockito.anyString(), Mockito.anyString()))
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
        returnTestTask.blockedDeviceSerialNumber = "TESTDEVICESN001";
        returnTestTask.unblockDeviceSecretKey = "UNBLOCKDEVICESECRET001";
        when(client.getTestStatus(Mockito.any(HydraLabAPIConfig.class), Mockito.anyString()))
                .thenReturn(returnTestTask);

        String returnBlobSAS = "SAS";
        when(client.getBlobSAS(Mockito.any(HydraLabAPIConfig.class)))
                .thenReturn(returnBlobSAS);

        HydraLabClientUtils.switchClientInstance(client);
        HydraLabClientUtils.runTestOnDeviceWithApp(reportFolderPath, apiConfig, testConfig);

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

    private void generalParamCheck(HydraLabAPIConfig apiConfig, TestConfig testConfig) {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            clientUtilsPlugin.requiredParamCheck(apiConfig, testConfig);
        }, "IllegalArgumentException was expected");
        Assertions.assertEquals("Required params not provided! Make sure the following params are all provided correctly: hydraLabAPIHost, authToken, deviceIdentifier, appPath, pkgName, runningType, runTimeOutSeconds.", thrown.getMessage());
    }

    private void typeSpecificParamCheck(HydraLabAPIConfig apiConfig, TestConfig testConfig, String requiredParamName) {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            clientUtilsPlugin.requiredParamCheck(apiConfig, testConfig);
        }, "IllegalArgumentException was expected");
        if (requiredParamName.equals("blockUnblockDevice")) {
            Assertions.assertEquals("Running type " + testConfig.runningType + " param block and unblock device should not be true in the same test task!", thrown.getMessage());
        } else if(requiredParamName.equals("unblockDeviceGroup")) {
            Assertions.assertEquals("Running type " + testConfig.runningType + " param deviceIdentifier should not be a Group when unblockDevice is set to true!", thrown.getMessage());
        }
        else {
            Assertions.assertEquals("Running type " + testConfig.runningType + " required param " + requiredParamName + " not provided!", thrown.getMessage());
        }
    }
}