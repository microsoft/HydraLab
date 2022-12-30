// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

import java.util.ArrayList;
import java.util.List;

public enum PerformanceInspectionService implements IPerformanceInspectionService {
    INSTANCE;
    private IPerformanceInspectionService serviceInstance = new IPerformanceInspectionService() {
        @Override
        public void initialize(PerformanceInspection performanceInspection) {

        }

        @Override
        public List<PerformanceInspectionResult> inspect(PerformanceInspection performanceInspection) {
            return new ArrayList<>();
        }

        @Override
        public PerformanceTestResult parse(PerformanceInspection performanceInspection) {
            return new PerformanceTestResult();
        }
    };

    void swapServiceInstance(IPerformanceInspectionService serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    public static PerformanceInspectionService getInstance() {
        return INSTANCE;
    }

    @Override
    public void initialize(PerformanceInspection performanceInspection) {
        serviceInstance.initialize(performanceInspection);
    }

    @Override
    public List<PerformanceInspectionResult> inspect(PerformanceInspection performanceInspection) {
        return serviceInstance.inspect(performanceInspection);
    }

    @Override
    public PerformanceTestResult parse(PerformanceInspection performanceInspection) {
        return serviceInstance.parse(performanceInspection);
    }
}
