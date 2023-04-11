// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.t2c.runner;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class T2CJsonParserTest {
    Logger logger = LoggerFactory.getLogger(T2CJsonParserTest.class);
    String filePath = "src/test/resources/DemoJson.json";
    String perfPath = "src/test/resources/PerfDemoJson.json";
    String iosPerfPath = "src/test/resources/iOSPerfDemoJson.json";

    @Test
    public void parseJsonFile() {
        T2CJsonParser t2CJsonParser = new T2CJsonParser(logger);
        TestInfo testInfo = t2CJsonParser.parseJsonFile(filePath);
        Assertions.assertNotNull(testInfo, "Analysis Json File failed");
        Assertions.assertTrue(testInfo.getDrivers() != null && testInfo.getDrivers().size() == 2, "Analysis Driver failed");
        for (ActionInfo actionInfo : testInfo.getActions()) {
            logger.info(actionInfo.toString());
        }
        Assertions.assertNotNull(testInfo.getActions(), "Analysis Action failed");
    }

    @Test
    public void parsePerfJsonFile() {
        T2CJsonParser t2CJsonParser = new T2CJsonParser(logger);
        TestInfo testInfo = t2CJsonParser.parseJsonFile(perfPath);
        Assertions.assertNotNull(testInfo, "Analysis Json File failed");
        Assertions.assertTrue(testInfo.getDrivers() != null && testInfo.getDrivers().size() == 1, "Analysis Driver failed");
        for (ActionInfo actionInfo : testInfo.getActions()) {
            logger.info(actionInfo.toString());
        }
        Assertions.assertEquals(5, testInfo.getActions().size(), "Analysis Action failed");
    }

    @Test
    public void parsIosPerfJsonFile() {
        T2CJsonParser t2CJsonParser = new T2CJsonParser(logger);
        TestInfo testInfo = t2CJsonParser.parseJsonFile(iosPerfPath);
        Assertions.assertNotNull(testInfo, "Analysis Json File failed");
        Assertions.assertTrue(testInfo.getDrivers() != null && testInfo.getDrivers().size() == 1, "Analysis Driver failed");
        for (ActionInfo actionInfo : testInfo.getActions()) {
            logger.info(actionInfo.toString());
        }
        Assertions.assertEquals(25, testInfo.getActions().size(), "Analysis Action failed");
    }
}