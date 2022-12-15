package com.microsoft.hydralab.common.entity.common;

import com.microsoft.hydralab.common.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;

class SASDataTest extends BaseTest {

    @Test
    void setExpiredTime() {
        SASData.SASPermission.Read.setExpiryTime(40, "SECONDS");
        logger.info(SASData.SASPermission.Read.toString());
        Assertions.assertEquals(SASData.SASPermission.Read.timeUnit, ChronoUnit.SECONDS);


        SASData.SASPermission.Write.setExpiryTime(2, "MINUTES");
        logger.info(SASData.SASPermission.Write.toString());
        Assertions.assertEquals(SASData.SASPermission.Write.timeUnit, ChronoUnit.MINUTES);
    }
}