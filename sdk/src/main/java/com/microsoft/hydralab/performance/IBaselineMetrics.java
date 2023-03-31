package com.microsoft.hydralab.performance;

import java.util.LinkedHashMap;

public interface IBaselineMetrics {
    // Use LinkedHashMap to ensure the order of metrics key-value
    LinkedHashMap<String, Double> getBaselineMetricsKeyValue();

    SummaryType getSummaryType();

    enum SummaryType {
        AVERAGE,
        MAX
    }
}
