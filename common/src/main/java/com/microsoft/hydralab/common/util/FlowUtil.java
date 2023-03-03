// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.util;

import java.util.concurrent.Callable;

public final class FlowUtil {

    private FlowUtil() {

    }

    public static boolean retryWhenFalse(int count, Callable<Boolean> predicate) {
        RuntimeException toThrow = null;
        int tmp = count;
        while (tmp > 0) {
            try {
                if (predicate.call()) {
                    return true;
                }
            } catch (Exception e) {
                toThrow = new RuntimeException(e);
            }
            tmp--;
        }
        if (toThrow != null) {
            throw toThrow;
        }
        return false;
    }

    public static boolean retryAndSleepWhenFalse(int count, int sleepSeconds, Callable<Boolean> predicate) throws Exception {
        Exception toThrow = null;
        int tmp = count;
        while (tmp > 0) {
            try {
                if (predicate.call()) {
                    return true;
                }
            } catch (Exception e) {
                toThrow = e;
            }
            ThreadUtils.safeSleep(sleepSeconds * 1000);
            tmp--;
        }
        if (toThrow != null) {
            throw toThrow;
        }
        return false;
    }
}
