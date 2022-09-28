// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.logger;

import com.android.ddmlib.IShellOutputReceiver;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;

public class MultiLineNoCancelLoggingReceiver extends MultiLineNoCancelReceiver {
    IShellOutputReceiver wrapped;
    Logger logger;

    public MultiLineNoCancelLoggingReceiver(Logger logger) {
        this.logger = logger;
    }

    public MultiLineNoCancelLoggingReceiver(Logger logger, IShellOutputReceiver wrapped) {
        this.logger = logger;
        this.wrapped = wrapped;
    }

    @Override
    public void processNewLines(String[] lines) {
        if (wrapped != null) {
            for (String line : lines) {
                byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
                wrapped.addOutput(bytes, 0, bytes.length);
            }
        }
        if (logger != null) {
            for (String l : lines) {
                logger.info(l);
            }
        }
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}