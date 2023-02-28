// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.management;


import com.microsoft.hydralab.common.management.impl.AndroidDeviceManager;
import com.microsoft.hydralab.common.management.impl.IOSDeviceManager;
import com.microsoft.hydralab.common.management.impl.WindowsDeviceManager;

public enum AgentType {
    ANDROID(1) {
        @Override
        public DeviceManager getManager() {
            return new AndroidDeviceManager();
        }
    }, WINDOWS(2) {
        @Override
        public DeviceManager getManager() {
            return new WindowsDeviceManager();
        }
    }, IOS(3) {
        @Override
        public DeviceManager getManager() {
            return new IOSDeviceManager();
        }
    };

    private final int agentType;

    AgentType(int agentType) {
        this.agentType = agentType;
    }

    public static AgentType formAgentType(int typeName) {
        for (AgentType type : AgentType.values()) {
            if (type.getAgentType() == typeName) {
                return type;
            }
        }
        return ANDROID;
    }

    public abstract DeviceManager getManager();

    private int getAgentType() {
        return agentType;
    }
}
