// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

public enum PerformanceInspectionService implements IPerformanceInspectionService {
    INSTANCE;

    public static PerformanceInspectionService getInstance() {
        return INSTANCE;
    }

    private IPerformanceInspectionService serviceImplementation = new IPerformanceInspectionService() {
        @Override
        public PerformanceInspectionResult inspect(PerformanceInspection performanceInspection) {
            return null;
        }

        @Override
        public void inspectWithStrategy(InspectionStrategy inspectionStrategy) {
        }

        @Override
        public PerformanceTestResult parse(PerformanceInspection performanceInspection) {
            return null;
        }
    };

    void swapImplementation(IPerformanceInspectionService serviceImplementation) {
        this.serviceImplementation = serviceImplementation;
    }

    @Override
    public PerformanceInspectionResult inspect(PerformanceInspection performanceInspection) {
        return serviceImplementation.inspect(performanceInspection);
    }

    @Override
    public void inspectWithStrategy(InspectionStrategy inspectionStrategy) {
        serviceImplementation.inspectWithStrategy(inspectionStrategy);
    }

    @Override
    public PerformanceTestResult parse(PerformanceInspection performanceInspection) {
        return serviceImplementation.parse(performanceInspection);
    }
}
