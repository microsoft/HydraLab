// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.utils;

import okhttp3.Response;

import java.util.concurrent.Callable;

/**
 * @author Li Shen
 * @date 9/11/2023
 */

public class FlowUtil {
    public static Response httpRetryAndSleepWhenException(int count, int sleepSeconds, Callable<Response> predicate) throws Exception {
        Response res = null;
        Exception toThrow = null;
        while (count > 0) {
            try {
                res = predicate.call();
                return res;
            } catch (Exception e) {
                toThrow = e;
            }
            CommonUtils.safeSleep(sleepSeconds * 1000L);
            count--;
        }
        if (toThrow != null) {
            throw toThrow;
        }
        return res;
    }


}
