// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.appcenter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.appcenter.entity.Device;
import com.microsoft.hydralab.common.appcenter.entity.HandledErrorLog;
import com.microsoft.hydralab.common.appcenter.entity.Log;
import com.microsoft.hydralab.common.appcenter.entity.LogContainer;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

public class AppCenterClient {
    private static final String LOG_URL = "https://in.appcenter.ms";
    private static final String API_PATH = "/logs?api-version=1.0.0";
    private static final String INSTALL_ID = "Install-ID";
    private static final String APP_SECRET = "App-Secret";
    private static final String CONTENT_TYPE_VALUE = "application/json";
    private static final String CHARSET_NAME = StandardCharsets.UTF_8.name();
    private static final Pattern TOKEN_REGEX_URL_ENCODED = Pattern.compile("token=[^&]+");
    private static final Pattern TOKEN_REGEX_JSON = Pattern.compile("token\":\"[^\"]+\"");
    private final long appInitializeTimestamp;
    OkHttpClient httpClient = new OkHttpClient.Builder()
            .addInterceptor(new GzipRequestInterceptor())
            .build();
    private final String appSecret;
    // TODO: this should be a persistent value
    private final String installId;
    private final UUID sid;
    AppCenterErrorLogHandler appCenterErrorLogHandler;


    public AppCenterClient(String appSecret, String name) {
        this.appSecret = appSecret;
        installId = UUID.randomUUID().toString();
        sid = UUID.randomUUID();
        appInitializeTimestamp = System.currentTimeMillis();
        appCenterErrorLogHandler = new AppCenterErrorLogHandler(getDevice(name), installId);
    }

    @NotNull
    private static Device getDevice(String name) {
        Device device = new Device();

        // System info
        Properties props = System.getProperties();
        device.setOemName(System.getProperty("user.name"));
        device.setOsName(props.getProperty("os.name"));
        device.setOsVersion(props.getProperty("os.version"));
        device.setLocale(Locale.getDefault().toString());
        device.setOsBuild(props.getProperty("os.arch"));
        device.setModel(props.getProperty("os.name") + "_HydraLab." + name + "_" + System.getenv("COMPUTERNAME"));
        device.setTimeZoneOffset(0);

        // App Center App info
        device.setSdkName("appcenter.android");
        device.setSdkVersion("4.4.4");
        device.setAppNamespace("com.microsoft.hydralab.agent");

        // Hydra Lab Agent info, TODO read from app version config
        device.setAppVersion("1.0.0");
        device.setAppBuild("110");

        // Mock value
        device.setOsApiLevel(30);
        device.setScreenSize("640x480");
        return device;
    }

    public JSONObject send(Log log) throws IOException {
        LogContainer logContainer = new LogContainer();
        logContainer.getLogs().add(log);

        if (log instanceof HandledErrorLog) {
            ((HandledErrorLog) log).setSid(sid);
        }

        String jsonString = JSON.toJSONString(logContainer);
        RequestBody body = RequestBody.create(jsonString, MediaType.parse(CONTENT_TYPE_VALUE));
        Request request = new Request.Builder()
                .url(LOG_URL + API_PATH)
                .header(APP_SECRET, appSecret)
                .header(INSTALL_ID, installId)
                .post(body)
                .build();

        // Perform the HTTP request.
        Response response = httpClient.newCall(request).execute();
        int status = response.code();
        if (status == 200) {
            String responseBody = response.body() != null ? response.body().string() : null;
            // Close the response.
            response.close();
            return JSON.parseObject(responseBody);
        }
        throw new RuntimeException(
                "Failed to send crash log to AppCenter. Status code: " + status +
                        " Response: " + response +
                        " response.body: " + (response.body() == null ? "" : response.body().string()));
    }

    public HandledErrorLog createErrorLog(Thread thread, Exception testException, boolean fatal) {
        return appCenterErrorLogHandler.createErrorLog(thread, testException, appInitializeTimestamp, fatal);
    }

    static class GzipRequestInterceptor implements Interceptor {
        @NotNull
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            if (originalRequest.body() == null || originalRequest.header("Content-Encoding") != null) {
                return chain.proceed(originalRequest);
            }

            Request compressedRequest = originalRequest.newBuilder()
                    .header("Content-Encoding", "gzip")
                    .method(originalRequest.method(), gzip(originalRequest.body()))
                    .build();
            return chain.proceed(compressedRequest);
        }

        private RequestBody gzip(final RequestBody body) {
            return new RequestBody() {
                @Override
                public MediaType contentType() {
                    return body.contentType();
                }

                @Override
                public long contentLength() {
                    return -1; // We don't know the compressed length in advance!
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
                    body.writeTo(gzipSink);
                    gzipSink.close();
                }
            };
        }
    }

}
