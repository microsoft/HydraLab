// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import java.util.concurrent.Callable;

public class FlowUtil {

    public static boolean retryWhenFalse(int count, Callable<Boolean> predicate) {
        RuntimeException toThrow = null;
        while (count > 0) {
            try {
                if (predicate.call()) {
                    return true;
                }
            } catch (Exception e) {
                toThrow = new RuntimeException(e);
            }
            count--;
        }
        if (toThrow != null) {
            throw toThrow;
        }
        return false;
    }
}
