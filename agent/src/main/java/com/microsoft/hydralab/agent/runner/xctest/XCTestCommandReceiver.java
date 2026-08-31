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
    private volatile boolean testComplete = false;

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
                // Detect xcodebuild test completion markers.
                // Two levels of completion signals:
                // 1. Early: "Test Suite 'All tests' {passed|failed} at ..." — appears immediately
                //    when all tests finish, BEFORE the 600s diagnostics collection phase.
                // 2. Late: "** TEST {SUCCEEDED|FAILED|EXECUTE FAILED} **" — appears only after
                //    xcodebuild finishes diagnostics collection (~600s later).
                // We detect the early marker to avoid a 10-minute wait.
                if ((line.contains("** TEST") && (line.contains("SUCCEEDED **") || line.contains("FAILED **")))
                        || (line.contains("Test Suite 'All tests'") && !line.contains("started"))) {
                    testComplete = true;
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

    /**
     * Returns true when xcodebuild output indicates test suite has finished.
     */
    public boolean isTestComplete() {
        return testComplete;
    }

    public ArrayList<String> getResult() {
        return result;
    }
}
