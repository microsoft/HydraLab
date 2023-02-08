// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtils {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Date.class, new TypeAdapter<Date>() {
                @Override
                public void write(JsonWriter out, Date value) throws IOException {
                    if (value == null) {
                        out.nullValue();
                    } else {
                        out.value(value.getTime());
                    }
                }

                @Override
                public Date read(JsonReader in) throws IOException {
                    if (in != null) {
                        try {
                            return new Date(in.nextLong());
                        } catch (IllegalStateException e) {
                            in.nextNull();
                            return null;
                        }
                    } else {
                        return null;
                    }
                }
            }).create();

    public static String maskCred(String content) {
        for (HydraLabClientUtils.MaskSensitiveData sensitiveData : HydraLabClientUtils.MaskSensitiveData.values()) {
            Pattern PATTERNCARD = Pattern.compile(sensitiveData.getRegEx(), Pattern.CASE_INSENSITIVE);
            Matcher matcher = PATTERNCARD.matcher(content);
            if (matcher.find()) {
                String maskedMessage = matcher.group(2);
                if (maskedMessage.length() > 0) {
                    content = content.replaceFirst(maskedMessage, "***");
                }
            }
        }

        return content;
    }

    public static void printlnf(String format, Object... args) {
        ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
        System.out.print("[" + utc.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] ");
        System.out.printf(format + "\n", args);
    }

    public static void assertNotNull(Object notnull, String argName) {
        if (notnull == null) {
            throw new IllegalArgumentException(argName + " is null");
        }
    }

    public static void assertTrue(boolean beTrue, String msg, Object data) {
        if (!beTrue) {
            throw new IllegalStateException(msg + (data == null ? "" : ": " + data));
        }
    }
}
