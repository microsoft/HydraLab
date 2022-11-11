package com.microsoft.hydralab.t2c.runner;

import org.junit.Assert;
import org.junit.Test;
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
        Assert.assertNotNull("Analysis Json File failed", testInfo);
        Assert.assertTrue("Analysis Driver failed", testInfo.getDrivers() != null && testInfo.getDrivers().size() == 2);
        Assert.assertNotNull("Analysis Case failed", testInfo.getCases() != null && testInfo.getCases().size() == 1);
    }
}