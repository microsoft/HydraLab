package com.microsoft.hydralab.common.performace;

import com.microsoft.hydralab.common.performace.impl.*;
import com.microsoft.hydralab.performance.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class PerformanceManager implements IPerformanceInspectionService {
    static final ScheduledExecutorService timerExecutor = Executors.newScheduledThreadPool(20 /* corePoolSize */);
    private final Map<String, PerformanceInspector> performanceInspectorMap = Map.of(
            PerformanceTestSpec.INSPECTOR_ANDROID_BATTERY_INFO, new AndroidBatteryInspector(),
            PerformanceTestSpec.INSPECTOR_ANDROID_MEMORY_DUMP, new AndroidMemoryDumpInspector(),
            PerformanceTestSpec.INSPECTOR_ANDROID_MEMORY_INFO, new AndroidMemoryInfoInspector(),
            PerformanceTestSpec.INSPECTOR_WIN_BATTERY, new WindowsBatteryInspector(),
            PerformanceTestSpec.INSPECTOR_WIN_MEMORY, new WindowsMemoryInspector()
    );
    final List<ScheduledFuture<?>> inspectPerformanceTimerList = new ArrayList<>();
    private final Map<String, List<PerformanceTestResult>> resultListMap = new ConcurrentHashMap<>();

    /**
     * TODO: when found a strategy, start it here
     * @param performanceTestSpec
     */
    public void initialize(PerformanceTestSpec performanceTestSpec) {
        PerformanceInspector performanceInspector = getInspectorByName(performanceTestSpec.getInspectors());
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
        for (String inspector : performanceTestSpec.getInspectors()) {
            PerformanceInspector performanceInspector = getInspectorByName(inspector);
            if (performanceInspector == null) {
                continue;
            }
            PerformanceInspectionResult result = performanceInspector.inspect(performanceTestSpec);
            if (result != null) {
                tempInspectionResultList.add(result);
            }
        }
        resultListMap.putIfAbsent(performanceTestSpec.id, new PerformanceTestResult());
        PerformanceTestResult performanceTestResult = resultListMap.get(performanceTestSpec.id);
        performanceTestResult.performanceInspectionResults.addAll(tempInspectionResultList);
        return tempInspectionResultList;
    }

    public List<PerformanceTestResult> parse(PerformanceTestSpec performanceTestSpec) {
        List<PerformanceTestResult> performanceTestResultList = new ArrayList<>();
        for (String inspector : performanceTestSpec.getInspectors()) {
            PerformanceInspector performanceInspector = getInspectorByName(inspector);
            if (performanceInspector == null) {
                continue;
            }
            performanceTestResultList.add(performanceInspector.parse(performanceInspectionResultList));
            PerformanceInspectionResult result = performanceInspector.inspect(performanceTestSpec);
            if (result != null) {
                tempInspectionResultList.add(result);
            }
        }
        for (PerformanceInspector performanceInspector : inspectors) {

        }
        for (ScheduledFuture<?> timer : inspectPerformanceTimerList) {
            timer.cancel(true);
        }
        return performanceTestResultList;
    }
}
