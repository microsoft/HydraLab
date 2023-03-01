// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.management.device;

import java.util.List;

/**
 * @author zhoule
 */

public enum DevicePairRule {
    SINGLE_PHONE() {
        @Override
        public List<DeviceRequirement> getDeviceDefine() {
            new DeviceRequirement().type="Android";
            return null;
        }
    },
    SINGLE_PC() {
        @Override
        public List<DeviceRequirement> getDeviceDefine() {
            return null;
        }
    },
    PC_TO_PHONE() {
        @Override
        public List<DeviceRequirement> getDeviceDefine() {
            return null;
        }
    },
    PHONE_TO_PHONE() {
        @Override
        public List<DeviceRequirement> getDeviceDefine() {
            return null;
        }
    };

    public abstract List<DeviceRequirement> getDeviceDefine();
}
