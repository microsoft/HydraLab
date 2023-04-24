// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.performance;

import com.microsoft.hydralab.common.entity.common.PerformanceTestResultEntity;
import com.microsoft.hydralab.performance.IBaselineMetrics;
import com.microsoft.hydralab.performance.PerformanceInspector;
import com.microsoft.hydralab.performance.PerformanceResultParser;
import com.microsoft.hydralab.performance.entity.AndroidBatteryInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

/**
 * @author taoran
 * @date 4/18/2023
 */

public class PerformanceTestResultEntityTest {
    @Test
    public void testInitWithNull_ReturnNull() {
        IBaselineMetrics metrics = new IBaselineMetrics() {
            @Override
            public LinkedHashMap<String, Double> getBaselineMetricsKeyValue() {
                return null;
            }

            @Override
            public SummaryType getSummaryType() {
                return null;
            }
        };

        PerformanceTestResultEntity entity = createMockPerformanceTestResultEntity(metrics);

        Assertions.assertNotNull(entity);
        Assertions.assertNull(entity.getSummaryType());
        Assertions.assertNull(entity.getMetric1Key());
        Assertions.assertEquals(-1d, entity.getMetric1Value());
    }

    @Test
    public void testInitWithAndroidBatteryInfo_ReturnNotNull() {
        AndroidBatteryInfo androidBatteryInfo = new AndroidBatteryInfo();
        androidBatteryInfo.setTotal(1.0f);
        androidBatteryInfo.setAppUsage(2.0f);
        androidBatteryInfo.setCpu(3.0f);
        androidBatteryInfo.setSystemService(4.0f);
        androidBatteryInfo.setWakeLock(5.0f);

        PerformanceTestResultEntity entity = createMockPerformanceTestResultEntity(androidBatteryInfo);

        Assertions.assertEquals("total", entity.getMetric1Key());
        Assertions.assertEquals(1.0d, entity.getMetric1Value());
        Assertions.assertEquals("wakeLock", entity.getMetric5Key());
        Assertions.assertEquals(5.0d, entity.getMetric5Value());
    }

    private PerformanceTestResultEntity createMockPerformanceTestResultEntity(IBaselineMetrics baselineMetrics) {
        PerformanceTestResultEntity entity = null;
        try {
            entity = new PerformanceTestResultEntity(
                    "", "",
                    PerformanceInspector.PerformanceInspectorType.INSPECTOR_ANDROID_BATTERY_INFO,
                    PerformanceResultParser.PerformanceResultParserType.PARSER_ANDROID_BATTERY_INFO,
                    baselineMetrics, "", "", "", "", false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return entity;
    }
}
