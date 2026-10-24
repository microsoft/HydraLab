// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Open another thread to read the stream and print it to console.
 * This will close the stream after reading.
 */
public class CommandOutputReceiver extends Thread {
    private final InputStream inputStream;
    @Nullable
    protected final Logger logger;
    @Setter
    private Charset charset = StandardCharsets.UTF_8;

    @Getter
    private volatile boolean finished = false;

    public CommandOutputReceiver(@NotNull InputStream inputStream, @Nullable Logger logger) {
        this.inputStream = inputStream;
        this.logger = logger;
    }

    public void run() {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (handleEachLine(line)) {
                    break;
                }
                if (logger == null) {
                    System.out.println(line);
                } else {
                    logger.info(line);
                }
            }
        } catch (IOException e) {
            if (logger != null) {
                logger.warn("Exception:" + e);
            } else e.printStackTrace();
        } finally {
            this.finished = true;
            synchronized (this) {
                notify();
            }
        }
    }

    /**
     * @param line each line the input stream produced
     * @return should this handling break the control flow, return true to break the flow
     */
    protected boolean handleEachLine(String line) {
        return false;
    }

}