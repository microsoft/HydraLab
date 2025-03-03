// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.microsoft.hydralab.config.DeviceConfig;
import com.microsoft.hydralab.config.HydraLabAPIConfig;
import com.microsoft.hydralab.config.TestConfig;
import com.microsoft.hydralab.entity.AttachmentInfo;
import com.microsoft.hydralab.entity.TestTask;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.microsoft.hydralab.utils.CommonUtils.GSON;
import static com.microsoft.hydralab.utils.CommonUtils.assertNotNull;
import static com.microsoft.hydralab.utils.CommonUtils.assertTrue;
import static com.microsoft.hydralab.utils.CommonUtils.maskCred;
import static com.microsoft.hydralab.utils.CommonUtils.printlnf;


public class HydraLabAPIClient {
    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(300, TimeUnit.SECONDS)
            .connectTimeout(300, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    private final int httpFailureRetryTimes = 10;

    public void checkCenterAlive(HydraLabAPIConfig apiConfig) {
        Request req = new Request.Builder()
                .addHeader("Authorization", "Bearer " + apiConfig.authToken)
                .url(apiConfig.checkCenterAliveUrl())
                .build();
        OkHttpClient clientToUse = client;
        AtomicInteger waitingRetry = new AtomicInteger(httpFailureRetryTimes);
        try (Response response = FlowUtil.httpRetryAndSleepWhenException(httpFailureRetryTimes, 1,
                () -> {
                    Response res = clientToUse.newCall(req).execute();
                    if (!res.isSuccessful()) {
                        waitingRetry.getAndDecrement();
                        printlnf("##[warning]Check center alive failed, remaining retry times: %d\nHttp code: %d\nHttp message: %s", waitingRetry.get(), res.code(),
                                res.message());
                        throw new IllegalStateException("Check center alive: " + res);
                    }
                    return res;
                }
        )) {
            printlnf("Center is alive, continue on requesting API...");
        } catch (Exception e) {
            throw new RuntimeException("check center alive fail: " + e.getMessage(), e);
        }
    }

    public String uploadApp(HydraLabAPIConfig apiConfig, TestConfig testConfig, String commitId, String commitCount, String commitMsg, File app, File testApp) {
        checkCenterAlive(apiConfig);

        MediaType contentType = MediaType.get("application/vnd.android.package-archive");
        MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("commitId", commitId)
                .addFormDataPart("commitCount", commitCount)
                .addFormDataPart("commitMessage", commitMsg)
                .addFormDataPart("appFile", app.getName(), RequestBody.create(contentType, app));
        if (!StringUtils.isEmpty(testConfig.teamName)) {
            multipartBodyBuilder.addFormDataPart("teamName", testConfig.teamName);
        }
        if (testApp != null) {
            multipartBodyBuilder.addFormDataPart("testAppFile", testApp.getName(), RequestBody.create(contentType, testApp));
        }
        if (StringUtils.isNotBlank(testConfig.appVersion)) {
            multipartBodyBuilder.addFormDataPart("appVersion", testConfig.appVersion);
        }

        Request req = new Request.Builder()
                .addHeader("Authorization", "Bearer " + apiConfig.authToken)
                .url(apiConfig.getUploadUrl())
                .post(multipartBodyBuilder.build())
                .build();
        OkHttpClient clientToUse = client;
        AtomicInteger waitingRetry = new AtomicInteger(httpFailureRetryTimes);
        try (Response response = FlowUtil.httpRetryAndSleepWhenException(httpFailureRetryTimes, 1,
                () -> {
                    Response res = clientToUse.newCall(req).execute();
                    if (!res.isSuccessful()) {
                        waitingRetry.getAndDecrement();
                        printlnf("##[warning]Upload App failed, remaining retry times: %d\nHttp code: %d\nHttp message: %s", waitingRetry.get(), res.code(),
                                res.message());
                        throw new IllegalStateException("Upload app: " + res);
                    }
                    return res;
                }
        )) {
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

    public JsonObject addAttachment(HydraLabAPIConfig apiConfig, String testFileSetId, AttachmentInfo attachmentConfig, File attachment) {
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
        AtomicInteger waitingRetry = new AtomicInteger(httpFailureRetryTimes);
        try (Response response = FlowUtil.httpRetryAndSleepWhenException(httpFailureRetryTimes, 1,
                () -> {
                    Response res = clientToUse.newCall(req).execute();
                    if (!res.isSuccessful()) {
                        waitingRetry.getAndDecrement();
                        printlnf("##[warning]Add attachments failed, remaining retry times: %d\nHttp code: %d\nHttp message: %s", waitingRetry.get(), res.code(),
                                res.message());
                        throw new IllegalStateException("Add attachments: " + res);
                    }
                    return res;
                }
        )) {
            ResponseBody body = response.body();
            assertNotNull(body, response + ": Add attachments ResponseBody");

            return GSON.fromJson(body.string(), JsonObject.class);
        } catch (Exception e) {
            throw new RuntimeException("Add attachments fail: " + e.getMessage(), e);
        }
    }

    public String generateAccessKey(HydraLabAPIConfig apiConfig, TestConfig testConfig) {
        checkCenterAlive(apiConfig);

        Request req = new Request.Builder()
                .addHeader("Authorization", "Bearer " + apiConfig.authToken)
                .url(String.format(apiConfig.getGenerateAccessKeyUrl(), testConfig.deviceConfig.deviceIdentifier))
                .get()
                .build();
        OkHttpClient clientToUse = client;
        JsonObject jsonObject = null;
        AtomicInteger waitingRetry = new AtomicInteger(httpFailureRetryTimes);
        try (Response response = FlowUtil.httpRetryAndSleepWhenException(httpFailureRetryTimes, 1,
                () -> {
                    Response res = clientToUse.newCall(req).execute();
                    if (!res.isSuccessful()) {
                        waitingRetry.getAndDecrement();
                        printlnf("##[warning]Generate accessKey failed, remaining retry times: %d\nHttp code: %d\nHttp message: %s", waitingRetry.get(), res.code(),
                                res.message());
                        throw new IllegalStateException("Generate accessKey: " + res);
                    }
                    return res;
                }
        )) {

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

    public JsonObject triggerTestRun(TestConfig testConfig, HydraLabAPIConfig apiConfig, String fileSetId, @Nullable String accessKey) {
        checkCenterAlive(apiConfig);

        DeviceConfig deviceConfig = testConfig.deviceConfig;

        JsonObject jsonElement = new JsonObject();
        jsonElement.addProperty("type", testConfig.triggerType);
        jsonElement.addProperty("runningType", testConfig.runningType);
        jsonElement.addProperty("deviceIdentifier", deviceConfig.deviceIdentifier);
        jsonElement.addProperty("fileSetId", fileSetId);
        jsonElement.addProperty("testSuiteClass", testConfig.testSuiteName);
        jsonElement.addProperty("testTimeOutSec", testConfig.runTimeOutSeconds);
        jsonElement.addProperty("pkgName", testConfig.pkgName);
        jsonElement.addProperty("testPkgName", testConfig.testPkgName);
        jsonElement.addProperty("groupTestType", deviceConfig.groupTestType);
        jsonElement.addProperty("pipelineLink", testConfig.pipelineLink);
        jsonElement.addProperty("frameworkType", testConfig.frameworkType);
        jsonElement.addProperty("maxStepCount", testConfig.maxStepCount);
        jsonElement.addProperty("deviceTestCount", testConfig.testRound);
        jsonElement.addProperty("skipInstall", testConfig.skipInstall);
        jsonElement.addProperty("needUninstall", testConfig.needUninstall);
        jsonElement.addProperty("needClearData", testConfig.needClearData);
        jsonElement.addProperty("testRunnerName", testConfig.testRunnerName);
        jsonElement.addProperty("testScope", testConfig.testScope);
        jsonElement.addProperty("disableRecording", testConfig.disableRecording);
        jsonElement.addProperty("enableNetworkMonitor", testConfig.enableNetworkMonitor);
        jsonElement.addProperty("networkMonitorRule", testConfig.networkMonitorRule);
        jsonElement.addProperty("enableTestOrchestrator", testConfig.enableTestOrchestrator);
        jsonElement.addProperty("notifyUrl", testConfig.notifyUrl);
        jsonElement.addProperty("blockDevice", testConfig.blockDevice);
        jsonElement.addProperty("unblockDevice", testConfig.unblockDevice);
        jsonElement.addProperty("unblockDeviceSecretKey", testConfig.unblockDeviceSecretKey);

        try {
            if (testConfig.neededPermissions.size() > 0) {
                jsonElement.add("neededPermissions", GSON.toJsonTree(testConfig.neededPermissions));
            }
            if (StringUtils.isNotBlank(testConfig.inspectionStrategiesStr)) {
                JsonParser parser = new JsonParser();
                JsonArray jsonArray = parser.parse(testConfig.inspectionStrategiesStr).getAsJsonArray();
                jsonElement.add("inspectionStrategies", jsonArray);
            }
            if (StringUtils.isNotBlank(deviceConfig.deviceActionsStr)) {
                JsonParser parser = new JsonParser();
                JsonObject jsonObject = parser.parse(deviceConfig.deviceActionsStr).getAsJsonObject();
                jsonElement.add("deviceActions", jsonObject);
            }
            if (testConfig.testRunArgs != null) {
                jsonElement.add("testRunArgs", GSON.toJsonTree(testConfig.testRunArgs).getAsJsonObject());
            }
            if (StringUtils.isNotBlank(testConfig.analysisConfigsStr) && "APK_SCANNER".equals(testConfig.runningType)) {
                JsonParser parser = new JsonParser();
                JsonArray jsonArray = parser.parse(testConfig.analysisConfigsStr).getAsJsonArray();
                jsonElement.add("analysisConfigs", jsonArray);
            }

        } catch (JsonParseException e) {
            throw new RuntimeException("trigger test running fail: " + e.getMessage(), e);
        }

        if (accessKey != null) {
            jsonElement.addProperty("accessKey", accessKey);
        }

        String content = GSON.toJson(jsonElement);
        printlnf("triggerTestRun api post body: %s", maskCred(content));
        RequestBody jsonBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), GSON.toJson(jsonElement));

        Request req = new Request.Builder()
                .addHeader("Authorization", "Bearer " + apiConfig.authToken)
                .url(apiConfig.getRunTestUrl())
                .post(jsonBody).build();
        OkHttpClient clientToUse = client;
        AtomicInteger waitingRetry = new AtomicInteger(httpFailureRetryTimes);
        try (Response response = FlowUtil.httpRetryAndSleepWhenException(httpFailureRetryTimes, 1,
                () -> {
                    Response res = clientToUse.newCall(req).execute();
                    if (!res.isSuccessful()) {
                        waitingRetry.getAndDecrement();
                        printlnf("##[warning]Trigger test running failed, remaining retry times: %d\nHttp code: %d\nHttp message: %s", waitingRetry.get(), res.code(),
                                res.message());
                        throw new IllegalStateException("Trigger test running: " + res);
                    }
                    return res;
                }
        )) {
            ResponseBody body = response.body();
            assertNotNull(body, response + ": triggerTestRun ResponseBody");
            String string = body.string();
            printlnf("RunningTestJson: %s", maskCred(string));

            return GSON.fromJson(string, JsonObject.class);
        } catch (Exception e) {
            throw new RuntimeException("trigger test running fail: " + e.getMessage(), e);
        }
    }

    public TestTask getTestStatus(HydraLabAPIConfig apiConfig, String testTaskId) {
        checkCenterAlive(apiConfig);

        Request req = new Request.Builder()
                .addHeader("Authorization", "Bearer " + apiConfig.authToken)
                .url(apiConfig.getTestStatusUrl(testTaskId))
                .build();
        OkHttpClient clientToUse = client;
        AtomicInteger waitingRetry = new AtomicInteger(httpFailureRetryTimes);
        try (Response response = FlowUtil.httpRetryAndSleepWhenException(httpFailureRetryTimes, 1,
                () -> {
                    Response res = clientToUse.newCall(req).execute();
                    if (!res.isSuccessful()) {
                        waitingRetry.getAndDecrement();
                        printlnf("##[warning]Get test status failed, remaining retry times: %d\nHttp code: %d\nHttp message: %s", waitingRetry.get(), res.code(),
                                res.message());
                        throw new IllegalStateException("Get test status: " + res);
                    }
                    return res;
                }
        )) {
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

    public String getBlobSAS(HydraLabAPIConfig apiConfig) {
        Request req = new Request.Builder()
                .addHeader("Authorization", "Bearer " + apiConfig.authToken)
                .url(apiConfig.getBlobSASUrl())
                .build();
        OkHttpClient clientToUse = client;
        AtomicInteger waitingRetry = new AtomicInteger(httpFailureRetryTimes);
        try (Response response = FlowUtil.httpRetryAndSleepWhenException(httpFailureRetryTimes, 1,
                () -> {
                    Response res = clientToUse.newCall(req).execute();
                    if (!res.isSuccessful()) {
                        waitingRetry.getAndDecrement();
                        printlnf("##[warning]Get Blob SAS failed, remaining retry times: %d\nHttp code: %d\nHttp message: %s", waitingRetry.get(), res.code(),
                                res.message());
                        throw new IllegalStateException("Get Blob SAS: " + res);
                    }
                    return res;
                }
        )) {
            ResponseBody body = response.body();

            assertNotNull(body, response + ": Blob SAS");
            JsonObject jsonObject = GSON.fromJson(body.string(), JsonObject.class);

            int resultCode = jsonObject.get("code").getAsInt();
            assertTrue(resultCode == 200, "Server returned code: " + resultCode, jsonObject);

            return jsonObject.getAsJsonObject("content").get("signature").getAsString();
        } catch (Exception e) {
            throw new RuntimeException("Get Blob SAS fail: " + e.getMessage(), e);
        }
    }

    public void downloadToFile(String fileUrl, File file) {
        Request req = new Request.Builder().get().url(fileUrl).build();
        try (Response response = FlowUtil.httpRetryAndSleepWhenException(httpFailureRetryTimes, 1,
                () -> client.newCall(req).execute()
        )) {
            if (!response.isSuccessful()) {
                return;
            }
            if (response.body() == null) {
                return;
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                IOUtils.copy(response.body().byteStream(), fos);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancelTestTask(HydraLabAPIConfig apiConfig, String testTaskId, String reason) {
        checkCenterAlive(apiConfig);

        Request req = new Request.Builder()
                .addHeader("Authorization", "Bearer " + apiConfig.authToken)
                .url(String.format(apiConfig.getCancelTestTaskUrl(), testTaskId, reason))
                .build();
        OkHttpClient clientToUse = client;
        AtomicInteger waitingRetry = new AtomicInteger(httpFailureRetryTimes);
        try (Response response = FlowUtil.httpRetryAndSleepWhenException(httpFailureRetryTimes, 1,
                () -> {
                    Response res = clientToUse.newCall(req).execute();
                    if (!res.isSuccessful()) {
                        waitingRetry.getAndDecrement();
                        printlnf("##[warning]Cancel test task failed, remaining retry times: %d\nHttp code: %d\nHttp message: %s", waitingRetry.get(), res.code(),
                                res.message());
                        throw new IllegalStateException("Cancel test task: " + res);
                    }
                    return res;
                }
        )) {
            printlnf("Test task canceled");
        } catch (Exception e) {
            throw new RuntimeException("cancel test task fail: " + e.getMessage(), e);
        }
    }
}
