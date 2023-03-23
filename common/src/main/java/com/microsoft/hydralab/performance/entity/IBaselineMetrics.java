package com.microsoft.hydralab.performance.entity;

import com.microsoft.hydralab.common.entity.common.PerformanceTestResultEntity;

import java.util.Map;

public interface IBaselineMetrics {
    void sgetBaselineMetrics(PerformanceTestResultEntity entity);

    Map<String, String> getBaselineKey();

    SummaryType getSummaryType();

    enum SummaryType {
        AVERAGE,
        MAX
    }
}
