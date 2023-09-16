// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.exception.reporter;

import com.microsoft.hydralab.common.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

/**
 * @author zhoule
 * @date 08/02/2023
 */

public class FileReporter implements ExceptionReporter {
    private final String errorFolderPath;

    private final Logger logger = LoggerFactory.getLogger(FileReporter.class);

    public FileReporter(String errorFolderPath) {
        this.errorFolderPath = errorFolderPath;
    }

    @Override
    public void reportException(Exception e, boolean fatal) {
        writeToFile(e, Thread.currentThread());
    }

    @Override
    public void reportException(Exception e, Thread thread, boolean fatal) {
        writeToFile(e, thread);
    }

    private void writeToFile(Exception e, Thread thread) {
        logger.info("Exception collected in Thread {} with message {}", thread.getName(), e.getMessage());
        File errorFileFolder = new File(errorFolderPath, DateUtil.ymdFormat.format(new Date()));
        if (!errorFileFolder.exists()) {
            if (!errorFileFolder.mkdirs()) {
                logger.warn("Failed to create error file folder: " + errorFileFolder.getAbsolutePath(), e);
            }
        }
        //write exception message and stacktrace to file
        try {
            File errorFile = File.createTempFile(
                    thread.getName() + "_" + thread.getId() + "_error_",
                    ".log",
                    errorFileFolder);
            logger.info("Writing exception to file: {}", errorFile.getAbsolutePath());
            PrintStream printStream = new PrintStream(errorFile);
            e.printStackTrace(printStream);
            printStream.flush();
            printStream.close();
        } catch (Exception ex) {
            logger.warn("Failed to write exception to file", e);
        }
    }
}
