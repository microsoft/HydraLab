package com.microsoft.hydralab.common.util;

import com.microsoft.hydralab.common.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.Date;

class DateUtilTest extends BaseTest {

    @Test
    void localToUTC() {
        long time = System.currentTimeMillis();
        logger.info(DateUtil.FORMAT.format(new Date(time)));
        logger.info(DateUtil.APP_CENTER_FORMAT_2.format(new Date(time)));
        logger.info(DateUtil.APP_CENTER_FORMAT_2.format(DateUtil.localToUTC(new Date(time))));
    }
}