package com.microsoft.hydralab.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CommandOutputReceiverTest {

    @Test
    public void testReceiverOutput() throws IOException, InterruptedException {
        Logger logger = LoggerFactory.getLogger(CommandOutputReceiverTest.class);
        StringBuilder infoBuilder = new StringBuilder();
        StringBuilder errorBuilder = new StringBuilder();

        Process process = null;
        try {
            process = Runtime.getRuntime().exec("python --version");

            CommandOutputReceiver receiverInfo = new CommandOutputReceiver(process.getInputStream(), logger) {
                @Override
                protected boolean handleEachLine(String line) {
                    infoBuilder.append(line).append('\n');
                    return false;
                }
            };
            receiverInfo.start();
            // As we return true in this method, we should see no log output from inner class of CommandOutputReceiver.
            CommandOutputReceiver receiverError = new CommandOutputReceiver(process.getErrorStream(), logger) {
                @Override
                protected boolean handleEachLine(String line) {
                    errorBuilder.append(line).append('\n');
                    return true;
                }
            };
            receiverError.start();

            process.waitFor();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        logger.info(infoBuilder.toString());
        logger.info(errorBuilder.toString());
        Assertions.assertTrue(infoBuilder.toString().length() + errorBuilder.toString().length() > 0);
    }
}
