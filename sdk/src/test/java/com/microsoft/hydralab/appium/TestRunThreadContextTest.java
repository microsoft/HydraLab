package com.microsoft.hydralab.appium;

import com.microsoft.hydralab.TestRunThreadContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRunThreadContextTest {
    Logger logger = LoggerFactory.getLogger(TestRunThreadContextTest.class);

    @Test
    public void init() {
        logger.info("todo");
        TestRunThreadContext.init(null, null, null);
    }

    @Test
    public void clean() {
    }
}