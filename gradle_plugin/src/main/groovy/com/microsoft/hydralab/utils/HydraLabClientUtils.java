package com.microsoft.hydralab.utils;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HydraLabClientUtils {
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(300, TimeUnit.SECONDS)
            .connectTimeout(300, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    private static final int waitStartSec = 30;
    private static final int minWaitFinishSec = 15;

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Date.class, new TypeAdapter<Date>() {
                @Override
                public void write(JsonWriter out, Date value) throws IOException {
                    if (value == null) {
                        out.nullValue();
                    } else {
                        out.value(value.getTime());
                    }
                }

                @Override
                public Date read(JsonReader in) throws IOException {
                    if (in != null) {
                        try {
                            return new Date(in.nextLong());
                        } catch (IllegalStateException e) {
                            in.nextNull();
                            return null;
                        }
                    } else {
                        return null;
                    }
                }
            }).create();
    private static boolean sMarkedFail = false;

    public static void runTestOnDeviceWithApp(String runningType, String appPath, String testAppPath,
                                              String attachmentConfigPath,
                                              String testSuiteName,
                                              @Nullable String deviceIdentifier,
                                              int queueTimeoutSec,
                                              int runTimeoutSec,
                                              String reportFolderPath,
                                              Map<String, String> instrumentationArgs,
                                              Map<String, String> extraArgs,
                                              HydraLabAPIConfig apiConfig) {
        String output = String.format("##[section]All args: runningType: %s, appPath: %s, deviceIdentifier: %s" +
                        "\n##[section]\tqueueTimeOutSeconds: %d, runTimeOutSeconds: %d, attachmentConfigPath: %s, argsMap: %s, extraArgsMap: %s" +
                        "\n##[section]\tapiConfig: %s",
                runningType, appPath, deviceIdentifier, queueTimeoutSec, runTimeoutSec, attachmentConfigPath,
                instrumentationArgs == null ? "" : instrumentationArgs.toString(), extraArgs == null ? "" : extraArgs.toString(),
                apiConfig.toString());
        switch (runningType) {
            case "INSTRUMENTATION":
            case "APPIUM":
            case "APPIUM_CROSS":
                output = output + String.format("\n##[section]\ttestApkPath: %s, testSuiteName: %s", testAppPath, testSuiteName);
                break;
            case "T2C_JSON":
                output = output + String.format("\n##[section]\ttestApkPath: %s", testAppPath);
                break;
            case "SMART":
            case "MONKEY":
            case "APPIUM_MONKEY":
            default:
                break;
        }
        printlnf(maskCred(output));

        sMarkedFail = false;
        try {
            runTestInner(runningType, appPath, testAppPath, attachmentConfigPath, testSuiteName, deviceIdentifier,
                    queueTimeoutSec, runTimeoutSec, reportFolderPath, instrumentationArgs, extraArgs, apiConfig);
            markBuildSuccess();
        } catch (RuntimeException e) {
            markBuildFail();
            throw e;
        }
    }

    private static void runTestInner(String runningType, String appPath, String testAppPath,
                                     String attachmentConfigPath,
                                     String testSuiteName,
                                     @Nullable String deviceIdentifier,
                                     int queueTimeoutSec,
                                     int runTimeoutSec,
                                     String reportFolderPath,
                                     Map<String, String> instrumentationArgs,
                                     Map<String, String> extraArgs,
                                     HydraLabAPIConfig apiConfig) {
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

        File app = null;
        File testApp = null;
        JsonArray attachmentInfos = new JsonArray();
        try {
            File file = new File(appPath);
            assertTrue(file.exists(), "app not exist", null);

            if (file.isDirectory()) {
                throw new IllegalArgumentException("appPath should be the path to the app file.");
            } else {
                app = file;
            }

            if (!testAppPath.isEmpty()) {
                file = new File(testAppPath);
                assertTrue(file.exists(), "testApp not exist", null);
                if (file.isDirectory()) {
                    throw new IllegalArgumentException("testAppPath should be the path to the test app/jar or JSON-described test file.");
                } else {
                    testApp = file;
                }
            }

            if (!attachmentConfigPath.isEmpty()) {
                file = new File(attachmentConfigPath);
                JsonParser parser = new JsonParser();
                attachmentInfos =  parser.parse(new FileReader(file)).getAsJsonArray();
                printlnf("Attachment size: %d", attachmentInfos.size());
                printlnf("Attachment information: %s", attachmentInfos.toString());
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Apps not found, or attachment config not extracted correctly: " + e.getMessage(), e);
        }

        if (apiConfig == null) {
            apiConfig = HydraLabAPIConfig.defaultAPI();
        }
        String testFileSetId = uploadApp(apiConfig, commitId, commitCount, commitMsg, app, testApp);
        printlnf("##[section]Uploaded test file set id: %s", testFileSetId);
        assertNotNull(testFileSetId, "testFileSetId");

        // TODO: make the pipeline link fetch approach compatible to other types of pipeline variables.
        apiConfig.pipelineLink = System.getenv("SYSTEM_TEAMFOUNDATIONSERVERURI") + System.getenv("SYSTEM_TEAMPROJECT") + "/_build/results?buildId=" + System.getenv("BUILD_BUILDID");
        printlnf("##[section]Callback pipeline link is: %s", apiConfig.pipelineLink);

        for (int index = 0; index < attachmentInfos.size(); index++){
            JsonObject attachmentJson = attachmentInfos.get(index).getAsJsonObject();
            AttachmentInfo attachmentInfo = GSON.fromJson(attachmentJson, AttachmentInfo.class);

            assertTrue(!attachmentInfo.filePath.isEmpty(), "Attachment file " + attachmentInfo.fileName + "has an empty path.", null);
            File attachment = new File(attachmentInfo.filePath);
            assertTrue(attachment.exists(), "Attachment file " + attachmentInfo.fileName + "doesn't exist.", null);

            JsonObject responseContent = addAttachment(apiConfig, testFileSetId, attachmentInfo, attachment);
            int resultCode = responseContent.get("code").getAsInt();

            int waitingRetry = 10;
            while (resultCode != 200 && waitingRetry > 0) {
                printlnf("##[warning]Attachment %s uploading failed, remaining retry times: %d\nCode: %d, message: %s", attachmentInfo.filePath, waitingRetry, resultCode, responseContent.get("message").getAsString());
                responseContent = addAttachment(apiConfig, testFileSetId, attachmentInfo, attachment);
                resultCode = responseContent.get("code").getAsInt();
                waitingRetry--;
            }
            assertTrue(resultCode == 200, "Attachment " + attachmentInfo.filePath + " uploading failed, test exits with exception:\n", responseContent.get("message").getAsString());
            printlnf("##[command]Attachment %s uploaded successfully", attachmentInfo.filePath);
        }

        String accessKey = generateAccessKey(apiConfig, deviceIdentifier);
        if (StringUtils.isEmpty(accessKey)) {
            printlnf("##[warning]Access key is empty.");
        }
        else {
            printlnf("##[command]Access key obtained.");
        }

        JsonObject responseContent = triggerTestRun(runningType, apiConfig, testFileSetId, testSuiteName, deviceIdentifier, accessKey, runTimeoutSec, instrumentationArgs, extraArgs);
        int resultCode = responseContent.get("code").getAsInt();

        // retry
        int waitingRetry = 20;
        while (resultCode != 200 && waitingRetry > 0) {
            responseContent = triggerTestRun(runningType, apiConfig, testFileSetId, testSuiteName, deviceIdentifier, accessKey, runTimeoutSec, instrumentationArgs, extraArgs);
            resultCode = responseContent.get("code").getAsInt();
            waitingRetry--;
        }
        assertTrue(resultCode == 200, "Server returned code: " + resultCode, responseContent.get("message").getAsString());

        String testTaskId = responseContent.getAsJsonObject("content").get("testTaskId").getAsString();
        printlnf("##[section]Triggered test task id: %s successful!", testTaskId);

        int sleepSecond = runTimeoutSec / 3;
        int totalWaitSecond = 0;
        boolean finished = false;
        TestTask runningTest = null;
        int hydraRetryTime = 0;
        String lastStatus = "";
        String currentStatus = "";

        while (!finished) {
            if (TestTask.TestStatus.WAITING.equals(currentStatus)) {
                if (totalWaitSecond > queueTimeoutSec) {
                    cancelTestTask(apiConfig, testTaskId);
                    printlnf("Cancelled the task as timeout %d seconds is reached", queueTimeoutSec);
                    break;
                }
                printlnf("Get test status after queuing for %d seconds", totalWaitSecond);
            } else if (TestTask.TestStatus.RUNNING.equals(currentStatus)) {
                if (totalWaitSecond > runTimeoutSec) {
                    break;
                }
                printlnf("Get test status after running for %d seconds", totalWaitSecond);
            }

            runningTest = getTestStatus(apiConfig, testTaskId);
            printlnf("Current running test info: %s", runningTest.toString());
            assertNotNull(runningTest, "testTask");

            lastStatus = currentStatus;
            currentStatus = runningTest.status;

            if (hydraRetryTime != runningTest.retryTime) {
                hydraRetryTime = runningTest.retryTime;
                printlnf("##[command]Retrying to run task again, current waited second will be reset. current retryTime is: %d", hydraRetryTime);
                totalWaitSecond = 0;
                sleepSecond = runTimeoutSec / 3;
            }

            if (TestTask.TestStatus.WAITING.equals(currentStatus)) {
                printlnf("##[command]" + runningTest.message + " Start waiting: 30 seconds");
                sleepIgnoreInterrupt(waitStartSec);
                totalWaitSecond += waitStartSec;
            } else {
                if (TestTask.TestStatus.WAITING.equals(lastStatus)) {
                    printlnf("##[command]Clear waiting time: %d", totalWaitSecond);
                    totalWaitSecond = 0;
                    sleepSecond = runTimeoutSec / 3;
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
            assertTrue(finished, "Queuing timeout after waiting for " + queueTimeoutSec + " seconds! Test id", runningTest);
        } else if (TestTask.TestStatus.RUNNING.equals(currentStatus)) {
            assertTrue(finished, "Running timeout after waiting for " + runTimeoutSec + " seconds! Test id", runningTest);
        }

        assertNotNull(runningTest, "runningTest");
        assertNotNull(runningTest.deviceTestResults, "runningTest.deviceTestResults");

        String testReportUrl = apiConfig.getTestReportUrl(runningTest.id);

        StringBuilder mdBuilder = new StringBuilder("# Hydra Lab Test Result Details\n\n\n");
        mdBuilder.append(String.format("### [Link to full report](%s)\n\n\n", testReportUrl));
        mdBuilder.append(String.format("### Statistic: total test case count: %s, failed: %s\n\n", runningTest.totalTestCount, runningTest.totalFailCount));
        if (runningTest.totalFailCount > 0 && runningTest.reportImagePath != null) {
            printlnf("##[warning] %d cases failed during the test", runningTest.totalFailCount);
        }

        if (runningTest.totalFailCount > 0) {
            printlnf("##[error]Fatal error during test, total fail count: %d", runningTest.totalFailCount);
            markBuildFail();
        }

        int index = 0;

        printlnf("##vso[task.setprogress value=90;]Almost Done with testing");
        printlnf("##[section]Start going through device test results, Test overall info: %s", runningTest);

        ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
        String deviceFolderPrefix = runningType + "-" + utc.format(DateTimeFormatter.ofPattern("MMddHHmmss")) + "-";

        for (DeviceTestResult deviceTestResult : runningTest.deviceTestResults) {
            printlnf(">>>>>>\n Device %s, failed cases count: %d, total cases: %d", deviceTestResult.deviceSerialNumber, deviceTestResult.failCount, deviceTestResult.totalCount);

            if (deviceTestResult.failCount > 0 || deviceTestResult.totalCount == 0) {
                if (deviceTestResult.crashStack != null && deviceTestResult.crashStack.length() > 0) {
                    printlnf("##[error]Fatal error during test on device %s, stack:\n%s", deviceTestResult.deviceSerialNumber, deviceTestResult.crashStack);
                } else {
                    printlnf("##[error]Fatal error during test on device %s with no stack found.", deviceTestResult.deviceSerialNumber);
                }
                markBuildFail();
            }

            if (deviceTestResult.attachments.size() != 0) {
                // add (test type + timestamp) in folder name to distinguish different test results when using the same device
                String distinctFolderName = deviceFolderPrefix + deviceTestResult.deviceSerialNumber;
                String fileDirectoryPath = reportFolderPath + File.separator + distinctFolderName;
                File file = new File(fileDirectoryPath);
                if (!file.exists()) {
                    file.mkdirs();
                }

                for (BlobFileInfo fileInfo : deviceTestResult.attachments) {
                    String attachmentUrl = fileInfo.blobUrl;
                    String attachmentFileName = fileInfo.fileName;

                    printlnf("Start downloading attachment for device %s, device name: %s, file name: %s, link: %s", deviceTestResult.deviceSerialNumber, deviceTestResult.deviceName, attachmentFileName, attachmentUrl);

                    file = new File(fileDirectoryPath, attachmentFileName);
                    downloadToFile(attachmentUrl, file);

                    printlnf("Finish downloading attachment %s for device %s", attachmentFileName, deviceTestResult.deviceSerialNumber);
                }
            }

            String deviceTestVideoUrl = apiConfig.getDeviceTestVideoUrl(deviceTestResult.id);
            printlnf("##[command]Device %s test video link: %s\n>>>>>>>>", deviceTestResult.deviceSerialNumber, deviceTestVideoUrl);
            // set this as a variable as we might need this in next task
            printlnf("##vso[task.setvariable variable=TestVideoLink%d;]%s", ++index, deviceTestVideoUrl);

            mdBuilder.append(String.format(Locale.US, "- On device %s (SN: %s), total case count: %d, failed: %d **[Video Link](%s)**\n", deviceTestResult.deviceName, deviceTestResult.deviceSerialNumber, deviceTestResult.totalCount, deviceTestResult.failCount, deviceTestVideoUrl));
        }

        printlnf("Attachment folder path: %s", reportFolderPath);
        printlnf("##vso[artifact.upload artifactname=testResult;]%s", reportFolderPath);

        printlnf("##[section]All done, overall failed cases count: %d, total count: %d, devices count: %d", runningTest.totalFailCount, runningTest.totalTestCount, runningTest.testDevicesCount);
        printlnf("##[section]Test task report link:");
        printlnf(testReportUrl);
        printlnf("##vso[task.setvariable variable=TestTaskReportLink;]%s", testReportUrl);

        File summaryMd = new File(reportFolderPath.replace("testResult", "summary"), "TestLabSummary.md");
        File summaryParent = summaryMd.getParentFile();
        if (summaryParent != null && !summaryParent.exists()) {
            summaryParent.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(summaryMd)) {
            IOUtils.write(mdBuilder.toString(), fos, StandardCharsets.UTF_8);
            printlnf("##vso[task.uploadsummary]%s", summaryMd.getAbsolutePath());
        } catch (IOException e) {
            // no need to rethrow
            e.printStackTrace();
        }

        returnFinalTestState();
    }

    private static void markBuildFail() {
        if (sMarkedFail) {
            return;
        }
        printlnf("##vso[build.addbuildtag]FAIL");
        sMarkedFail = true;
    }

    private static void markBuildSuccess() {
        if (sMarkedFail) {
            return;
        }
        printlnf("##vso[build.addbuildtag]SUCCESS");
    }

    private static void returnFinalTestState() {
        assertTrue(!sMarkedFail, "##[error]Final test state: fail.", null);
        printlnf("Final test state: success.");
    }

    private static void downloadToFile(String fileUrl, File file) {
        Request req = new Request.Builder().get().url(fileUrl).build();
        try (Response response = client.newCall(req).execute()) {
            if (!response.isSuccessful()) {
                return;
            }
            if (response.body() == null) {
                return;
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                IOUtils.copy(response.body().byteStream(), fos);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void assertNotNull(Object notnull, String argName) {
        if (notnull == null) {
            throw new IllegalArgumentException(argName + " is null");
        }
    }

    private static void assertTrue(boolean beTrue, String msg, Object data) {
        if (!beTrue) {
            throw new IllegalStateException(msg + (data == null ? "" : ": " + data));
        }
    }

    private static void printlnf(String format, Object... args) {
        ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
        System.out.print("[" + utc.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] ");
        System.out.printf(format + "\n", args);
    }

    private static void sleepIgnoreInterrupt(int second) {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(second));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void checkCenterAlive(HydraLabAPIConfig apiConfig) {
        Request req = new Request.Builder()
                .addHeader("Authorization", "Bearer " + apiConfig.authToken)
                .url(apiConfig.checkCenterAliveUrl())
                .build();
        OkHttpClient clientToUse = client;
        try (Response response = clientToUse.newCall(req).execute()) {
            assertTrue(response.isSuccessful(), "check center alive", response);
            printlnf("Center is alive, continue on requesting API...");
        } catch (Exception e) {
            throw new RuntimeException("check center alive fail: " + e.getMessage(), e);
        }
    }

    private static String uploadApp(HydraLabAPIConfig apiConfig, String commitId, String commitCount, String commitMsg, File app, File testApp) {
        checkCenterAlive(apiConfig);

        MediaType contentType = MediaType.get("application/vnd.android.package-archive");
        MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("commitId", commitId)
                .addFormDataPart("commitCount", commitCount)
                .addFormDataPart("commitMessage", commitMsg)
                .addFormDataPart("appFile", app.getName(), RequestBody.create(contentType, app));
        if (!StringUtils.isEmpty(apiConfig.teamName)) {
            multipartBodyBuilder.addFormDataPart("teamName", apiConfig.teamName);
        }
        if (testApp != null) {
            multipartBodyBuilder.addFormDataPart("testAppFile", testApp.getName(), RequestBody.create(contentType, testApp));
        }

        Request req = new Request.Builder()
                .addHeader("Authorization", "Bearer " + apiConfig.authToken)
                .url(apiConfig.getUploadUrl())
                .post(multipartBodyBuilder.build())
                .build();
        OkHttpClient clientToUse = client;
        try (Response response = clientToUse.newCall(req).execute()) {
            assertTrue(response.isSuccessful(), "upload App", response);
            ResponseBody body = response.body();

            assertNotNull(body, response + ": upload App ResponseBody");
            JsonObject jsonObject = GSON.fromJson(body.string(), JsonObject.class);

            int resultCode = jsonObject.get("code").getAsInt();
            assertTrue(resultCode == 200, "Server returned code: " + resultCode, jsonObject);

            return jsonObject.getAsJsonObject("content").get("id").getAsString();
        } catch (Exception e) {
            throw new RuntimeException("upload App fail: " + e.getMessage(), e);
        }
    }

    private static JsonObject addAttachment(HydraLabAPIConfig apiConfig, String testFileSetId, AttachmentInfo attachmentConfig, File attachment) {
        checkCenterAlive(apiConfig);

        // default text file type: text/plain
        // default binary file type: application/octet-stream
        // todo: check if file is readable, set corresponding type
        MediaType contentType = MediaType.get("application/octet-stream");
        MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("fileSetId", testFileSetId)
                .addFormDataPart("fileType", attachmentConfig.fileType)
                .addFormDataPart("attachment", attachmentConfig.fileName, RequestBody.create(contentType, attachment));
        if (StringUtils.isNotEmpty(attachmentConfig.loadType)) {
            multipartBodyBuilder.addFormDataPart("loadType", attachmentConfig.loadType);
        }
        if (StringUtils.isNotEmpty(attachmentConfig.loadDir)) {
            multipartBodyBuilder.addFormDataPart("loadDir", attachmentConfig.loadDir);
        }

        Request req = new Request.Builder()
                .addHeader("Authorization", "Bearer " + apiConfig.authToken)
                .url(apiConfig.getAddAttachmentUrl())
                .post(multipartBodyBuilder.build())
                .build();
        OkHttpClient clientToUse = client;
        try (Response response = clientToUse.newCall(req).execute()) {
            assertTrue(response.isSuccessful(), "Add attachments", response);
            ResponseBody body = response.body();
            assertNotNull(body, response + ": add attachments ResponseBody");

            return GSON.fromJson(body.string(), JsonObject.class);
        } catch (Exception e) {
            throw new RuntimeException("Add attachments fail: " + e.getMessage(), e);
        }
    }

    private static String generateAccessKey(HydraLabAPIConfig apiConfig, String deviceIdentifier) {
        checkCenterAlive(apiConfig);

        Request req = new Request.Builder()
                .addHeader("Authorization", "Bearer " + apiConfig.authToken)
                .url(String.format(apiConfig.getGenerateAccessKeyUrl(), deviceIdentifier))
                .get()
                .build();
        OkHttpClient clientToUse = client;
        JsonObject jsonObject = null;
        try (Response response = clientToUse.newCall(req).execute()) {
            assertTrue(response.isSuccessful(), "generate accessKey", response);
            ResponseBody body = response.body();

            assertNotNull(body, response + ": generateAccessKey ResponseBody");
            jsonObject = GSON.fromJson(body.string(), JsonObject.class);

            int resultCode = jsonObject.get("code").getAsInt();
            assertTrue(resultCode == 200, "Server returned code: " + resultCode, jsonObject);

            return jsonObject.getAsJsonObject("content").get("key").getAsString();
        } catch (Exception e) {
            // TODO: no blocking for now, replace after enabling the access key usage
            printlnf("##[warning]Request generateAccess failed: " + jsonObject.toString());
            return "";
//            throw new RuntimeException("generate accessKey fail: " + e.getMessage(), e);
        }
    }

    private static JsonObject triggerTestRun(String runningType, HydraLabAPIConfig apiConfig, String fileSetId, String testSuiteName,
                                             String deviceIdentifier, @Nullable String accessKey, int runTimeoutSec, Map<String, String> instrumentationArgs, Map<String, String> extraArgs) {
        checkCenterAlive(apiConfig);

        JsonObject jsonElement = new JsonObject();
        jsonElement.addProperty("runningType", runningType);
        jsonElement.addProperty("deviceIdentifier", deviceIdentifier);
        jsonElement.addProperty("fileSetId", fileSetId);
        jsonElement.addProperty("testSuiteClass", testSuiteName);
        jsonElement.addProperty("testTimeOutSec", runTimeoutSec);
        jsonElement.addProperty("pkgName", apiConfig.pkgName);
        jsonElement.addProperty("testPkgName", apiConfig.testPkgName);
        jsonElement.addProperty("groupTestType", apiConfig.groupTestType);
        jsonElement.addProperty("pipelineLink", apiConfig.pipelineLink);
        jsonElement.addProperty("frameworkType", apiConfig.frameworkType);
        jsonElement.addProperty("maxStepCount", apiConfig.maxStepCount);
        jsonElement.addProperty("deviceTestCount", apiConfig.deviceTestCount);
        jsonElement.addProperty("needUninstall", apiConfig.needUninstall);
        jsonElement.addProperty("needClearData", apiConfig.needClearData);
        jsonElement.addProperty("testRunnerName", apiConfig.testRunnerName);

        if (accessKey != null) {
            jsonElement.addProperty("accessKey", accessKey);
        }
        if (instrumentationArgs != null) {
            jsonElement.add("instrumentationArgs", GSON.toJsonTree(instrumentationArgs).getAsJsonObject());
        }
        if (extraArgs != null) {
            extraArgs.forEach(jsonElement::addProperty);
        }

        String content = GSON.toJson(jsonElement);
        printlnf("triggerTestRun api post body: %s", maskCred(content));
        RequestBody jsonBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), GSON.toJson(jsonElement));

        Request req = new Request.Builder()
                .addHeader("Authorization", "Bearer " + apiConfig.authToken)
                .url(apiConfig.getRunTestUrl())
                .post(jsonBody).build();
        OkHttpClient clientToUse = client;
        try (Response response = clientToUse.newCall(req).execute()) {
            assertTrue(response.isSuccessful(), "trigger test running", response);
            ResponseBody body = response.body();
            assertNotNull(body, response + ": triggerTestRun ResponseBody");
            String string = body.string();
            printlnf("RunningTestJson: %s", maskCred(string));
            JsonObject jsonObject = GSON.fromJson(string, JsonObject.class);

            return jsonObject;
        } catch (Exception e) {
            throw new RuntimeException("trigger test running fail: " + e.getMessage(), e);
        }
    }

    private static TestTask getTestStatus(HydraLabAPIConfig apiConfig, String testTaskId) {
        checkCenterAlive(apiConfig);

        Request req = new Request.Builder()
                .addHeader("Authorization", "Bearer " + apiConfig.authToken)
                .url(apiConfig.getTestStatusUrl(testTaskId))
                .build();
        OkHttpClient clientToUse = client;
        try (Response response = clientToUse.newCall(req).execute()) {
            assertTrue(response.isSuccessful(), "get test status", response);
            ResponseBody body = response.body();
            assertNotNull(body, response + ": getTestStatus ResponseBody");
            JsonObject jsonObject = GSON.fromJson(body.string(), JsonObject.class);

            int resultCode = jsonObject.get("code").getAsInt();
            assertTrue(resultCode == 200, "Server returned code: " + resultCode, jsonObject);

            TestTask result = GSON.fromJson(jsonObject.getAsJsonObject("content"), TestTask.class);
            if (result.id == null) {
                result.id = testTaskId;
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("get test status fail: " + e.getMessage(), e);
        }
    }

    private static void cancelTestTask(HydraLabAPIConfig apiConfig, String testTaskId) {
        checkCenterAlive(apiConfig);

        Request req = new Request.Builder()
                .addHeader("Authorization", "Bearer " + apiConfig.authToken)
                .url(apiConfig.getCancelTestTaskUrl(testTaskId))
                .build();
        OkHttpClient clientToUse = client;
        try (Response response = clientToUse.newCall(req).execute()) {
            assertTrue(response.isSuccessful(), "cancel test task", response);
        } catch (Exception e) {
            throw new RuntimeException("cancel test task fail: " + e.getMessage(), e);
        }
    }

    private static String maskCred(String content) {
        for (MaskSensitiveData sensitiveData : MaskSensitiveData.values()) {
            Pattern PATTERNCARD = Pattern.compile(sensitiveData.getRegEx(), Pattern.CASE_INSENSITIVE);
            Matcher matcher = PATTERNCARD.matcher(content);
            if (matcher.find()) {
                String maskedMessage = matcher.group(2);
                if (maskedMessage.length() > 0) {
                    content = content.replaceFirst(maskedMessage, "***");
                }
            }
        }

        return content;
    }

    private static String getCommitCount(File commandDir, String startCommit) throws IOException {
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
        Process process = Runtime.getRuntime().exec("git log --pretty=format:%s " + commitId + " -1", null, workingDirFile.getAbsoluteFile());
        try (InputStream inputStream = process.getInputStream()) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8).trim();
        } finally {
            process.destroy();
        }
    }

    public static class HydraLabAPIConfig {
        public String schema = "https";
        public String host = "";
        public String contextPath = "";
        public String authToken = "";
        public boolean onlyAuthPost = true;
        public String checkCenterVersionAPIPath = "/api/center/info";
        public String checkCenterAliveAPIPath = "/api/center/isAlive";
        public String uploadAPKAPIPath = "/api/package/add";
        public String addAttachmentAPIPath = "/api/package/addAttachment";
        public String generateAccessKeyAPIPath = "/api/deviceGroup/generate?deviceIdentifier=%s";
        public String runTestAPIPath = "/api/test/task/run/";
        public String testStatusAPIPath = "/api/test/task/";
        public String cancelTestTaskAPIPath = "/api/test/task/cancel/";
        public String testPortalTaskInfoPath = "/portal/index.html?redirectUrl=/info/task/";
        public String testPortalTaskDeviceVideoPath = "/portal/index.html?redirectUrl=/info/videos/";
        public String pkgName = "";
        public String testPkgName = "";
        public String groupTestType = "SINGLE";
        public String pipelineLink = "";
        public String frameworkType = "JUnit4";
        public int maxStepCount = 100;
        public int deviceTestCount = -1;
        public boolean needUninstall = true;
        public boolean needClearData = true;
        public String teamName = "";
        public String testRunnerName = "androidx.test.runner.AndroidJUnitRunner";

        public static HydraLabAPIConfig defaultAPI() {
            return new HydraLabAPIConfig();
        }

        public String checkCenterAliveUrl() {
            return String.format(Locale.US, "%s://%s%s%s", schema, host, contextPath, checkCenterAliveAPIPath);
        }

        public String getUploadUrl() {
            return String.format(Locale.US, "%s://%s%s%s", schema, host, contextPath, uploadAPKAPIPath);
        }

        public String getAddAttachmentUrl() {
            return String.format(Locale.US, "%s://%s%s%s", schema, host, contextPath, addAttachmentAPIPath);
        }

        public String getGenerateAccessKeyUrl() {
            return String.format(Locale.US, "%s://%s%s%s", schema, host, contextPath, generateAccessKeyAPIPath);
        }

        public String getRunTestUrl() {
            return String.format(Locale.US, "%s://%s%s%s", schema, host, contextPath, runTestAPIPath);
        }

        public String getTestStatusUrl(String testTaskId) {
            return String.format(Locale.US, "%s://%s%s%s%s", schema, host, contextPath, testStatusAPIPath, testTaskId);
        }

        public String getCancelTestTaskUrl(String testTaskId) {
            return String.format(Locale.US, "%s://%s%s%s%s", schema, host, contextPath, cancelTestTaskAPIPath, testTaskId);
        }

        public String getTestReportUrl(String testTaskId) {
            return String.format(Locale.US, "%s://%s%s%s%s", schema, host, contextPath, testPortalTaskInfoPath, testTaskId);
        }

        public String getDeviceTestVideoUrl(String id) {
            return String.format(Locale.US, "%s://%s%s%s%s", schema, host, contextPath, testPortalTaskDeviceVideoPath, id);
        }

        @Override
        public String toString() {
            return "HydraLabAPIConfig Upload URL {" + getUploadUrl() + '}';
        }

        public String getTestStaticResUrl(String resPath) {
            return String.format(Locale.US, "%s://%s%s%s", schema, host, contextPath, resPath);
        }
    }

    public static class AttachmentInfo {
        String fileName;
        String filePath;
        String fileType;
        String loadType;
        String loadDir;
    }

    public static class TestTask {
        public String id;
        public List<DeviceTestResult> deviceTestResults;
        public int testDevicesCount;
        public Date startDate;
        public Date endDate;
        public int totalTestCount;
        public int totalFailCount;
        public String testSuite;
        public String reportImagePath;
        public String baseUrl;
        public String status;
        public String testErrorMsg;
        public String message;
        public int retryTime;

        @Override
        public String toString() {
            return "TestTask{" +
                    "id='" + id + '\'' +
                    ", testDevicesCount=" + testDevicesCount +
                    ", startDate=" + startDate +
                    ", totalTestCount=" + totalTestCount +
                    ", status='" + status + '\'' +
                    '}';
        }

        public interface TestStatus {
            String RUNNING = "running";
            String FINISHED = "finished";
            String CANCELED = "canceled";
            String EXCEPTION = "error";
            String WAITING = "waiting";
        }
    }

    public static class DeviceTestResult {
        public String id;
        public String deviceSerialNumber;
        public String deviceName;
        public String instrumentReportPath;
        public String controlLogPath;
        public String instrumentReportBlobUrl;
        public String testXmlReportBlobUrl;
        public String logcatBlobUrl;
        public String testGifBlobUrl;

        public List<BlobFileInfo> attachments;

        public String crashStackId;
        public String errorInProcess;

        public String crashStack;

        public int totalCount;
        public int failCount;
        public boolean success;
        public long testStartTimeMillis;
        public long testEndTimeMillis;

        @Override
        public String toString() {
            return "{" +
                    "SN='" + deviceSerialNumber + '\'' +
                    ", totalCase:" + totalCount +
                    ", failCase:" + failCount +
                    ", success:" + success +
                    '}';
        }
    }

    public static class BlobFileInfo {
        public String fileId;
        public String fileType;
        public String fileName;
        public String blobUrl;
        public String blobPath;
        public long fileLen;
        public String md5;
        public String loadDir;
        public String loadType;
        public JsonObject fileParser;
        public Date createTime;
        public Date updateTime;


        public interface fileType {
            String WINDOWS_APP = "WINAPP";
            String COMMOM_FILE = "COMMON";
            String AGENT_PACKAGE = "PACKAGE";
            String APP_FILE = "APP";
            String TEST_APP_FILE = "TEST_APP";
        }

        public interface loadType {
            String CPOY = "COPY";
            String UNZIP = "UNZIP";
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