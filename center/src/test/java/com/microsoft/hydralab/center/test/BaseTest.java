// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.test;

import com.microsoft.hydralab.center.util.AuthUtil;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.transaction.Transactional;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author zhoule
 * @date 11/10/2022
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@ActiveProfiles({"test", "local"})
@EnableCaching
@Transactional
@Rollback
@Disabled
public class BaseTest {
    protected Logger baseLogger = LoggerFactory.getLogger(BaseTest.class);
    @MockBean
    protected StorageServiceClientProxy storageServiceClientProxy;
    @MockBean
    AuthUtil authUtil;

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
