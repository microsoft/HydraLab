package com.microsoft.hydralab.common.file.azure;

import com.microsoft.hydralab.common.file.impl.azure.SASPermission;
import com.microsoft.hydralab.common.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;

class SASDataTest extends BaseTest {

    @Test
    void setExpiredTime() {
        SASPermission.READ.setExpiryTime(40, "SECONDS");
        logger.info(SASPermission.READ.toString());
        Assertions.assertEquals(SASPermission.READ.timeUnit, ChronoUnit.SECONDS);


        SASPermission.WRITE.setExpiryTime(2, "MINUTES");
        logger.info(SASPermission.WRITE.toString());
        Assertions.assertEquals(SASPermission.WRITE.timeUnit, ChronoUnit.MINUTES);
    }
}