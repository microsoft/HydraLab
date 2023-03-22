// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.xctest;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class XCTestCommandReceiver extends Thread {
    private InputStream inputStream;
    private Logger logger;
    private final ArrayList<String> result = new ArrayList<>();

    public XCTestCommandReceiver(InputStream inputStream, Logger logger) {
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
                    result.add(line);
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

    public ArrayList<String> getResult() {
        return result;
    }
}