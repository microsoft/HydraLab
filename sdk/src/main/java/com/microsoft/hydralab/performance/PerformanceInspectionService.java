// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PerformanceInspectionService implements IPerformanceInspectionService{
    IPerformanceInspectionService serviceInstance = new IPerformanceInspectionService() {
    };
    List<PerformanceInspector> inspectors = new ArrayList<>();

}
