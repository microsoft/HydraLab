// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.xctest;

import com.microsoft.hydralab.common.util.CommandOutputReceiver;
import lombok.Getter;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.ArrayList;

@Getter
public class XCTestCommandReceiver extends CommandOutputReceiver {
    private final ArrayList<String> result = new ArrayList<>();

    public XCTestCommandReceiver(InputStream inputStream, Logger logger) {
        super(inputStream, logger);
    }

    @Override
    protected boolean handleEachLine(String line) {
        result.add(line);
        return super.handleEachLine(line);
    }
}