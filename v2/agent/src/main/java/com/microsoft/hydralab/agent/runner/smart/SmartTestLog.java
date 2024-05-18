// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.smart;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SmartTestLog extends Thread {
    private final InputStream inputStream;
    private final Logger logger;
    private volatile boolean inFinish = false;
    private String res;

    public SmartTestLog(InputStream inputStream, Logger logger) {
        this.inputStream = inputStream;
        this.inFinish = false;
        this.logger = logger;
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(inputStream, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(isr);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                logger.info(line);
                if (line.startsWith("smartTestResult:")) {
                    res = line.replaceFirst("smartTestResult:", "");
                }
            }
            isr.close();
            bufferedReader.close();
        } catch (IOException e) {
            logger.info("Exception:" + e);
            e.printStackTrace();
        } finally {
            this.inFinish = true;
            synchronized (this) {
                notify();
            }
        }
    }

    public String getContent() {
        if (!this.inFinish) {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException ignore) {
                    ignore.printStackTrace();
                }
            }
        }
        return this.res;
    }

}