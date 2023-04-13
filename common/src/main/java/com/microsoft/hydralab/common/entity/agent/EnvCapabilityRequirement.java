// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.agent;

import lombok.Data;

@Data
public class EnvCapabilityRequirement {
    boolean isReady = false;
    EnvCapability envCapability;

    public EnvCapabilityRequirement() {
    }

    public EnvCapabilityRequirement(EnvCapability.CapabilityKeyword keyword, int majorVersion, int minorVersion) {
        envCapability = new EnvCapability(keyword, majorVersion, minorVersion);
    }
}
