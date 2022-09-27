// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CommandOutputReceiver extends Thread {
    private InputStream inputStream;
    private Logger logger;

    public CommandOutputReceiver(InputStream inputStream, Logger logger) {
        this.inputStream = inputStream;
        this.logger = logger;
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(inputStream, "GBK");
            BufferedReader bufferedReader = new BufferedReader(isr);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (logger == null) {
                    System.out.println(line);
                } else {
                    logger.info(line);
                }
            }
            isr.close();
            bufferedReader.close();
        } catch (IOException e) {
            if (logger != null) {
                logger.info("Exception:" + e);
            }
            e.printStackTrace();
        } finally {
            synchronized (this) {
                notify();
            }
        }
    }
}