// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author zhoule
 */
@Data
public class DeviceAction {
    private String deviceType;
    private String method;
    private List<String> args = new ArrayList<>();


    public interface When {
        String SET_UP = "setUp";
        String TEAR_DOWN = "tearDown";
    }
}