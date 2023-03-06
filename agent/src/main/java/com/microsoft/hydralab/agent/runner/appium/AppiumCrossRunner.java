// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.appium;

import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;

public class AppiumCrossRunner extends AppiumRunner {
    String agentName;

    public AppiumCrossRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                             PerformanceTestManagementService performanceTestManagementService, String agentName) {
        super(agentManagementService, testTaskRunCallback, performanceTestManagementService);
        this.agentName = agentName;
    }
}
