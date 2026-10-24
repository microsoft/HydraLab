// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.util;


import org.slf4j.Logger;

import java.io.File;

/**
 * @author zhoule
 * @date 08/18/2023
 */

public class PythonUtil {
    public static boolean installRequirements(File requirementsFile, Logger logger) {
        logger.info("Start to install python requirements: {}", requirementsFile.getAbsolutePath());
        String[] command = new String[]{"pip3", "install", "-r", requirementsFile.getAbsolutePath()};
        try {
            Process process = Runtime.getRuntime().exec(command);
            CommandOutputReceiver err = new CommandOutputReceiver(process.getErrorStream(), logger);
            CommandOutputReceiver out = new CommandOutputReceiver(process.getInputStream(), logger);
            err.start();
            out.start();
            process.waitFor();
            return true;
        } catch (Exception e) {
            logger.error("Install python requirements failed: " + e);
        }
        return false;
    }
}
