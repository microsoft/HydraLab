package com.microsoft.hydralab.performance;

import java.util.LinkedHashMap;

public interface IBaselineMetrics {
    LinkedHashMap<String, Double> getBaselineMetricsKeyValue();

    SummaryType getSummaryType();

    enum SummaryType {
        AVERAGE,
        MAX
    }
}
