// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import com.microsoft.hydralab.performance.IBaselineMetrics;
import com.microsoft.hydralab.performance.PerformanceInspector;
import com.microsoft.hydralab.performance.PerformanceResultParser;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author taoran
 * @date 3/14/2023
 */
@Data
@Entity
public class PerformanceTestResultEntity implements Serializable {
    private static final int MAX_METRICS_NUM = 5;
    @Id
    private String id = UUID.randomUUID().toString();
    private String testRunId;
    private String testTaskId;
    private Date date = new Date(System.currentTimeMillis());
    private String inspectorType;
    private String parserType;
    private String testSuite;
    private String runningType;
    private String appId;
    private String deviceId;
    private boolean success;
    private String summaryType;
    private String metric1Key;
    private double metric1Value = -1;
    private String metric2Key;
    private double metric2Value = -1;
    private String metric3Key;
    private double metric3Value = -1;
    private String metric4Key;
    private double metric4Value = -1;
    private String metric5Key;
    private double metric5Value = -1;
    private String model;
    // Build number for android device
    private String buildNumber;

    public PerformanceTestResultEntity(String testRunId, String testTaskId, PerformanceInspector.PerformanceInspectorType inspectorType,
                                       PerformanceResultParser.PerformanceResultParserType parserType, IBaselineMetrics baselineMetrics, String testSuite,
                                       String runningType, String appId, String deviceId, boolean success, String model, String buildNumber)
            throws NoSuchFieldException, IllegalAccessException {

        this.testRunId = testRunId;
        this.testTaskId = testTaskId;
        this.inspectorType = inspectorType == null ? null : inspectorType.name();
        this.parserType = parserType == null ? null : parserType.name();
        this.testSuite = testSuite;
        this.runningType = runningType;
        this.appId = appId;
        this.deviceId = deviceId;
        this.success = success;
        this.summaryType = baselineMetrics.getSummaryType() == null ? null : baselineMetrics.getSummaryType().name();
        this.model = model;
        this.buildNumber = buildNumber;
        initBaselineMetrics(baselineMetrics.getBaselineMetricsKeyValue());
    }

    public PerformanceTestResultEntity() {
    }

    private void initBaselineMetrics(LinkedHashMap<String, Double> baselineMetrics) throws NoSuchFieldException, IllegalAccessException {
        if (baselineMetrics == null) {
            return;
        }

        Class<? extends PerformanceTestResultEntity> clazz = this.getClass();
        int i = 1;
        for (Map.Entry<String, Double> entry : baselineMetrics.entrySet()) {
            if (i > Math.min(baselineMetrics.size(), MAX_METRICS_NUM)) {
                break;
            }

            Field metricKeyField = clazz.getDeclaredField("metric" + i + "Key");
            Field metricValueField = clazz.getDeclaredField("metric" + i + "Value");
            metricKeyField.set(this, entry.getKey());
            metricValueField.set(this, entry.getValue());
            i++;
        }
    }
}
