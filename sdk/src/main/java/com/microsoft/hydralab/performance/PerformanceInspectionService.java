// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public enum PerformanceInspectionService implements IPerformanceInspectionService {
    INSTANCE;

    public static PerformanceInspectionService getInstance() {
        return INSTANCE;
    }

    private IPerformanceInspectionService serviceInstance = new IPerformanceInspectionService() {
        @Override
        public void reset(PerformanceInspection performanceInspection) {

        }

        @Override
        public PerformanceInspectionResult inspect(PerformanceInspection performanceInspection) {
            return new PerformanceInspectionResult(0, new File("."));
        }

        @Override
        public void inspectWithStrategy(PerformanceInspection performanceInspection, InspectionStrategy inspectionStrategy) {

        }

        @Override
        public List<PerformanceTestResult> parse() {
            return new ArrayList<>();
        }
    };


    void swapServiceInstance(IPerformanceInspectionService serviceInstance) {
        this.serviceInstance = serviceInstance;
    }


    @Override
    public void reset(PerformanceInspection performanceInspection) {
        serviceInstance.reset(performanceInspection);
    }

    @Override
    public PerformanceInspectionResult inspect(PerformanceInspection performanceInspection) {
        return serviceInstance.inspect(performanceInspection);
    }

    @Override
    public void inspectWithStrategy(PerformanceInspection performanceInspection, InspectionStrategy inspectionStrategy) {
        serviceInstance.inspectWithStrategy(performanceInspection, inspectionStrategy);
    }

    @Override
    public List<PerformanceTestResult> parse() {
        return serviceInstance.parse();
    }
}
