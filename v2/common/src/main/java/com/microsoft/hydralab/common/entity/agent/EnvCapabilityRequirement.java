// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.agent;

import lombok.Data;

@Data
public class EnvCapabilityRequirement {
    boolean isReady = false;
    String name;
    EnvCapability envCapability;

    public EnvCapabilityRequirement() {
    }

    /**
     * @param keyword
     * @param majorVersion major version of the capability, -1 means any version
     * @param minorVersion minor version of the capability, -1 means any version
     */
    public EnvCapabilityRequirement(EnvCapability.CapabilityKeyword keyword, int majorVersion, int minorVersion) {
        envCapability = new EnvCapability(keyword, majorVersion, minorVersion);
        this.name = keyword.toString();
    }

    public EnvCapabilityRequirement(EnvCapability.CapabilityKeyword keyword, int majorVersion, int minorVersion, String name) {
        envCapability = new EnvCapability(keyword, majorVersion, minorVersion);
        this.name = name;
    }
}
