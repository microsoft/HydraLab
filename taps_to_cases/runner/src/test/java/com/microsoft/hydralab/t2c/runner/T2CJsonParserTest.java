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

    @Test
    public void parseJsonFile() {
        T2CJsonParser t2CJsonParser = new T2CJsonParser(logger);
        TestInfo testInfo = t2CJsonParser.parseJsonFile(filePath);
        logger.info(testInfo.toString());
        Assertions.assertNotNull(testInfo, "Analysis Json File failed");
        Assertions.assertTrue(testInfo.getDrivers() != null && testInfo.getDrivers().size() == 2, "Analysis Driver failed");
        Assertions.assertNotNull(testInfo.getCases() != null && testInfo.getCases().size() == 1, "Analysis Case failed");
    }
}