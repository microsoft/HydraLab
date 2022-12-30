package com.microsoft.hydralab.performance;

import com.microsoft.hydralab.performance.impl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class PerformanceInspectorManagementService implements IPerformanceInspectionService {
    static final ScheduledExecutorService timerExecutor = Executors.newScheduledThreadPool(5 /* corePoolSize */);
    private final Map<String, PerformanceInspector> performanceInspectorMap = Map.of(
            PerformanceInspection.INSPECTOR_ANDROID_BATTERY_INFO, new AndroidBatteryInspector(),
            PerformanceInspection.INSPECTOR_ANDROID_MEMORY_DUMP, new AndroidMemoryDumpInspector(),
            PerformanceInspection.INSPECTOR_ANDROID_MEMORY_INFO, new AndroidMemoryInfoInspector(),
            PerformanceInspection.INSPECTOR_WIN_BATTERY, new WindowsBatteryInspector(),
            PerformanceInspection.INSPECTOR_WIN_MEMORY, new WindowsMemoryInspector()
    );
    final List<ScheduledFuture<?>> inspectPerformanceTimerList = new ArrayList<>();
    private final Map<String, PerformanceTestResult> resultMap = new ConcurrentHashMap<>();

    public void initialize() {
        PerformanceInspectionService.getInstance().swapServiceInstance(this);
    }

    /**
     * TODO: when found a strategy, start it here
     *
     */
    public void initialize(PerformanceInspection performanceInspection) {
        PerformanceInspector performanceInspector = getInspectorByName(performanceInspection.inspector);
        if (performanceInspector == null) {
            return;
        }
        performanceInspector.initialize(performanceInspection);
    }

    private PerformanceInspector getInspectorByName(String inspectorName) {
        return performanceInspectorMap.get(inspectorName);
    }

    public void startInspectPerformanceTimer(PerformanceInspection performanceInspection) {
        initialize(performanceInspection);
        ScheduledFuture<?> scheduledFuture = timerExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                inspect(performanceInspection);
            }
        }, performanceInspection.interval, TimeUnit.SECONDS);
        inspectPerformanceTimerList.add(scheduledFuture);
    }
    public List<PerformanceInspectionResult> inspectWithStrategy(PerformanceInspection performanceInspection,InspectionStrategy inspectionStrategy){

    }

    public List<PerformanceInspectionResult> inspect(PerformanceInspection performanceInspection) {
        List<PerformanceInspectionResult> tempInspectionResultList = new ArrayList<>();
        String inspector = performanceInspection.inspector;
        PerformanceInspector performanceInspector = getInspectorByName(inspector);
        if (performanceInspector == null) {
            return tempInspectionResultList;
        }
        PerformanceInspectionResult result = performanceInspector.inspect(performanceInspection);
        if (result != null) {
            tempInspectionResultList.add(result);
        }
        resultMap.putIfAbsent(performanceInspection.inspectionKey, new PerformanceTestResult());
        PerformanceTestResult performanceTestResult = resultMap.get(performanceInspection.inspectionKey);
        performanceTestResult.performanceInspectionResults.addAll(tempInspectionResultList);
        return tempInspectionResultList;
    }

    public PerformanceTestResult parse(PerformanceInspection performanceInspection) {
        PerformanceTestResult performanceTestResult = resultMap.get(performanceInspection.inspectionKey);
        if (performanceTestResult == null) {
            return null;
        }
        String inspector = performanceInspection.inspector;
        PerformanceInspector performanceInspector = getInspectorByName(inspector);
        performanceInspector.parse(performanceTestResult.performanceInspectionResults);
        for (ScheduledFuture<?> timer : inspectPerformanceTimerList) {
            timer.cancel(true);
        }
        return performanceTestResult;
    }
}
