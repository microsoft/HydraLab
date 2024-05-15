// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

public class HydraLabRuntimeException extends RuntimeException {
    private int code;

    public HydraLabRuntimeException(String message) {
        this(500, message);
    }

    public HydraLabRuntimeException(String message, Exception e) {
        this(500, message, e);
    }

    public HydraLabRuntimeException(int code, String message) {
        super(message);
        this.code = code;
    }

    public HydraLabRuntimeException(int code, String message, Exception e) {
        super(message, e);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
