// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.appcenter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
import java.util.regex.Pattern;

public class AppCenterClientService {
    private static final String LOG_URL = "https://in.appcenter.ms";
    private static final String API_PATH = "/logs?api-version=1.0.0";
    private static final String INSTALL_ID = "Install-ID";
    private static final String APP_SECRET = "App-Secret";
    private static final int MIN_GZIP_LENGTH = 1400;
    private static final int MAX_PRETTIFY_LOG_LENGTH = 4 * 1024;
    private static final String CONTENT_TYPE_VALUE = "application/json";
    private static final String CHARSET_NAME = StandardCharsets.UTF_8.name();
    private static final Pattern TOKEN_REGEX_URL_ENCODED = Pattern.compile("token=[^&]+");
    private static final Pattern TOKEN_REGEX_JSON = Pattern.compile("token\":\"[^\"]+\"");
    private static final Pattern REDIRECT_URI_REGEX_JSON = Pattern.compile("redirect_uri\":\"[^\"]+\"");

    public JSONObject sendCrashLog(String appSecret, String installId, LogContainer logContainer) throws IOException {

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new GzipRequestInterceptor())
                .build();

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
