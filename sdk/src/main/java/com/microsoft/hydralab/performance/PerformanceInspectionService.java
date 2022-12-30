// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

import java.util.ArrayList;
import java.util.List;

public enum PerformanceInspectionService implements IPerformanceInspectionService {
    INSTANCE;
    private IPerformanceInspectionService serviceInstance = new IPerformanceInspectionService() {
        @Override
        public void initialize(PerformanceTestSpec performanceTestSpec) {

        }

        @Override
        public List<PerformanceInspectionResult> inspect(PerformanceTestSpec performanceTestSpec) {
            return new ArrayList<>();
        }

        @Override
        public PerformanceTestResult parse(PerformanceTestSpec performanceTestSpec) {
            return new PerformanceTestResult();
        }
    };

    void switchServiceInstance(IPerformanceInspectionService serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    public static PerformanceInspectionService getInstance() {
        return INSTANCE;
    }

    @Override
    public void initialize(PerformanceTestSpec performanceTestSpec) {
        serviceInstance.initialize(performanceTestSpec);
    }

    @Override
    public List<PerformanceInspectionResult> inspect(PerformanceTestSpec performanceTestSpec) {
        return serviceInstance.inspect(performanceTestSpec);
    }

    @Override
    public PerformanceTestResult parse(PerformanceTestSpec performanceTestSpec) {
        return serviceInstance.parse(performanceTestSpec);
    }
}
