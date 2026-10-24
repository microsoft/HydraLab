// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.smart;

import com.microsoft.hydralab.common.util.CommandOutputReceiver;
import org.slf4j.Logger;

import java.io.InputStream;

public class SmartTestLog extends CommandOutputReceiver {

    private String res;

    public SmartTestLog(InputStream inputStream, Logger logger) {
        super(inputStream, logger);
    }

    @Override
    protected boolean handleEachLine(String line) {
        String prefix = "smartTestResult:";
        if (line.startsWith(prefix)) {
            res = line.replaceFirst(prefix, "");
        }
        return super.handleEachLine(line);
    }

    public String getContent() {
        if (!isFinished()) {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    if (logger == null) {
                        e.printStackTrace();
                    } else {
                        logger.warn("Error in getContent", e);
                    }
                }
            }
        }
        return this.res;
    }

}