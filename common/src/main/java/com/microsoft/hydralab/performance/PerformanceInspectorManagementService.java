package com.microsoft.hydralab.performance;

import com.microsoft.hydralab.performance.impl.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class PerformanceInspectorManagementService implements IPerformanceInspectionService {
    static final ScheduledExecutorService timerExecutor = Executors.newScheduledThreadPool(5 /* corePoolSize */);
    private final Map<String, PerformanceInspector> performanceInspectorMap = Map.of(
            PerformanceTestSpec.INSPECTOR_ANDROID_BATTERY_INFO, new AndroidBatteryInspector(),
            PerformanceTestSpec.INSPECTOR_ANDROID_MEMORY_DUMP, new AndroidMemoryDumpInspector(),
            PerformanceTestSpec.INSPECTOR_ANDROID_MEMORY_INFO, new AndroidMemoryInfoInspector(),
            PerformanceTestSpec.INSPECTOR_WIN_BATTERY, new WindowsBatteryInspector(),
            PerformanceTestSpec.INSPECTOR_WIN_MEMORY, new WindowsMemoryInspector()
    );
    final List<ScheduledFuture<?>> inspectPerformanceTimerList = new ArrayList<>();
    private final Map<String, PerformanceTestResult> resultMap = new ConcurrentHashMap<>();

    public void setup() {
        PerformanceInspectionService.getInstance().switchServiceInstance(this);
    }

    /**
     * TODO: when found a strategy, start it here
     *
     * @param performanceTestSpec
     */
    public void initialize(PerformanceTestSpec performanceTestSpec) {
        PerformanceInspector performanceInspector = getInspectorByName(performanceTestSpec.inspector);
        if (performanceInspector == null) {
            return;
        }
        performanceInspector.initialize(performanceTestSpec);
    }

    private PerformanceInspector getInspectorByName(String inspectorName) {
        return performanceInspectorMap.get(inspectorName);
    }

    public void startInspectPerformanceTimer(PerformanceTestSpec performanceTestSpec) {
        initialize(performanceTestSpec);
        ScheduledFuture<?> scheduledFuture = timerExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                inspect(performanceTestSpec);
            }
        }, performanceTestSpec.interval, TimeUnit.SECONDS);
        inspectPerformanceTimerList.add(scheduledFuture);
    }

    public List<PerformanceInspectionResult> inspect(PerformanceTestSpec performanceTestSpec) {
        List<PerformanceInspectionResult> tempInspectionResultList = new ArrayList<>();
        String inspector = performanceTestSpec.inspector;
        PerformanceInspector performanceInspector = getInspectorByName(inspector);
        if (performanceInspector == null) {
            return tempInspectionResultList;
        }
        PerformanceInspectionResult result = performanceInspector.inspect(performanceTestSpec);
        if (result != null) {
            tempInspectionResultList.add(result);
        }
        resultMap.putIfAbsent(performanceTestSpec.inspectionKey, new PerformanceTestResult());
        PerformanceTestResult performanceTestResult = resultMap.get(performanceTestSpec.inspectionKey);
        performanceTestResult.performanceInspectionResults.addAll(tempInspectionResultList);
        return tempInspectionResultList;
    }

    public PerformanceTestResult parse(PerformanceTestSpec performanceTestSpec) {
        PerformanceTestResult performanceTestResult = resultMap.get(performanceTestSpec.inspectionKey);
        if (performanceTestResult == null) {
            return null;
        }
        String inspector = performanceTestSpec.inspector;
        PerformanceInspector performanceInspector = getInspectorByName(inspector);
        performanceInspector.parse(performanceTestResult.performanceInspectionResults);
        for (ScheduledFuture<?> timer : inspectPerformanceTimerList) {
            timer.cancel(true);
        }
        return performanceTestResult;
    }
}
