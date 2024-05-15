// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.agent;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class AgentFunctionAvailability implements Serializable {
    AgentFunctionType functionType;
    String functionName;
    List<EnvCapabilityRequirement> envCapabilityRequirements = new ArrayList<>();
    boolean enabled;
    boolean available;

    public AgentFunctionAvailability() {
    }

    public AgentFunctionAvailability(String functionName, AgentFunctionType functionType, boolean enabled, boolean available, List<EnvCapabilityRequirement> requirements) {
        this.functionName = functionName;
        this.functionType = functionType;
        this.enabled = enabled;
        this.available = available;
        this.envCapabilityRequirements = requirements;
    }

    public enum AgentFunctionType {
        TEST_RUNNER,
        ANALYSIS_RUNNER,
        DEVICE_DRIVER
    }
}
