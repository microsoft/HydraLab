// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.management;


import com.microsoft.hydralab.common.management.device.TestDeviceManager;
import com.microsoft.hydralab.common.management.device.impl.AndroidTestDeviceManager;
import com.microsoft.hydralab.common.management.device.impl.IOSTestDeviceManager;
import com.microsoft.hydralab.common.management.device.impl.WindowsTestDeviceManager;

public enum AgentType {
    ANDROID(1) {
        @Override
        public TestDeviceManager getManager() {
            return new AndroidTestDeviceManager();
        }
    }, WINDOWS(2) {
        @Override
        public TestDeviceManager getManager() {
            return new WindowsTestDeviceManager();
        }
    }, IOS(3) {
        @Override
        public TestDeviceManager getManager() {
            return new IOSTestDeviceManager();
        }
    };

    private int agentType;

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

    public abstract TestDeviceManager getManager();

    private int getAgentType() {
        return agentType;
    }
}
