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
        public void inspectWithStrategy(PerformanceInspection performanceInspection, InspectionStrategy inspectionStrategy) {

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
    public void inspectWithStrategy(PerformanceInspection performanceInspection, InspectionStrategy inspectionStrategy) {

    }
}
