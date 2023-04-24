// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.appium;

import com.microsoft.hydralab.agent.runner.TestRunDeviceOrchestrator;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.common.entity.agent.EnvCapability;
import com.microsoft.hydralab.common.entity.agent.EnvCapabilityRequirement;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;

import java.util.ArrayList;
import java.util.List;

public class AppiumCrossRunner extends AppiumRunner {
    String agentName;
    private static final int MAJOR_APPIUM_VERSION = 1;
    private static final int MINOR_APPIUM_VERSION = -1;
    private static final int MAJOR_FFMPEG_VERSION = 4;
    private static final int MINOR_FFMPEG_VERSION = -1;

    public AppiumCrossRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                             TestRunDeviceOrchestrator testRunDeviceOrchestrator,
                             PerformanceTestManagementService performanceTestManagementService, String agentName) {
        super(agentManagementService, testTaskRunCallback, testRunDeviceOrchestrator, performanceTestManagementService);
        this.agentName = agentName;
    }

    @Override
    protected List<EnvCapabilityRequirement> getEnvCapabilityRequirements() {
        List<EnvCapabilityRequirement> envCapabilityRequirements = new ArrayList<>();
        envCapabilityRequirements.add(new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.appium, MAJOR_APPIUM_VERSION, MINOR_APPIUM_VERSION));
        envCapabilityRequirements.add(new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.ffmpeg, MAJOR_FFMPEG_VERSION, MINOR_FFMPEG_VERSION));
        return envCapabilityRequirements;
    }
}
