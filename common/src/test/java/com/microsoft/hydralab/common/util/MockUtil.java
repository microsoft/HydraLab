package com.microsoft.hydralab.common.util;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class MockUtil {
    @NotNull
    public static OkHttpClient mockOkHttpClient(String responseBody) throws IOException {
        OkHttpClient client = Mockito.mock(OkHttpClient.class);
        Call mockCall = Mockito.mock(Call.class);

        Response response = new Response.Builder()
                .protocol(Protocol.HTTP_1_1).message(responseBody)
                .request(new Request.Builder().url("https://mock.com").build())
                .code(200)
                .body(ResponseBody
                        .create(responseBody, MediaType.parse("application/json"))
                )
                .build();
        when(client.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(response);
        return client;
    }
}
