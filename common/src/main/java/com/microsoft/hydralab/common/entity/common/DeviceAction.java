// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zhoule
 */
@Data
public class DeviceAction implements Serializable {
    private String deviceType;
    private String method;
    private List<String> args = new ArrayList<>();


    public interface When {
        String SET_UP = "setUp";
        String TEAR_DOWN = "tearDown";
    }

    public DeviceAction() {
    }

    public DeviceAction(String deviceType, String method) {
        this.deviceType = deviceType;
        this.method = method;
    }
}