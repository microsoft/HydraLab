// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.PrintWriter;
import java.io.StringWriter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@SuppressWarnings("unchecked")
public class Result<T> {
    private int code;
    private String message;
    private T content;

    public static Result<Object> ok() {
        return builder().code(200).message("OK!").build();
    }

    public static <T> Result<T> ok(T content) {
        return (Result<T>) builder().code(200).message("OK!").content(content).build();
    }

    public static <T> Result<T> error(int code) {
        return (Result<T>) builder().code(code).message("error").build();
    }

    public static <T> Result<T> error(int code, String message) {
        return (Result<T>) builder().code(code).message(message).build();
    }

    public static <T> Result<T> error(int code, Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String sStackTrace = sw.toString();
        return (Result<T>) builder().code(code).message(throwable.getClass().getName() + ": " + throwable.getMessage() + "\n" + sStackTrace).build();
    }
}
