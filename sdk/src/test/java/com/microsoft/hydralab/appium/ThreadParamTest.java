package com.microsoft.hydralab.appium;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadParamTest {
    Logger logger = LoggerFactory.getLogger(ThreadParamTest.class);

    @Test
    public void init() {
        logger.info("todo");
        ThreadParam.init(null,null);
    }

    @Test
    public void clean() {
    }
}