// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.utils;

import com.google.gson.*;
import com.microsoft.hydralab.config.DeviceConfig;
import com.microsoft.hydralab.config.HydraLabAPIConfig;
import com.microsoft.hydralab.config.TestConfig;
import com.microsoft.hydralab.entity.*;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.microsoft.hydralab.utils.CommonUtils.*;


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
        Response response = null;
        try {
            response = clientToUse.newCall(req).execute();
            int waitingRetry = httpFailureRetryTimes;
            while (!response.isSuccessful() && waitingRetry > 0) {
                printlnf("##[warning]Check center alive failed, remaining retry times: %d\nHttp code: %d\nHttp message: %s", waitingRetry, response.code(), response.message());
                response = clientToUse.newCall(req).execute();
                waitingRetry--;
            }

            assertTrue(response.isSuccessful(), "check center alive", response);
            printlnf("Center is alive, continue on requesting API...");
        } catch (Exception e) {
            throw new RuntimeException("check center alive fail: " + e.getMessage(), e);
        } finally {
            response.close();
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

        Request req = new Request.Builder()
                .addHeader("Authorization", "Bearer " + apiConfig.authToken)
                .url(apiConfig.getUploadUrl())
                .post(multipartBodyBuilder.build())
                .build();
        OkHttpClient clientToUse = client;
        Response response = null;
        try {
            response = clientToUse.newCall(req).execute();
            int waitingRetry = httpFailureRetryTimes;
            while (!response.isSuccessful() && waitingRetry > 0) {
                printlnf("##[warning]Upload App failed, remaining retry times: %d\nHttp code: %d\nHttp message: %s", waitingRetry, response.code(), response.message());
                response = clientToUse.newCall(req).execute();
                waitingRetry--;
            }

            assertTrue(response.isSuccessful(), "upload App", response);
            ResponseBody body = response.body();

            assertNotNull(body, response + ": upload App ResponseBody");
            JsonObject jsonObject = GSON.fromJson(body.string(), JsonObject.class);

            int resultCode = jsonObject.get("code").getAsInt();
            assertTrue(resultCode == 200, "Server returned code: " + resultCode, jsonObject);

            return jsonObject.getAsJsonObject("content").get("id").getAsString();
        } catch (Exception e) {
            throw new RuntimeException("upload App fail: " + e.getMessage(), e);
        } finally {
            response.close();
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
        Response response = null;
        try {
            response = clientToUse.newCall(req).execute();
            int waitingRetry = httpFailureRetryTimes;
            while (!response.isSuccessful() && waitingRetry > 0) {
                printlnf("##[warning]Add attachments failed, remaining retry times: %d\nHttp code: %d\nHttp message: %s", waitingRetry, response.code(), response.message());
                response = clientToUse.newCall(req).execute();
                waitingRetry--;
            }

            assertTrue(response.isSuccessful(), "Add attachments", response);
            ResponseBody body = response.body();
            assertNotNull(body, response + ": Add attachments ResponseBody");

            return GSON.fromJson(body.string(), JsonObject.class);
        } catch (Exception e) {
            throw new RuntimeException("Add attachments fail: " + e.getMessage(), e);
        } finally {
            response.close();
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
        Response response = null;
        try {
            response = clientToUse.newCall(req).execute();
            int waitingRetry = httpFailureRetryTimes;
            while (!response.isSuccessful() && waitingRetry > 0) {
                printlnf("##[warning]Generate accessKey failed, remaining retry times: %d\nHttp code: %d\nHttp message: %s", waitingRetry, response.code(), response.message());
                response = clientToUse.newCall(req).execute();
                waitingRetry--;
            }

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
        } finally {
            response.close();
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
        jsonElement.addProperty("needUninstall", testConfig.needUninstall);
        jsonElement.addProperty("needClearData", testConfig.needClearData);
        jsonElement.addProperty("testRunnerName", testConfig.testRunnerName);
        jsonElement.addProperty("testScope", testConfig.testScope);

        try {
            if (testConfig.neededPermissions.size() > 0) {
                jsonElement.add("neededPermissions", GSON.toJsonTree(testConfig.neededPermissions));
            }
            if (StringUtils.isNotBlank(deviceConfig.deviceActionsStr)) {
                JsonParser parser = new JsonParser();
                JsonObject jsonObject = parser.parse(deviceConfig.deviceActionsStr).getAsJsonObject();
                jsonElement.add("deviceActions", jsonObject);
            }
            if (testConfig.testRunArgs != null) {
                jsonElement.add("testRunArgs", GSON.toJsonTree(testConfig.testRunArgs).getAsJsonObject());
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
        Response response = null;
        try {
            response = clientToUse.newCall(req).execute();
            int waitingRetry = httpFailureRetryTimes;
            while (!response.isSuccessful() && waitingRetry > 0) {
                printlnf("##[warning]Trigger test running failed, remaining retry times: %d\nHttp code: %d\nHttp message: %s", waitingRetry, response.code(), response.message());
                response = clientToUse.newCall(req).execute();
                waitingRetry--;
            }

            assertTrue(response.isSuccessful(), "trigger test running", response);
            ResponseBody body = response.body();
            assertNotNull(body, response + ": triggerTestRun ResponseBody");
            String string = body.string();
            printlnf("RunningTestJson: %s", maskCred(string));
            JsonObject jsonObject = GSON.fromJson(string, JsonObject.class);

            return jsonObject;
        } catch (Exception e) {
            throw new RuntimeException("trigger test running fail: " + e.getMessage(), e);
        } finally {
            response.close();
        }
    }

    public TestTask getTestStatus(HydraLabAPIConfig apiConfig, String testTaskId) {
        checkCenterAlive(apiConfig);

        Request req = new Request.Builder()
                .addHeader("Authorization", "Bearer " + apiConfig.authToken)
                .url(apiConfig.getTestStatusUrl(testTaskId))
                .build();
        OkHttpClient clientToUse = client;
        Response response = null;
        try {
            response = clientToUse.newCall(req).execute();
            int waitingRetry = httpFailureRetryTimes;
            while (!response.isSuccessful() && waitingRetry > 0) {
                printlnf("##[warning]Get test status failed, remaining retry times: %d\nHttp code: %d\nHttp message: %s", waitingRetry, response.code(), response.message());
                response = clientToUse.newCall(req).execute();
                waitingRetry--;
            }

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
        } finally {
            response.close();
        }
    }

    public String getStorageToken(HydraLabAPIConfig apiConfig) {
        Request req = new Request.Builder()
                .addHeader("Authorization", "Bearer " + apiConfig.authToken)
                .url(apiConfig.getStorageTokenUrl())
                .build();
        OkHttpClient clientToUse = client;
        Response response = null;
        try {
            response = clientToUse.newCall(req).execute();
            int waitingRetry = httpFailureRetryTimes;
            while (!response.isSuccessful() && waitingRetry > 0) {
                printlnf("##[warning]Get storage token failed, remaining retry times: %d\nHttp code: %d\nHttp message: %s", waitingRetry, response.code(), response.message());
                response = clientToUse.newCall(req).execute();
                waitingRetry--;
            }

            assertTrue(response.isSuccessful(), "Get storage token", response);
            ResponseBody body = response.body();

            assertNotNull(body, response + ": storage token");
            JsonObject jsonObject = GSON.fromJson(body.string(), JsonObject.class);

            int resultCode = jsonObject.get("code").getAsInt();
            assertTrue(resultCode == 200, "Server returned code: " + resultCode, jsonObject);

            return jsonObject.getAsJsonObject("content").getAsString();
        } catch (Exception e) {
            throw new RuntimeException("Get storage token fail: " + e.getMessage(), e);
        } finally {
            response.close();
        }
    }

    public void downloadToFile(String fileUrl, File file) {
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

    public void cancelTestTask(HydraLabAPIConfig apiConfig, String testTaskId, String reason) {
        checkCenterAlive(apiConfig);

        Request req = new Request.Builder()
                .addHeader("Authorization", "Bearer " + apiConfig.authToken)
                .url(String.format(apiConfig.getCancelTestTaskUrl(), testTaskId, reason))
                .build();
        OkHttpClient clientToUse = client;
        Response response = null;
        try {
            response = clientToUse.newCall(req).execute();
            int waitingRetry = httpFailureRetryTimes;
            while (!response.isSuccessful() && waitingRetry > 0) {
                printlnf("##[warning]Cancel test task failed, remaining retry times: %d\nHttp code: %d\nHttp message: %s", waitingRetry, response.code(), response.message());
                response = clientToUse.newCall(req).execute();
                waitingRetry--;
            }

            assertTrue(response.isSuccessful(), "cancel test task", response);
        } catch (Exception e) {
            throw new RuntimeException("cancel test task fail: " + e.getMessage(), e);
        } finally {
            response.close();
        }
    }
}
