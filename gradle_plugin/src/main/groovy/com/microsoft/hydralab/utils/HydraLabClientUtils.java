// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.utils;

import com.google.gson.*;
import com.microsoft.hydralab.config.HydraLabAPIConfig;
import com.microsoft.hydralab.config.TestConfig;
import com.microsoft.hydralab.entity.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.microsoft.hydralab.utils.CommonUtils.*;

public class HydraLabClientUtils {
    private static HydraLabAPIClient hydraLabAPIClient = new HydraLabAPIClient();
    private static final int waitStartSec = 30;
    private static final int minWaitFinishSec = 15;
    private static boolean isTestResultFailed = false;

    public static void switchClientInstance(HydraLabAPIClient client) {
        hydraLabAPIClient = client;
    }

    public static void runTestOnDeviceWithApp(String reportFolderPath, HydraLabAPIConfig apiConfig, TestConfig testConfig) {
        String output = String.format("##[section]All args: reportFolderPath: %s\n%s\n%s",
                reportFolderPath, apiConfig.toString(), testConfig.toString());

        // re-init static fields to overlap the previous values from run of this task (daemon mode will reuse last context)
        isTestResultFailed = false;
        printlnf(maskCred(output));

        try {
            runTestInner(reportFolderPath, apiConfig, testConfig);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    private static void runTestInner(String reportFolderPath, HydraLabAPIConfig apiConfig, TestConfig testConfig) {
        // Collect git info
        File commandDir = new File(".");
        // TODO: make the commit info fetch approach compatible to other types of pipeline variables.
        String commitId = System.getenv("BUILD_SOURCEVERSION");
        String commitCount = "";
        String commitMsg = System.getenv("BUILD_SOURCEVERSIONMESSAGE");
        try {
            if (StringUtils.isEmpty(commitId)) {
                commitId = getLatestCommitHash(commandDir);
            }
            printlnf("Commit ID: %s", commitId);

            if (!StringUtils.isEmpty(commitId)) {
                if (StringUtils.isEmpty(commitCount)) {
                    commitCount = getCommitCount(commandDir, commitId);
                }
                if (StringUtils.isEmpty(commitMsg)) {
                    commitMsg = getCommitMessage(commandDir, commitId);
                }
            }
            if (StringUtils.isEmpty(commitCount)) {
                commitCount = "-1";
            }
            if (StringUtils.isEmpty(commitMsg)) {
                commitMsg = "NOT PARSED";
            }
            printlnf("Commit Count: %s", commitCount);
            printlnf("Commit Message: %s", commitMsg);
        } catch (Exception e) {
            throw new IllegalArgumentException("Get commit info failed: " + e.getMessage(), e);
        }

        File app = new File(testConfig.appPath);
        File testApp = null;
        if (StringUtils.isNotEmpty(testConfig.testAppPath)) {
            testApp = new File(testConfig.testAppPath);
        }
        try {
            if (StringUtils.isNotBlank(testConfig.attachmentConfigPath)) {
                File file = new File(testConfig.attachmentConfigPath);
                JsonParser parser = new JsonParser();
                JsonArray attachmentInfoJsons = parser.parse(new FileReader(file)).getAsJsonArray();
                printlnf("Attachment size: %d", attachmentInfoJsons.size());
                printlnf("Attachment information: %s", attachmentInfoJsons.toString());

                // new a list to override yml config if the file path exists
                testConfig.attachmentInfos = new ArrayList<>();
                for (JsonElement attachmentInfoJson : attachmentInfoJsons) {
                    AttachmentInfo attachmentInfo = GSON.fromJson(attachmentInfoJson, AttachmentInfo.class);
                    testConfig.attachmentInfos.add(attachmentInfo);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Attachment config not extracted correctly: " + e.getMessage(), e);
        }

        String testFileSetId = hydraLabAPIClient.uploadApp(apiConfig, testConfig, commitId, commitCount, commitMsg, app, testApp);
        printlnf("##[section]Uploaded test file set id: %s", testFileSetId);
        assertNotNull(testFileSetId, "testFileSetId");

        // TODO: make the pipeline link fetch approach compatible to other types of pipeline variables.
        testConfig.pipelineLink = System.getenv("SYSTEM_TEAMFOUNDATIONSERVERURI") + System.getenv("SYSTEM_TEAMPROJECT") + "/_build/results?buildId=" + System.getenv("BUILD_BUILDID");
        printlnf("##[section]Callback pipeline link is: %s", testConfig.pipelineLink);

        for (AttachmentInfo attachmentInfo : testConfig.attachmentInfos) {
            assertTrue(!attachmentInfo.filePath.isEmpty(), "Attachment file " + attachmentInfo.fileName + "has an empty path.", null);
            File attachment = new File(attachmentInfo.filePath);
            assertTrue(attachment.exists(), "Attachment file " + attachmentInfo.fileName + "doesn't exist.", null);

            JsonObject responseContent = hydraLabAPIClient.addAttachment(apiConfig, testFileSetId, attachmentInfo, attachment);
            int resultCode = responseContent.get("code").getAsInt();

            int waitingRetry = 10;
            while (resultCode != 200 && waitingRetry > 0) {
                printlnf("##[warning]Attachment %s uploading failed, remaining retry times: %d\nServer code: %d, message: %s", attachmentInfo.filePath, waitingRetry, resultCode, responseContent.get("message").getAsString());
                responseContent = hydraLabAPIClient.addAttachment(apiConfig, testFileSetId, attachmentInfo, attachment);
                resultCode = responseContent.get("code").getAsInt();
                waitingRetry--;
            }
            assertTrue(resultCode == 200, "Attachment " + attachmentInfo.filePath + " uploading failed, test exits with exception:\n", responseContent.get("message").getAsString());
            printlnf("##[command]Attachment %s uploaded successfully", attachmentInfo.filePath);
        }

        String accessKey = hydraLabAPIClient.generateAccessKey(apiConfig, testConfig);
        if (StringUtils.isEmpty(accessKey)) {
            printlnf("##[warning]Access key is empty.");
        } else {
            printlnf("##[command]Access key obtained.");
        }

        JsonObject responseContent = hydraLabAPIClient.triggerTestRun(testConfig, apiConfig, testFileSetId, accessKey);
        int resultCode = responseContent.get("code").getAsInt();

        // retry
        int waitingRetry = 20;
        while (resultCode != 200 && waitingRetry > 0) {
            printlnf("##[warning]Trigger test run failed, remaining retry times: %d\nServer code: %d, message: %s", waitingRetry, resultCode, responseContent.get("message").getAsString());
            responseContent = hydraLabAPIClient.triggerTestRun(testConfig, apiConfig, testFileSetId, accessKey);
            resultCode = responseContent.get("code").getAsInt();
            waitingRetry--;
        }
        assertTrue(resultCode == 200, "Server returned code: " + resultCode, responseContent.get("message").getAsString());

        String testTaskId = responseContent.getAsJsonObject("content").get("testTaskId").getAsString();
        printlnf("##[section]Triggered test task id: %s successful!", testTaskId);

        int sleepSecond = testConfig.runTimeOutSeconds / 3;
        int totalWaitSecond = 0;
        boolean finished = false;
        TestTask runningTest = null;
        int hydraRetryTime = 0;
        String lastStatus = "";
        String currentStatus = "";

        while (!finished) {
            if (TestTask.TestStatus.WAITING.equals(currentStatus)) {
                if (totalWaitSecond > testConfig.queueTimeOutSeconds) {
                    break;
                }
                printlnf("Get test status after queuing for %d seconds", totalWaitSecond);
            } else if (TestTask.TestStatus.RUNNING.equals(currentStatus)) {
                if (totalWaitSecond > testConfig.runTimeOutSeconds) {
                    break;
                }
                printlnf("Get test status after running for %d seconds", totalWaitSecond);
            }

            runningTest = hydraLabAPIClient.getTestStatus(apiConfig, testTaskId);
            printlnf("Current running test info: %s", runningTest.toString());
            assertNotNull(runningTest, "testTask");

            lastStatus = currentStatus;
            currentStatus = runningTest.status;

            if (hydraRetryTime != runningTest.retryTime) {
                hydraRetryTime = runningTest.retryTime;
                printlnf("##[command]Retrying to run task again, current waited second will be reset. current retryTime is: %d", hydraRetryTime);
                totalWaitSecond = 0;
                sleepSecond = testConfig.runTimeOutSeconds / 3;
            }

            if (TestTask.TestStatus.WAITING.equals(currentStatus)) {
                printlnf("##[command]" + runningTest.message + " Start waiting: 30 seconds");
                sleepIgnoreInterrupt(waitStartSec);
                totalWaitSecond += waitStartSec;
            } else {
                if (TestTask.TestStatus.WAITING.equals(lastStatus)) {
                    printlnf("##[command]Clear waiting time: %d", totalWaitSecond);
                    totalWaitSecond = 0;
                    sleepSecond = testConfig.runTimeOutSeconds / 3;
                }
                printlnf("##[command]Running test on %d device, status for now: %s", runningTest.testDevicesCount, currentStatus);
                assertTrue(!TestTask.TestStatus.CANCELED.equals(currentStatus), "The test task is canceled", runningTest);
                assertTrue(!TestTask.TestStatus.EXCEPTION.equals(currentStatus), "The test task is error", runningTest);
                finished = TestTask.TestStatus.FINISHED.equals(currentStatus);
                if (finished) {
                    break;
                }
                // using ##[command] as a highlight indicator
                printlnf("##[command]Start waiting: %d seconds", sleepSecond);
                sleepIgnoreInterrupt(sleepSecond);
                totalWaitSecond += sleepSecond;
                // binary wait with min boundary
                sleepSecond = Math.max(sleepSecond / 2, minWaitFinishSec);
            }
        }

        if (TestTask.TestStatus.WAITING.equals(currentStatus)) {
            hydraLabAPIClient.cancelTestTask(apiConfig, testTaskId, "Queue timeout!");
            printlnf("Cancelled the task as queuing timeout %d seconds is reached", testConfig.queueTimeOutSeconds);
            assertTrue(finished, "Queuing timeout after waiting for " + testConfig.queueTimeOutSeconds + " seconds! Test id", runningTest);
        } else if (TestTask.TestStatus.RUNNING.equals(currentStatus)) {
            hydraLabAPIClient.cancelTestTask(apiConfig, testTaskId, "Run timeout!");
            printlnf("Cancelled the task as running timeout %d seconds is reached", testConfig.runTimeOutSeconds);
            assertTrue(finished, "Running timeout after waiting for " + testConfig.runTimeOutSeconds + " seconds! Test id", runningTest);
        }

        assertNotNull(runningTest, "runningTest");
        assertNotNull(runningTest.deviceTestResults, "runningTest.deviceTestResults");
        if (testConfig.blockDevice) {
            assertNotNull(runningTest.blockedDeviceSerialNumber, "blockedDeviceSerialNumber");
            printlnf("##vso[task.setvariable variable=BlockedDeviceSerialNumber;isOutput=true]%s", runningTest.blockedDeviceSerialNumber);
            printlnf("##vso[task.setvariable variable=UnblockDeviceSecretKey;isOutput=true]%s", runningTest.unblockDeviceSecretKey);
        }

        if (testConfig.unblockDevice && testConfig.deviceConfig.deviceIdentifier.equals(runningTest.unblockedDeviceSerialNumber)) {
            printlnf("##[section] Device % s, unblocked.", runningTest.unblockedDeviceSerialNumber);
        }

        String testReportUrl = apiConfig.getTestReportUrl(runningTest.id);

        StringBuilder mdBuilder = new StringBuilder("# Hydra Lab Test Result Details\n\n\n");
        mdBuilder.append(String.format("### [Link to full report](%s)\n\n\n", testReportUrl));
        mdBuilder.append(String.format("### Statistic: total test case count: %s, failed: %s\n\n", runningTest.totalTestCount, runningTest.totalFailCount));
        if (runningTest.totalFailCount > 0 && runningTest.reportImagePath != null) {
            printlnf("##[warning] %d cases failed during the test", runningTest.totalFailCount);
        }

        if (runningTest.totalFailCount > 0) {
            printlnf("##[error]Fatal error during test, total fail count: %d", runningTest.totalFailCount);
            markTestResultFail();
        }

        int index = 0;

        printlnf("##vso[task.setprogress value=90;]Almost Done with testing");
        printlnf("##[section]Start going through device test results, Test overall info: %s", runningTest);

        // add (test type + timestamp) in folder name to distinguish different test results when using the same device
        ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
        String testFolder;
        if (StringUtils.isEmpty(testConfig.artifactTag)) {
            testFolder = testConfig.runningType + "-" + utc.format(DateTimeFormatter.ofPattern("MMddHHmmss"));
        } else {
            testFolder = testConfig.runningType + "-" + testConfig.artifactTag + "-" + utc.format(DateTimeFormatter.ofPattern("MMddHHmmss"));
        }

        File file = new File(reportFolderPath, testFolder);
        if (!file.exists()) {
            file.mkdirs();
        }
        String testFolderPath = file.getAbsolutePath();

        for (DeviceTestResult deviceTestResult : runningTest.deviceTestResults) {
            printlnf(">>>>>>\n Device %s, failed cases count: %d, total cases: %d", deviceTestResult.deviceSerialNumber, deviceTestResult.failCount, deviceTestResult.totalCount);

            if (deviceTestResult.failCount > 0 || deviceTestResult.totalCount == 0) {
                if (deviceTestResult.crashStack != null && deviceTestResult.crashStack.length() > 0) {
                    printlnf("##[error]Fatal error during test on device %s, stack:\n%s", deviceTestResult.deviceSerialNumber, deviceTestResult.crashStack);
                } else {
                    printlnf("##[error]Fatal error during test on device %s with no stack found.", deviceTestResult.deviceSerialNumber);
                }
                markTestResultFail();
            }

            String deviceFileFolder = deviceTestResult.deviceSerialNumber;
            file = new File(testFolderPath, deviceFileFolder);
            if (!file.exists()) {
                file.mkdirs();
            }
            String deviceFileFolderPath = file.getAbsolutePath();

            if (deviceTestResult.attachments.size() != 0) {
                String signature = hydraLabAPIClient.getBlobSAS(apiConfig);
                for (StorageFileInfo fileInfo : deviceTestResult.attachments) {
                    String attachmentUrl = fileInfo.blobUrl + "?" + signature;
                    String attachmentFileName = fileInfo.fileName;

                    printlnf("Start downloading attachment for device %s, device name: %s, file name: %s, link: %s", deviceTestResult.deviceSerialNumber, deviceTestResult.deviceName, attachmentFileName, attachmentUrl);

                    file = new File(deviceFileFolderPath, attachmentFileName);
                    hydraLabAPIClient.downloadToFile(attachmentUrl, file);

                    printlnf("Finish downloading attachment %s for device %s", attachmentFileName, deviceTestResult.deviceSerialNumber);
                }
            }

            String deviceTestVideoUrl = apiConfig.getDeviceTestVideoUrl(deviceTestResult.id);
            printlnf("##[command]Device %s test video link: %s\n>>>>>>>>", deviceTestResult.deviceSerialNumber, deviceTestVideoUrl);
            // set this as a variable as we might need this in next task
            printlnf("##vso[task.setvariable variable=TestVideoLink%d;]%s", ++index, deviceTestVideoUrl);

            mdBuilder.append(String.format(Locale.US, "- On device %s (SN: %s), total case count: %d, failed: %d **[Video Link](%s)**\n", deviceTestResult.deviceName, deviceTestResult.deviceSerialNumber, deviceTestResult.totalCount, deviceTestResult.failCount, deviceTestVideoUrl));
        }

        File summaryMd = new File(testFolderPath, "TestLabSummary.md");

        try (FileOutputStream fos = new FileOutputStream(summaryMd)) {
            IOUtils.write(mdBuilder.toString(), fos, StandardCharsets.UTF_8);
            printlnf("##vso[task.uploadsummary]%s", summaryMd.getAbsolutePath());
        } catch (IOException e) {
            // no need to rethrow
            e.printStackTrace();
        }

        printlnf("Attachment folder path: %s", reportFolderPath);
        printlnf("##vso[artifact.upload artifactname=testResult;]%s", reportFolderPath);

        printlnf("##[section]All done, overall failed cases count: %d, total count: %d, devices count: %d", runningTest.totalFailCount, runningTest.totalTestCount, runningTest.testDevicesCount);
        printlnf("##[section]Test task report link:");
        printlnf(testReportUrl);
        printlnf("##vso[task.setvariable variable=TestTaskReportLink;]%s", testReportUrl);

        displayFinalTestState(testConfig.enableFailingTask);
    }

    private static void markTestResultFail() {
        isTestResultFailed = true;
    }

    private static void displayFinalTestState(boolean enableFailingTask) {
        if (isTestResultFailed) {
            printlnf("##[error]Final test state: fail.");
            if (enableFailingTask) {
                throw new RuntimeException("Test result failed, please check the output test result XML file.");
            }
        } else {
            printlnf("Final test state: success.");
        }
    }

    private static void sleepIgnoreInterrupt(int second) {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(second));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static String getCommitCount(File commandDir, String startCommit) throws IOException {
        Process process = Runtime.getRuntime().exec(String.format("git rev-list --first-parent --right-only --count %s..HEAD", startCommit), null, commandDir.getAbsoluteFile());
        try (InputStream inputStream = process.getInputStream()) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8).trim();
        } finally {
            process.destroy();
        }
    }

    public static String getLatestCommitHash(File commandDir) throws IOException {
        Process process = Runtime.getRuntime().exec(new String[]{"git", "log", "-1", "--pretty=format:%h"}, null, commandDir.getAbsoluteFile());
        try (InputStream inputStream = process.getInputStream()) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8).trim();
        } finally {
            process.destroy();
        }
    }

    public static String getCommitMessage(File workingDirFile, String commitId) throws IOException {
        Process process = Runtime.getRuntime().exec(new String[]{"git", "log", "--pretty=format:%s", commitId, "-1"}, null, workingDirFile.getAbsoluteFile());
        try (InputStream inputStream = process.getInputStream()) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8).trim();
        } finally {
            process.destroy();
        }
    }

    public enum MaskSensitiveData {
        CURRENT_PASSWORD("(current[_\\s-]*password)[=:\"\\s]*(\\w*)"),
        PASSWORD("[&,;\"\'\\s]+(password|pwd)[=:\"\\s]*(\\w*)"),
        GENERAL_PASSWORD("\\w*(password|pwd)[=:\\\"\\s]*(\\w*)"),
        PASSWORD_CONFIRMATION("(password[_\\s-]*confirmation)[=:\"\\s]*(\\w*)"),
        EMAIL("[&,;\"\'\\s]+(mail)[=:\"\\s]*(\\w*)"),
        GENERAL_EMAIL("\\w*(mail)[=:\\\"\\s]*(\\w*)"),
        API_KEY("(api[_\\s-]*key)[=:\"\\s]*(\\w*)"),
        RESET_PASSWORD_TOKEN("(reset[_\\s-]*password[_\\s-]*token)[=:\"\\s]*(\\w*)"),
        UPLOAD_TOKEN("(upload[_\\s-]*token)[=:\"\\s]*(\\w*)"),
        AUTH_TOKEN("(auth[_\\s-]*token)[=:\"\\s]*(\\w*)"),
        ACCESS_KEY("(access[_\\s-]*key)[=:\"\\s]*(\\w*)");

        private String regEx;

        MaskSensitiveData(String exp) {
            regEx = exp;
        }

        public String getRegEx() {
            return regEx;
        }
    }
}